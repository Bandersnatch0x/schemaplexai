package com.schemaplexai.agent.engine.orchestrator;

import com.schemaplexai.agent.engine.admission.ExecutionAdmissionService;
import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.lifecycle.AgentExecutionLifecycleService;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.observability.ObservabilityRecorder;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import com.schemaplexai.common.constants.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AgentRuntimeOrchestrator {

    private final AgentStateMachine stateMachine;
    private final ExecutionAdmissionService admissionService;
    private final CompositeChatMemoryStore chatMemoryStore;
    private final ObservabilityRecorder observabilityRecorder;
    private final com.schemaplexai.agent.engine.config.AgentEngineProperties engineProperties;
    private final StringRedisTemplate redisTemplate;
    private final AgentExecutionLifecycleService lifecycleService;

    public AgentRuntimeOrchestrator(
            AgentStateMachine stateMachine,
            ExecutionAdmissionService admissionService,
            CompositeChatMemoryStore chatMemoryStore,
            ObservabilityRecorder observabilityRecorder,
            com.schemaplexai.agent.engine.config.AgentEngineProperties engineProperties,
            StringRedisTemplate redisTemplate,
            @Lazy AgentExecutionLifecycleService lifecycleService) {
        this.stateMachine = stateMachine;
        this.admissionService = admissionService;
        this.chatMemoryStore = chatMemoryStore;
        this.observabilityRecorder = observabilityRecorder;
        this.engineProperties = engineProperties;
        this.redisTemplate = redisTemplate;
        this.lifecycleService = lifecycleService;
    }

    private static final int MAX_ITERATIONS = 50;

    /** Volatile flag to signal cancellation from another thread. */
    private volatile boolean cancelled;

    /**
     * Check whether this execution has been externally paused or cancelled.
     *
     * @param executionId the execution ID to check
     * @return true if paused, false otherwise
     */
    private boolean isPaused(Long executionId) {
        String key = String.format(CommonConstants.REDIS_KEY_EXECUTION_PAUSED, executionId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null;
    }

    /**
     * Signal cancellation. Used by cancelExecution() via lifecycle service.
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Reset the cancellation flag for a new execution run.
     */
    private void resetCancelled() {
        this.cancelled = false;
    }

    public void run(SfAgentExecution execution, String tenantId, String prompt) {
        String traceId = observabilityRecorder.startTrace(
            String.valueOf(execution.getId()),
            "agent-execution",
            String.valueOf(execution.getCreatedBy()),
            execution.getConversationId(),
            prompt
        ).getTraceId();

        int roundCount = 0;
        try {
            // Reset cancellation flag for this run
            resetCancelled();

            // Initialize token budget (with tool-call limit)
            TokenBudget tokenBudget = new TokenBudget(
                    CommonConstants.DEFAULT_MAX_INPUT_TOKENS,
                    CommonConstants.DEFAULT_MAX_OUTPUT_TOKENS,
                    engineProperties.getMaxToolCalls()
            );
            execution.setTokenBudgetJson(serializeBudget(tokenBudget));

            // Admission check
            var admission = admissionService.admit(tenantId, execution.getAgentId(), tokenBudget);
            if (!admission.isAllowed()) {
                log.warn("Execution {} admission denied: {}", execution.getId(), admission.getReason());
                stateMachine.transition(AgentExecutionState.GATE_BLOCKED, execution);
                return;
            }

            // Save user prompt to memory
            chatMemoryStore.saveMessage(execution.getConversationId(), new LlmMessage("user", prompt));

            // Start the state machine
            stateMachine.start(execution);

            // State machine loop with iteration guard and interruptibility
            int iteration = 0;
            while (iteration < MAX_ITERATIONS) {
                // Check for external pause signal (Redis key set by pauseExecution API)
                if (isPaused(execution.getId())) {
                    log.info("Execution {} pause signal detected, transitioning to PAUSED", execution.getId());
                    stateMachine.transition(AgentExecutionState.PAUSED, execution);
                    break;
                }

                // Check for cancellation signal (volatile flag set by cancelExecution)
                if (cancelled || Thread.currentThread().isInterrupted()) {
                    log.info("Execution {} cancellation signal detected, transitioning to CANCELLED", execution.getId());
                    stateMachine.transition(AgentExecutionState.CANCELLED, execution);
                    break;
                }

                AgentExecutionState currentState = stateMachine.getCurrentState(execution.getId());
                if (currentState == null || currentState.isTerminal()) {
                    break;
                }
                stateMachine.transition(currentState, execution);
                iteration++;
            }
            roundCount = iteration;

            if (iteration >= MAX_ITERATIONS && !cancelled && !isPaused(execution.getId())) {
                log.warn("Execution {} hit max iterations, forcing completion", execution.getId());
                stateMachine.transition(AgentExecutionState.COMPLETED, execution);
            }
        } catch (Exception e) {
            log.error("Execution {} failed", execution.getId(), e);
            try {
                stateMachine.transition(AgentExecutionState.FAILED, execution);
            } catch (Exception ex) {
                log.error("Failed to transition execution {} to FAILED", execution.getId(), ex);
            }
        } finally {
            observabilityRecorder.endTrace(traceId,
                "{\"state\":\"" + execution.getState() + "\",\"rounds\":" + roundCount + "}");
            admissionService.releaseConcurrency(tenantId, execution.getAgentId());
        }
    }

    private String serializeBudget(TokenBudget budget) {
        return budget.getMaxInputTokens() + "," + budget.getMaxOutputTokens() + ",0,0," + budget.getMaxToolCalls() + ",0";
    }
}
