package com.schemaplexai.agent.engine.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.mapper.ExecutionEventMapper;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.service.ExecutionConcurrencyService;
import com.schemaplexai.agent.engine.sse.ExecutionEventBus;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import com.schemaplexai.model.event.ApprovalDecisionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQ consumer for approval decisions from Core.
 *
 * <p>Subscribes to {@code approval.decisions} topic.
 * On receipt:
 * <ol>
 *   <li>Validate expectedExecutionVersion matches current version</li>
 *   <li>Validate decisionVersion is monotonically increasing</li>
 *   <li>Transition state: PAUSED → RUNNING (on APPROVE)</li>
 *   <li>Transition state: PAUSED → REJECTED (on REJECT)</li>
 *   <li>Write ExecutionEvent (APPROVAL_GRANTED or APPROVAL_REJECTED)</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalDecisionConsumer {

    private final AgentStateMachine stateMachine;
    private final SfAgentExecutionMapper executionMapper;
    private final ExecutionConcurrencyService concurrencyService;
    private final ExecutionEventBus eventBus;
    private final ExecutionEventMapper executionEventMapper;
    private final ObjectMapper objectMapper;

    /** Tracks last decisionVersion per execution for monotonic check. */
    private final Map<Long, Integer> lastDecisionVersions = new ConcurrentHashMap<>();

    /**
     * RabbitMQ listener for approval decisions.
     *
     * @param message the raw JSON message from MQ
     */
    @RabbitListener(queues = "${mq.queue.approval.decisions:approval.decisions}")
    public void onMessage(String message) {
        try {
            ApprovalDecisionEvent event = objectMapper.readValue(message, ApprovalDecisionEvent.class);
            consume(event);
        } catch (Exception e) {
            log.error("Failed to deserialize approval decision message: {}", message, e);
        }
    }

    /**
     * Processes an approval decision event.
     *
     * @param event the deserialized decision event
     */
    public void consume(ApprovalDecisionEvent event) {
        if (event == null || event.ticketId() == null || event.executionId() == null) {
            log.warn("Invalid approval decision event: null fields");
            return;
        }

        Long executionId = event.executionId();

        // 1. Version validation: expectedExecutionVersion must match current
        SfAgentExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            log.warn("Execution {} not found for approval decision, dropping event", executionId);
            return;
        }

        int expectedVersion = event.expectedExecutionVersion();
        if (expectedVersion > 0 && !Integer.valueOf(expectedVersion).equals(execution.getVersion())) {
            log.warn("Version conflict for execution {}: expected={}, current={}",
                    executionId, expectedVersion, execution.getVersion());
            emitVersionConflictEvent(execution, event);
            return;
        }

        // 2. Monotonic decisionVersion check
        int currentLastVersion = lastDecisionVersions.getOrDefault(executionId, 0);
        if (event.decisionVersion() > 0 && event.decisionVersion() <= currentLastVersion) {
            log.warn("Non-monotonic decisionVersion for execution {}: received={}, last={}",
                    executionId, event.decisionVersion(), currentLastVersion);
            return; // duplicate or out-of-order, drop silently
        }
        lastDecisionVersions.put(executionId, event.decisionVersion());

        // 3. State validation: must be in PAUSED state
        AgentExecutionState currentState = stateMachine.getCurrentState(executionId);
        if (currentState != AgentExecutionState.PAUSED) {
            log.warn("Execution {} not in PAUSED state (current={}), dropping approval decision",
                    executionId, currentState);
            return;
        }

        // 4. Apply decision
        switch (event.action()) {
            case "APPROVE" -> handleApprove(execution, event);
            case "REJECT" -> handleReject(execution, event);
            default -> log.warn("Unknown approval action: {} for execution {}", event.action(), executionId);
        }
    }

    private void handleApprove(SfAgentExecution execution, ApprovalDecisionEvent event) {
        log.info("Approval granted for execution {} (ticket={}, approver={})",
                execution.getId(), event.ticketId(), event.approverId());

        // Transition PAUSED → RESUMING (resume execution after approval)
        stateMachine.transition(AgentExecutionState.RESUMING, execution);

        // Write ExecutionEvent
        emitApprovalEvent(execution, "APPROVAL_GRANTED", event);
    }

    private void handleReject(SfAgentExecution execution, ApprovalDecisionEvent event) {
        log.info("Approval rejected for execution {} (ticket={}, approver={}, reason={})",
                execution.getId(), event.ticketId(), event.approverId(), event.reason());

        // Transition PAUSED → REJECTED (terminal)
        stateMachine.transition(AgentExecutionState.REJECTED, execution);

        // Write ExecutionEvent
        emitApprovalEvent(execution, "APPROVAL_REJECTED", event);
    }

    private void emitApprovalEvent(SfAgentExecution execution, String eventType,
                                    ApprovalDecisionEvent event) {
        try {
            ExecutionEvent execEvent = new ExecutionEvent();
            execEvent.setEventId(UUID.randomUUID());
            execEvent.setExecutionId(execution.getId());
            execEvent.setEventType(eventType);
            execEvent.setTenantId(execution.getTenantId() != null ? Long.valueOf(execution.getTenantId()) : null);
            execEvent.setAgentId(execution.getAgentId());
            execEvent.setOccurredAt(event.decidedAt() != null ? event.decidedAt() : Instant.now());
            execEvent.setSensitivity("AUDIT");

            Map<String, Object> payload = Map.of(
                    "ticketId", event.ticketId(),
                    "approverId", event.approverId(),
                    "reason", event.reason() != null ? event.reason() : "",
                    "decisionVersion", event.decisionVersion()
            );
            execEvent.setPayload(objectMapper.writeValueAsString(payload));

            // Use next seq
            int nextSeq = (execution.getLastEventSeq() != null ? execution.getLastEventSeq() : 0) + 1;
            execEvent.setSeq(nextSeq);

            executionEventMapper.insert(execEvent);

            // Broadcast via SSE
            eventBus.publishStateTransition(execution.getId(),
                    AgentExecutionState.PAUSED,
                    eventType.equals("APPROVAL_GRANTED") ? AgentExecutionState.RESUMING : AgentExecutionState.REJECTED);

            log.debug("Emitted {} event for execution {} (seq={})", eventType, execution.getId(), nextSeq);
        } catch (Exception e) {
            log.error("Failed to emit approval event for execution {}", execution.getId(), e);
        }
    }

    private void emitVersionConflictEvent(SfAgentExecution execution, ApprovalDecisionEvent event) {
        try {
            ExecutionEvent execEvent = new ExecutionEvent();
            execEvent.setEventId(UUID.randomUUID());
            execEvent.setExecutionId(execution.getId());
            execEvent.setEventType("VERSION_CONFLICT");
            execEvent.setTenantId(execution.getTenantId() != null ? Long.valueOf(execution.getTenantId()) : null);
            execEvent.setAgentId(execution.getAgentId());
            execEvent.setOccurredAt(Instant.now());
            execEvent.setSensitivity("AUDIT");

            Map<String, Object> payload = Map.of(
                    "ticketId", event.ticketId(),
                    "expectedVersion", event.expectedExecutionVersion(),
                    "actualVersion", execution.getVersion()
            );
            execEvent.setPayload(objectMapper.writeValueAsString(payload));

            int nextSeq = (execution.getLastEventSeq() != null ? execution.getLastEventSeq() : 0) + 1;
            execEvent.setSeq(nextSeq);

            executionEventMapper.insert(execEvent);
        } catch (Exception e) {
            log.error("Failed to emit version conflict event for execution {}", execution.getId(), e);
        }
    }
}
