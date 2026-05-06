package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mq.AgentExecutionEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

            // Publish blocked event for notification/alerting
            eventPublisher.publishExecutionEvent("AGENT_GATE_BLOCKED", Map.of(
                    "executionId", execution.getId(),
                    "agentId", execution.getAgentId(),
                    "reason", blockedReason,
                    "retryable", true,
                    "retryCountdown", DEFAULT_RETRY_COUNTDOWN_SECONDS
            ));

            log.info("Gate blocked retryable for execution {}, transitioning to RETRYING after {}s",
                    execution.getId(), DEFAULT_RETRY_COUNTDOWN_SECONDS);

            stateMachine.transition(AgentExecutionState.RETRYING, execution);
        } else {
            // Non-retryable admission denial — permanent failure
            eventPublisher.publishExecutionEvent("AGENT_GATE_BLOCKED", Map.of(
                    "executionId", execution.getId(),
                    "agentId", execution.getAgentId(),
                    "reason", blockedReason,
                    "retryable", false
            ));

            log.warn("Gate blocked permanently for execution {}, reason: {}", execution.getId(), blockedReason);
            stateMachine.transition(AgentExecutionState.FAILED, execution);
        }
    }
}
