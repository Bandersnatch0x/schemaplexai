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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

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

    /** TTL for the per-execution cancellation signal (mirrors the pause key). */
    private static final Duration CANCEL_KEY_TTL = Duration.ofHours(24);

    /** Sub-category of the per-execution cancellation key. */
    private static final String CANCEL_SUBKEY = "cancelled";

    /**
     * Build the cancellation key for ONE execution
     * ({@code sf:{tenantId}:execution:cancelled:{executionId}}, TenantRedisKeyResolver
     * style). Executions without a tenant (e.g. dynamic sub-agents) fall back to the
     * global namespace — the key is still scoped to the single executionId.
     */
    static String cancelKey(String tenantId, Long executionId) {
        if (tenantId == null || tenantId.isBlank()) {
            return com.schemaplexai.common.redis.TenantRedisKeyResolver.globalKey(
                    com.schemaplexai.common.redis.TenantRedisKeyResolver.CAT_EXECUTION,
                    CANCEL_SUBKEY, String.valueOf(executionId));
        }
        return com.schemaplexai.common.redis.TenantRedisKeyResolver.tenantKey(
                tenantId,
                com.schemaplexai.common.redis.TenantRedisKeyResolver.CAT_EXECUTION,
                CANCEL_SUBKEY, String.valueOf(executionId));
    }

    /**
     * Check whether this execution has been externally paused.
     *
     * @param executionId the execution ID to check
     * @return true if paused, false otherwise
     */
    private boolean isPaused(Long executionId, String tenantId) {
        String key = com.schemaplexai.common.redis.TenantRedisKeyResolver.executionPaused(
                tenantId, String.valueOf(executionId));
        String value = redisTemplate.opsForValue().get(key);
        return value != null;
    }

    /**
     * Check whether THIS execution has been externally cancelled.
     *
     * <p>Per-execution isolation (issue 906 / REQ-27): the signal is a Redis key
     * scoped to {@code executionId}, so cancelling one execution never affects any
     * other concurrent execution on this instance.</p>
     */
    private boolean isCancelled(Long executionId, String tenantId) {
        String value = redisTemplate.opsForValue().get(cancelKey(tenantId, executionId));
        return value != null;
    }

    /**
     * Signal cancellation for ONE specific execution (issue 906 / REQ-27).
     *
     * <p>Replaces the former global {@code volatile boolean cancelled} flag on this
     * singleton: only the targeted execution's Redis key is set, so concurrently
     * running executions are unaffected, and a signal raised before the target's run
     * loop starts is still honored (there is no global reset that could wipe it).</p>
     *
     * @param tenantId    the tenant of the execution (nullable for tenantless sub-agents)
     * @param executionId the execution to cancel
     */
    public void cancel(String tenantId, Long executionId) {
        String key = cancelKey(tenantId, executionId);
        redisTemplate.opsForValue().set(key, "CANCELLED", CANCEL_KEY_TTL);
        log.info("Cancellation signal set for execution {} (key={})", executionId, key);
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
            // NOTE: there is deliberately NO global cancellation reset here (issue 906).
            // The per-execution cancel key is authoritative: a signal raised before
            // this run started must still cancel this execution.

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
                if (isPaused(execution.getId(), tenantId)) {
                    log.info("Execution {} pause signal detected, transitioning to PAUSED", execution.getId());
                    stateMachine.transition(AgentExecutionState.PAUSED, execution);
                    break;
                }

                // Check THIS execution's cancellation signal (per-execution Redis key,
                // issue 906 — never a shared/global flag)
                if (isCancelled(execution.getId(), tenantId) || Thread.currentThread().isInterrupted()) {
                    log.info("Execution {} cancellation signal detected, transitioning to CANCELLED", execution.getId());
                    // Consume the one-shot signal once observed
                    redisTemplate.delete(cancelKey(tenantId, execution.getId()));
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

            if (iteration >= MAX_ITERATIONS && !isCancelled(execution.getId(), tenantId)
                    && !isPaused(execution.getId(), tenantId)) {
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
