package com.schemaplexai.quality.service;

import com.schemaplexai.model.event.ApprovalRequestEvent;
import com.schemaplexai.quality.entity.ApprovalTicket;
import com.schemaplexai.quality.mapper.ApprovalTicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * MQ consumer for approval requests with DB-backed idempotency via InboxDeduplicationService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalRequestConsumer {

    private static final String CONSUMER_NAME = "ApprovalRequestConsumer";

    private final ApprovalTicketMapper approvalTicketMapper;
    private final InboxDeduplicationService dedupService;

    @Transactional
    public void consume(ApprovalRequestEvent event) {
        if (event == null || event.approvalRequestId() == null) {
            return;
        }

        UUID eventId = event.approvalRequestId();
        if (dedupService.isProcessed(eventId, CONSUMER_NAME)) {
            log.debug("[ApprovalRequestConsumer] Duplicate approval request skipped: eventId={}", eventId);
            return;
        }

        ApprovalTicket ticket = new ApprovalTicket();
        ticket.setTicketId(UUID.randomUUID());
        ticket.setExecutionId(event.executionId());
        ticket.setTenantId(event.tenantId());
        ticket.setAgentId(event.agentId());
        ticket.setApprovalRequestId(event.approvalRequestId());
        ticket.setStage("PENDING_" + event.requestType());
        ticket.setHandler(event.requestType());
        ticket.setRiskLevel(event.riskLevel());
        ticket.setActionDescription(event.actionDescription());
        ticket.setTriggeringSeq(event.triggeringSeq());
        ticket.setDeferred(false);
        ticket.setDecisionVersion(0);
        ticket.setCreatedAt(Instant.now());
        ticket.setUpdatedAt(Instant.now());

        approvalTicketMapper.insert(ticket);

        dedupService.markProcessed(eventId, CONSUMER_NAME);
    }
}
