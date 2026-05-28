package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.quality.entity.ApprovalTicket;
import com.schemaplexai.quality.mapper.ApprovalTicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Monitors pending approval tickets and triggers escalation for SLA violations.
 *
 * <p>Escalation rules:
 * <ul>
 *   <li>Level 0 (primary approver): SLA = 4h (configurable per tenant)</li>
 *   <li>Level 1 (senior approver): SLA = 24h</li>
 *   <li>Level 2+ (no more escalation): auto-reject after 24h</li>
 * </ul>
 *
 * <p>Runs every 5 minutes to check for overdue tickets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EscalationPolicyService {

    private final ApprovalTicketMapper approvalTicketMapper;
    private final ApprovalEscalationService escalationService;

    private static final Duration DEFAULT_PRIMARY_SLA = Duration.ofHours(4);
    private static final Duration DEFAULT_ESCALATED_SLA = Duration.ofHours(24);

    /**
     * Checks for overdue approval tickets and triggers escalation.
     * Runs every 5 minutes.
     */
    @Scheduled(
            fixedDelayString = "${approval.escalation.check.interval-ms:300000}",
            initialDelayString = "${approval.escalation.check.initial-delay-ms:300000}"
    )
    public void checkEscalations() {
        Instant now = Instant.now();

        // Find all pending tickets
        LambdaQueryWrapper<ApprovalTicket> wrapper = new LambdaQueryWrapper<ApprovalTicket>()
                .in(ApprovalTicket::getStage, "PENDING_FAST", "PENDING_BPMN")
                .orderByAsc(ApprovalTicket::getCreatedAt);
        List<ApprovalTicket> pendingTickets = approvalTicketMapper.selectList(wrapper);
        if (pendingTickets == null || pendingTickets.isEmpty()) {
            return;
        }

        for (ApprovalTicket ticket : pendingTickets) {
            try {
                checkTicketEscalation(ticket, now);
            } catch (Exception e) {
                log.error("[Escalation] Error checking ticket {}", ticket.getTicketId(), e);
            }
        }
    }

    private void checkTicketEscalation(ApprovalTicket ticket, Instant now) {
        Instant createdAt = ticket.getCreatedAt();
        if (createdAt == null) {
            return;
        }

        Duration elapsed = Duration.between(createdAt, now);
        String stage = ticket.getStage();

        if ("PENDING_FAST".equals(stage)) {
            if (elapsed.compareTo(DEFAULT_PRIMARY_SLA) > 0) {
                log.warn("[Escalation] FAST ticket {} overdue ({}h), escalating to BPMN",
                        ticket.getTicketId(), elapsed.toHours());
                escalationService.escalateFastToBpmn(ticket.getTicketId(),
                        "SLA timeout after " + elapsed.toHours() + "h");
            }
        } else if ("PENDING_BPMN".equals(stage)) {
            // BPMN workflow handles its own SLA via boundary timer events
            // This is a safety net for tickets stuck in PENDING_BPMN without a workflow
            if (elapsed.compareTo(DEFAULT_ESCALATED_SLA.multipliedBy(2)) > 0) {
                log.warn("[Escalation] BPMN ticket {} stuck for {}h — manual intervention needed",
                        ticket.getTicketId(), elapsed.toHours());
            }
        }
    }

    /**
     * Returns the SLA duration for a given approval stage and escalation level.
     */
    public Duration getSlaForStage(String stage, int escalationLevel) {
        if ("PENDING_FAST".equals(stage) && escalationLevel == 0) {
            return DEFAULT_PRIMARY_SLA;
        }
        return DEFAULT_ESCALATED_SLA;
    }
}
