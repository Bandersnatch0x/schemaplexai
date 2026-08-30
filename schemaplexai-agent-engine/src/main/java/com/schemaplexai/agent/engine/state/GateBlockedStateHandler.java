package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mq.AgentExecutionEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles GATE_BLOCKED state with AdmissionResult feedback, retry countdown, and MQ notification.
 *
 * State transitions:
 * - retryable AdmissionResult: GATE_BLOCKED → (retryCountdown) → RETRYING
 * - non-retryable: GATE_BLOCKED → FAILED
 */
@Slf4j
@Component
public class GateBlockedStateHandler implements AgentStateHandler {

    private static final int DEFAULT_RETRY_COUNTDOWN_SECONDS = 60;

    private final AgentExecutionEventPublisher eventPublisher;

    public GateBlockedStateHandler(AgentExecutionEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.GATE_BLOCKED;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.warn("Agent {} gate blocked, execution {}",
                execution.getAgentId(), execution.getId());

        // Record admission result from upstream handler (Thinking / ToolCalling)
        String blockedReason = (String) execution.getMetadata("blockedReason");
        if (blockedReason == null) {
            blockedReason = "admission_denied";
        }

        // Determine if retryable based on admission result
        String admissionType = (String) execution.getMetadata("admissionType");
        boolean isRetryable = !"FATAL".equalsIgnoreCase(admissionType);

        if (isRetryable) {
            // Set retry countdown
            execution.setMetadata("retryCountdown", String.valueOf(DEFAULT_RETRY_COUNTDOWN_SECONDS));
            execution.setState(AgentExecutionState.GATE_BLOCKED.name());
            stateMachine.saveExecution(execution);

            // Publish blocked event for notification/alerting.
            // The publisher enriches the payload via put(), so the map must be mutable
            // (Map.of would trigger UnsupportedOperationException and degrade to FAILED).
            Map<String, Object> retryablePayload = new HashMap<>();
            retryablePayload.put("executionId", execution.getId());
            retryablePayload.put("agentId", execution.getAgentId());
            retryablePayload.put("reason", blockedReason);
            retryablePayload.put("retryable", true);
            retryablePayload.put("retryCountdown", DEFAULT_RETRY_COUNTDOWN_SECONDS);
            eventPublisher.publishExecutionEvent("AGENT_GATE_BLOCKED", retryablePayload);

            log.info("Gate blocked retryable for execution {}, transitioning to RETRYING after {}s",
                    execution.getId(), DEFAULT_RETRY_COUNTDOWN_SECONDS);

            stateMachine.transition(AgentExecutionState.RETRYING, execution);
        } else {
            // Non-retryable admission denial — permanent failure.
            // Mutable map required: the publisher enriches the payload via put().
            Map<String, Object> fatalPayload = new HashMap<>();
            fatalPayload.put("executionId", execution.getId());
            fatalPayload.put("agentId", execution.getAgentId());
            fatalPayload.put("reason", blockedReason);
            fatalPayload.put("retryable", false);
            eventPublisher.publishExecutionEvent("AGENT_GATE_BLOCKED", fatalPayload);

            log.warn("Gate blocked permanently for execution {}, reason: {}", execution.getId(), blockedReason);
            stateMachine.transition(AgentExecutionState.FAILED, execution);
        }
    }
}
