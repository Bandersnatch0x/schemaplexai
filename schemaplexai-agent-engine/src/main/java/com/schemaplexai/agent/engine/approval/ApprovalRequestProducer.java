package com.schemaplexai.agent.engine.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.service.ExecutionEventService;
import com.schemaplexai.model.event.ApprovalRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Produces ApprovalRequestEvent to the Outbox for MQ delivery to Core.
 *
 * <p>Flow:
 * <ol>
 *   <li>Generate UUIDv5 approvalRequestId</li>
 *   <li>Write ExecutionEvent (APPROVAL_REQUESTED) via ExecutionEventService</li>
 *   <li>Write Outbox entry (topic: approval.requests)</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalRequestProducer {

    private final ExecutionEventService executionEventService;
    private final ObjectMapper objectMapper;

    /** MQ topic for approval requests. */
    public static final String TOPIC_APPROVAL_REQUESTS = "approval.requests";

    /** MQ topic for deferred approval requests (Core unreachable). */
    public static final String TOPIC_APPROVAL_DEFERRED = "approval.requests.deferred";

    /**
     * Produces an approval request event and writes it to the outbox.
     *
     * @param executionId          the execution ID
     * @param tenantId             the tenant ID
     * @param agentId              the agent ID
     * @param triggeringSeq        the event sequence that triggered approval
     * @param requestType          FAST or BPMN
     * @param riskLevel            the risk level of the tool call
     * @param actionDescription    human-readable description
     * @param executionVersion     current execution version at pause
     * @param deferred             true if this is a deferred request (Core unreachable)
     * @return the generated approvalRequestId
     */
    @Transactional
    public UUID produce(Long executionId, Long tenantId, Long agentId,
                        int triggeringSeq, String requestType, String riskLevel,
                        String actionDescription, int executionVersion,
                        boolean deferred) {
        UUID approvalRequestId = generateApprovalRequestId(executionId, triggeringSeq);
        String topic = deferred ? TOPIC_APPROVAL_DEFERRED : TOPIC_APPROVAL_REQUESTS;

        // Build the ApprovalRequestEvent
        ApprovalRequestEvent event = new ApprovalRequestEvent(
                approvalRequestId,
                executionId,
                tenantId,
                agentId,
                triggeringSeq,
                requestType,
                riskLevel,
                actionDescription,
                executionVersion,
                Instant.now()
        );

        // Write ExecutionEvent to append-only log + Outbox for MQ delivery
        ExecutionEvent execEvent = buildExecutionEvent(executionId, tenantId, agentId,
                triggeringSeq, event);
        executionEventService.appendEventAndOutbox(execEvent, topic);

        log.info("Produced approval request {} for execution {} (seq={}, type={}, deferred={})",
                approvalRequestId, executionId, triggeringSeq, requestType, deferred);

        return approvalRequestId;
    }

    /**
     * Generates a deterministic UUIDv5 for idempotency.
     * Key: executionId + ":approval:" + triggeringSeq
     */
    private UUID generateApprovalRequestId(Long executionId, int triggeringSeq) {
        String namespace = "execution-approval";
        String input = executionId + ":approval:" + triggeringSeq;
        // UUIDv5 simulation using namespace + input hash
        return UUID.nameUUIDFromBytes((namespace + ":" + input).getBytes());
    }

    private ExecutionEvent buildExecutionEvent(Long executionId, Long tenantId, Long agentId,
                                                int triggeringSeq, ApprovalRequestEvent event) {
        ExecutionEvent execEvent = new ExecutionEvent();
        execEvent.setEventId(event.approvalRequestId());
        execEvent.setExecutionId(executionId);
        execEvent.setSeq(triggeringSeq);
        execEvent.setEventType("APPROVAL_REQUESTED");
        execEvent.setTenantId(tenantId);
        execEvent.setAgentId(agentId);
        execEvent.setOccurredAt(event.createdAt());
        execEvent.setSensitivity("AUDIT");
        try {
            execEvent.setPayload(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.warn("Failed to serialize approval request event payload", e);
            execEvent.setPayload("{}");
        }
        return execEvent;
    }

}
