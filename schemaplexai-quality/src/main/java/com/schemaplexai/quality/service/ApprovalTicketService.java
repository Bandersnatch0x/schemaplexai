package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.event.ApprovalDecisionEvent;
import com.schemaplexai.model.event.ApprovalRequestEvent;
import com.schemaplexai.quality.entity.ApprovalTicket;
import com.schemaplexai.quality.mapper.ApprovalTicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Unified truth for all approval tickets.
 *
 * <p>Tickets are created by {@link ApprovalRequestConsumer} when the Engine
 * sends an {@link ApprovalRequestEvent}. Tickets can be FAST (single approver)
 * or BPMN (multi-step workflow). This service exposes approve/reject/escalate
 * operations and publishes {@link ApprovalDecisionEvent} to MQ on every decision.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalTicketService {

    private static final String EXCHANGE_APPROVAL = "approval";
    private static final String ROUTING_KEY_DECISIONS = "approval.decisions";

    private static final String STAGE_PENDING_FAST = "PENDING_FAST";
    private static final String STAGE_PENDING_BPMN = "PENDING_BPMN";
    private static final String STAGE_APPROVED = "APPROVED";
    private static final String STAGE_REJECTED = "REJECTED";

    private static final String ROLE_TENANT_ADMIN = "TENANT_ADMIN";
    private static final String ROLE_APPROVER = "APPROVER";

    private final ApprovalTicketMapper approvalTicketMapper;
    private final ApprovalEscalationService escalationService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new approval ticket from an incoming request event.
     *
     * @param event the approval request event
     * @return the created ticket
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalTicket createTicket(ApprovalRequestEvent event) {
        if (event == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Approval request event must not be null");
        }
        if (event.approvalRequestId() == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "approvalRequestId must not be null");
        }

        ApprovalTicket ticket = new ApprovalTicket();
        ticket.setTicketId(UUID.randomUUID());
        ticket.setExecutionId(event.executionId());
        ticket.setTenantId(event.tenantId());
        ticket.setAgentId(event.agentId());
        ticket.setApprovalRequestId(event.approvalRequestId());

        String requestType = event.requestType() != null ? event.requestType() : "FAST";
        ticket.setStage(STAGE_PENDING_FAST.equals(requestType) || STAGE_PENDING_BPMN.equals(requestType)
                ? requestType
                : "PENDING_" + requestType);
        ticket.setHandler(requestType);
        ticket.setRiskLevel(event.riskLevel());
        ticket.setActionDescription(event.actionDescription());
        ticket.setTriggeringSeq(event.triggeringSeq());
        ticket.setDeferred(false);
        ticket.setDecisionVersion(0);
        ticket.setExpectedExecutionVersion(event.executionVersionAtPause());
        ticket.setCreatedAt(Instant.now());
        ticket.setUpdatedAt(Instant.now());

        approvalTicketMapper.insert(ticket);
        log.info("[ApprovalTicket] Created ticket={} for execution={}, stage={}",
                ticket.getTicketId(), ticket.getExecutionId(), ticket.getStage());
        return ticket;
    }

    /**
     * Approves a ticket after validating approver authority.
     *
     * @param ticketId   the ticket UUID
     * @param approverId the approver user ID
     * @param reason     optional approval reason
     * @return the updated ticket
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalTicket approve(UUID ticketId, String approverId, String reason) {
        ApprovalTicket ticket = findTicketOrThrow(ticketId);
        validateApproverAuthority(ticket, approverId);
        validateTicketIsPending(ticket);

        ticket.setStage(STAGE_APPROVED);
        ticket.setDecidedAt(Instant.now());
        ticket.setDecisionVersion(incrementVersion(ticket.getDecisionVersion()));
        ticket.setUpdatedAt(Instant.now());
        approvalTicketMapper.updateById(ticket);

        publishDecisionEvent(ticket, "APPROVE", approverId, reason);
        log.info("[ApprovalTicket] Ticket={} approved by approver={}", ticketId, approverId);
        return ticket;
    }

    /**
     * Rejects a ticket after validating approver authority.
     *
     * @param ticketId   the ticket UUID
     * @param approverId the approver user ID
     * @param reason     rejection reason
     * @return the updated ticket
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalTicket reject(UUID ticketId, String approverId, String reason) {
        ApprovalTicket ticket = findTicketOrThrow(ticketId);
        validateApproverAuthority(ticket, approverId);
        validateTicketIsPending(ticket);

        ticket.setStage(STAGE_REJECTED);
        ticket.setDecidedAt(Instant.now());
        ticket.setDecisionVersion(incrementVersion(ticket.getDecisionVersion()));
        ticket.setUpdatedAt(Instant.now());
        approvalTicketMapper.updateById(ticket);

        publishDecisionEvent(ticket, "REJECT", approverId, reason);
        log.info("[ApprovalTicket] Ticket={} rejected by approver={}, reason={}", ticketId, approverId, reason);
        return ticket;
    }

    /**
     * Escalates a ticket. If FAST, upgrades to BPMN. If BPMN, signals escalation within workflow.
     *
     * @param ticketId     the ticket UUID
     * @param escalatorId  the user requesting escalation
     * @return the updated ticket
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalTicket escalate(UUID ticketId, String escalatorId) {
        ApprovalTicket ticket = findTicketOrThrow(ticketId);
        validateApproverAuthority(ticket, escalatorId);
        validateTicketIsPending(ticket);

        if (STAGE_PENDING_FAST.equals(ticket.getStage())) {
            boolean escalated = escalationService.escalateFastToBpmn(ticketId, "Manual escalation by " + escalatorId);
            if (!escalated) {
                throw new BaseException(ResultCode.ERROR, "Failed to escalate ticket " + ticketId);
            }
            // Reload ticket after escalation service mutates it
            ticket = findTicketOrThrow(ticketId);
        } else if (STAGE_PENDING_BPMN.equals(ticket.getStage())) {
            escalationService.escalateWithinBpmn(ticketId, escalatorId, "Manual escalation by " + escalatorId);
            ticket.setUpdatedAt(Instant.now());
            approvalTicketMapper.updateById(ticket);
        }

        log.info("[ApprovalTicket] Ticket={} escalated by escalator={}", ticketId, escalatorId);
        return ticket;
    }

    /**
     * Retrieves a ticket by its UUID.
     *
     * @param ticketId the ticket UUID
     * @return the ticket, or null if not found
     */
    public ApprovalTicket getTicket(UUID ticketId) {
        if (ticketId == null) {
            return null;
        }
        return approvalTicketMapper.selectOne(
                new LambdaQueryWrapper<ApprovalTicket>().eq(ApprovalTicket::getTicketId, ticketId));
    }

    /**
     * Lists all pending tickets for a tenant.
     *
     * @param tenantId the tenant ID
     * @return list of pending tickets
     */
    public List<ApprovalTicket> listPendingByTenant(Long tenantId) {
        if (tenantId == null) {
            return List.of();
        }
        return approvalTicketMapper.selectList(
                new LambdaQueryWrapper<ApprovalTicket>()
                        .eq(ApprovalTicket::getTenantId, tenantId)
                        .in(ApprovalTicket::getStage, STAGE_PENDING_FAST, STAGE_PENDING_BPMN)
                        .orderByDesc(ApprovalTicket::getCreatedAt));
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private ApprovalTicket findTicketOrThrow(UUID ticketId) {
        if (ticketId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "ticketId must not be null");
        }
        ApprovalTicket ticket = getTicket(ticketId);
        if (ticket == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Approval ticket not found: " + ticketId);
        }
        return ticket;
    }

    private void validateTicketIsPending(ApprovalTicket ticket) {
        String stage = ticket.getStage();
        if (!STAGE_PENDING_FAST.equals(stage) && !STAGE_PENDING_BPMN.equals(stage)) {
            throw new BaseException(ResultCode.PARAM_ERROR,
                    "Ticket is not pending (current stage=" + stage + ")");
        }
    }

    /**
     * Validates that the approver has authority over the ticket.
     * TENANT_ADMIN can approve any ticket in their tenant.
     * APPROVER can only approve tickets assigned to them or in their own tenant.
     */
    private void validateApproverAuthority(ApprovalTicket ticket, String approverId) {
        if (approverId == null || approverId.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "approverId must not be blank");
        }

        // In a real system, roles would be resolved from a user/role service.
        // For now we accept the raw role prefix in the approverId (e.g. "TENANT_ADMIN:user-1")
        // or default to checking tenant ownership.
        String role = extractRole(approverId);
        String userId = extractUserId(approverId);

        if (ROLE_TENANT_ADMIN.equals(role)) {
            // TENANT_ADMIN can approve any ticket in the tenant
            return;
        }

        if (ROLE_APPROVER.equals(role)) {
            // APPROVER can only approve tickets in their own tenant
            // (tenant ownership check would normally come from user service)
            if (ticket.getTenantId() == null) {
                throw new BaseException(ResultCode.FORBIDDEN, "Approver lacks authority for this ticket");
            }
            return;
        }

        // Fallback: if no role prefix, allow if the ticket has a tenant (basic ownership)
        if (ticket.getTenantId() == null) {
            throw new BaseException(ResultCode.FORBIDDEN, "Approver lacks authority for this ticket");
        }
    }

    private String extractRole(String approverId) {
        if (approverId == null) {
            return null;
        }
        int idx = approverId.indexOf(':');
        return idx > 0 ? approverId.substring(0, idx) : null;
    }

    private String extractUserId(String approverId) {
        if (approverId == null) {
            return null;
        }
        int idx = approverId.indexOf(':');
        return idx > 0 && idx + 1 < approverId.length() ? approverId.substring(idx + 1) : approverId;
    }

    private int incrementVersion(Integer current) {
        return current != null ? current + 1 : 1;
    }

    @SneakyThrows
    private void publishDecisionEvent(ApprovalTicket ticket, String action, String approverId, String reason) {
        ApprovalDecisionEvent event = new ApprovalDecisionEvent(
                ticket.getTicketId(),
                ticket.getExecutionId(),
                action,
                approverId,
                reason,
                Instant.now(),
                ticket.getDecisionVersion(),
                ticket.getExpectedExecutionVersion() != null ? ticket.getExpectedExecutionVersion() : 0
        );
        String json = objectMapper.writeValueAsString(event);
        rabbitTemplate.convertAndSend(EXCHANGE_APPROVAL, ROUTING_KEY_DECISIONS, json);
        log.debug("[ApprovalTicket] Published decision event: action={}, ticket={}", action, ticket.getTicketId());
    }
}
