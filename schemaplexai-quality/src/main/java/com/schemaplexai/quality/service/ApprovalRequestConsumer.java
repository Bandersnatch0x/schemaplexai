package com.schemaplexai.quality.service;

import com.schemaplexai.model.event.ApprovalRequestEvent;
import com.schemaplexai.quality.entity.ApprovalTicket;
import com.schemaplexai.quality.mapper.ApprovalTicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQ consumer for approval requests with idempotency guard.
 * Phase 1: in-memory dedup; Phase 2+ will use sf_processed_event table.
 */
@Service
@RequiredArgsConstructor
public class ApprovalRequestConsumer {

    private final ApprovalTicketMapper approvalTicketMapper;
    private final Set<UUID> processed = ConcurrentHashMap.newKeySet();

    public void consume(ApprovalRequestEvent event) {
        if (event == null || event.approvalRequestId() == null) {
            return;
        }

        if (!processed.add(event.approvalRequestId())) {
            return; // already processed
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
    }
}
