package com.schemaplexai.agent.engine.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.service.ExecutionEventService;
import com.schemaplexai.agent.engine.sse.ExecutionEventBus;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * MQ consumer for deferred approval activation from Core.
 *
 * <p>When Core recovers after being unreachable, it sends deferred approval activation events.
 * This consumer validates the execution state and transitions GATE_BLOCKED → PAUSED.
 *
 * <p>Flow:
 * <ol>
 *   <li>Receive event from {@code approval.deferred.created} topic</li>
 *   <li>Validate execution.state == GATE_BLOCKED</li>
 *   <li>Transition: GATE_BLOCKED → PAUSED</li>
 *   <li>Write ExecutionEvent (DEFERRED_APPROVAL_ACTIVATED)</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalDeferredCreatedConsumer {

    private final AgentStateMachine stateMachine;
    private final SfAgentExecutionMapper executionMapper;
    private final ExecutionEventBus eventBus;
    private final ExecutionEventService executionEventService;
    private final ObjectMapper objectMapper;

    /**
     * RabbitMQ listener for deferred approval activation events.
     *
     * @param message the raw JSON message from MQ
     */
    @RabbitListener(queues = "${mq.queue.approval.deferred.created:approval.deferred.created}")
    public void onMessage(String message) {
        try {
            DeferredApprovalActivationEvent event = objectMapper.readValue(message, DeferredApprovalActivationEvent.class);
            consume(event);
        } catch (Exception e) {
            log.error("Failed to deserialize deferred approval activation message: {}", message, e);
        }
    }

    /**
     * Processes a deferred approval activation event.
     *
     * @param event the deserialized activation event
     */
    public void consume(DeferredApprovalActivationEvent event) {
        if (event == null || event.executionId() == null) {
            log.warn("Invalid deferred approval activation event: null fields");
            return;
        }

        Long executionId = event.executionId();

        // Load execution
        SfAgentExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            log.warn("Execution {} not found for deferred activation, dropping event", executionId);
            return;
        }

        // Validate execution is in GATE_BLOCKED state
        AgentExecutionState currentState = stateMachine.getCurrentState(executionId);
        // Also check the entity state (in-memory map might not have the entry)
        String entityState = execution.getState();
        boolean isGateBlocked = currentState == AgentExecutionState.GATE_BLOCKED
                || "GATE_BLOCKED".equals(entityState);

        if (!isGateBlocked) {
            log.info("Execution {} not in GATE_BLOCKED state (inMemory={}, entity={}), "
                    + "dropping deferred activation silently",
                    executionId, currentState, entityState);
            return;
        }

        log.info("Deferred approval activated for execution {} (requestId={})",
                executionId, event.approvalRequestId());

        // Transition GATE_BLOCKED → PAUSED
        stateMachine.transition(AgentExecutionState.PAUSED, execution);

        // Write ExecutionEvent
        emitActivationEvent(execution, event);
    }

    private void emitActivationEvent(SfAgentExecution execution,
                                      DeferredApprovalActivationEvent event) {
        try {
            ExecutionEvent execEvent = new ExecutionEvent();
            execEvent.setEventId(UUID.randomUUID());
            execEvent.setExecutionId(execution.getId());
            execEvent.setEventType("DEFERRED_APPROVAL_ACTIVATED");
            execEvent.setTenantId(execution.getTenantId() != null ? Long.valueOf(execution.getTenantId()) : null);
            execEvent.setAgentId(execution.getAgentId());
            execEvent.setOccurredAt(Instant.now());
            execEvent.setSensitivity("AUDIT");

            Map<String, Object> payload = Map.of(
                    "approvalRequestId", event.approvalRequestId(),
                    "activatedAt", Instant.now().toString()
            );
            execEvent.setPayload(objectMapper.writeValueAsString(payload));

            int nextSeq = (execution.getLastEventSeq() != null ? execution.getLastEventSeq() : 0) + 1;
            execEvent.setSeq(nextSeq);

            executionEventService.writeEvent(execEvent);

            // Update lastEventSeq
            execution.setLastEventSeq(nextSeq);
            executionMapper.updateById(execution);

            // Broadcast via SSE
            eventBus.publishStateTransition(execution.getId(),
                    AgentExecutionState.GATE_BLOCKED, AgentExecutionState.PAUSED);

            log.debug("Emitted DEFERRED_APPROVAL_ACTIVATED event for execution {} (seq={})",
                    execution.getId(), nextSeq);
        } catch (Exception e) {
            log.error("Failed to emit deferred activation event for execution {}",
                    execution.getId(), e);
        }
    }

    /**
     * Internal record for deferred approval activation events from MQ.
     */
    public record DeferredApprovalActivationEvent(
            String approvalRequestId,
            Long executionId,
            Long tenantId,
            Instant activatedAt
    ) {
    }
}
