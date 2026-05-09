package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.quality.entity.ApprovalTicket;
import com.schemaplexai.quality.mapper.ApprovalTicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles FAST → BPMN upgrade for approval tickets.
 *
 * <p>Single-ticket rule: when a FAST approval escalates, the same ticket
 * is updated to PENDING_BPMN stage instead of creating a new ticket.
 * A BPMN workflow instance is then started with businessKey=ticketId.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalEscalationService {

    private final ApprovalTicketMapper approvalTicketMapper;
    private final ApprovalWorkflowBridge workflowBridge;

    /**
     * Escalates a FAST approval ticket to BPMN workflow.
     *
     * @param ticketId the ticket to escalate
     * @param reason   escalation reason
     * @return true if escalated, false if ticket not found or already escalated
     */
    @Transactional
    public boolean escalateFastToBpmn(UUID ticketId, String reason) {
        ApprovalTicket ticket = approvalTicketMapper.selectOne(
                new LambdaQueryWrapper<ApprovalTicket>().eq(ApprovalTicket::getTicketId, ticketId));
        if (ticket == null) {
            log.warn("[Escalation] Ticket {} not found", ticketId);
            return false;
        }

        if (!"PENDING_FAST".equals(ticket.getStage())) {
            log.warn("[Escalation] Ticket {} is not PENDING_FAST (current stage={}), skipping",
                    ticketId, ticket.getStage());
            return false;
        }

        // Single-ticket rule: update existing ticket, don't create new one
        ticket.setStage("PENDING_BPMN");
        ticket.setHandler("workflow");
        ticket.setUpdatedAt(Instant.now());
        approvalTicketMapper.updateById(ticket);

        log.info("[Escalation] Ticket {} escalated from PENDING_FAST to PENDING_BPMN, reason: {}",
                ticketId, reason);

        // Start BPMN workflow instance with businessKey=ticketId
        String workflowInstanceId = workflowBridge.startApprovalWorkflow(ticket);

        log.info("[Escalation] BPMN workflow instance {} started for ticket {}",
                workflowInstanceId, ticketId);

        return true;
    }

    /**
     * Escalates a BPMN approval to the next level in the chain.
     * This delegates to the BPMN process's internal escalation flow.
     *
     * @param ticketId     the ticket to escalate
     * @param approverId   the approver requesting escalation
     * @param reason       escalation reason
     */
    @Transactional
    public void escalateWithinBpmn(UUID ticketId, String approverId, String reason) {
        ApprovalTicket ticket = approvalTicketMapper.selectOne(
                new LambdaQueryWrapper<ApprovalTicket>().eq(ApprovalTicket::getTicketId, ticketId));
        if (ticket == null) {
            log.warn("[Escalation] Ticket {} not found for BPMN escalation", ticketId);
            return;
        }

        // BPMN process handles internal escalation via EscalationDelegate
        // Just signal the workflow to proceed with escalation
        workflowBridge.signalEscalation(ticket, approverId, reason);

        log.info("[Escalation] BPMN escalation signaled for ticket {} by approver {}, reason: {}",
                ticketId, approverId, reason);
    }
}
