package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.context.AgentContext;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.exception.FallbackRecoveryStrategy;
import com.schemaplexai.agent.engine.exception.RecoveryResult;
import com.schemaplexai.agent.engine.exception.RecoveryStrategy;
import com.schemaplexai.agent.engine.exception.RetryRecoveryStrategy;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the EXCEPTION_HANDLING pseudo-state by selecting an appropriate
 * RecoveryStrategy based on the ToolErrorCategory of the exception.
 *
 * State machine flow:
 *   Any state → (error) → EXCEPTION_HANDLING → RETRYING / REFLECTING / FAILED
 *
 * This handler does NOT map to a formal AgentExecutionState enum value;
 * it intercepts error conditions and routes to the appropriate recovery path.
 */
@Slf4j
@Component
public class ExceptionHandlingStateHandler {

    private final List<RecoveryStrategy> recoveryStrategies;
    private final Map<String, Integer> globalRetryCounts = new ConcurrentHashMap<>();

    /**
     * Constructs the handler with all available RecoveryStrategy implementations.
     */
    public ExceptionHandlingStateHandler(List<RecoveryStrategy> recoveryStrategies) {
        this.recoveryStrategies = recoveryStrategies;
        if (recoveryStrategies.isEmpty()) {
            // Register defaults if none injected
            this.recoveryStrategies.add(new RetryRecoveryStrategy());
            this.recoveryStrategies.add(new FallbackRecoveryStrategy());
        }
    }

    /**
     * Handles an exception that occurred during tool execution.
     * Selects the appropriate RecoveryStrategy and transitions the state machine.
     *
     * @param stateMachine the state machine to transition
     * @param execution    the current execution entity
     * @param error        the tool execution exception
     * @param context      agent execution context
     */
    public void handleException(AgentStateMachine stateMachine,
                                SfAgentExecution execution,
                                ToolExecutionException error,
                                AgentContext context) {
        ToolErrorCategory category = error.getErrorCategory();
        log.warn("Handling exception for execution {}: category={}, message={}",
                execution.getId(), category, error.getMessage());

        // Find matching recovery strategy
        RecoveryStrategy strategy = findStrategy(category);
        if (strategy == null) {
            log.error("No recovery strategy found for category {}, failing execution {}",
                    category, execution.getId());
            stateMachine.transition(AgentExecutionState.FAILED, execution);
            return;
        }

        // Check global retry budget
        String globalKey = execution.getId() + ":" + category.name();
        int globalRetries = globalRetryCounts.getOrDefault(globalKey, 0);
        int maxRetries = strategy.getMaxRetries();
        if (maxRetries > 0 && globalRetries >= maxRetries * 2) {
            log.error("Global retry budget exhausted for execution {} category {}",
                    execution.getId(), category);
            stateMachine.transition(AgentExecutionState.FAILED, execution);
            globalRetryCounts.remove(globalKey);
            return;
        }

        // Execute recovery
        RecoveryResult result = strategy.recover(error, context);

        switch (result.type()) {
            case RETRY -> {
                globalRetryCounts.merge(globalKey, 1, Integer::sum);
                log.info("Recovery RETRY for execution {}: {}", execution.getId(), result.message());
                if (stateMachine.getCurrentState(execution.getId()) != null) {
                    stateMachine.transition(AgentExecutionState.RETRYING, execution);
                }
            }
            case FALLBACK -> {
                AgentExecutionState nextState = result.nextState() != null
                        ? result.nextState()
                        : AgentExecutionState.REFLECTING;
                log.info("Recovery FALLBACK for execution {} to state {}: {}",
                        execution.getId(), nextState, result.message());
                globalRetryCounts.remove(globalKey);
                stateMachine.transition(nextState, execution);
            }
            case FAILED -> {
                log.error("Recovery FAILED for execution {}: {}", execution.getId(), result.message());
                globalRetryCounts.remove(globalKey);
                stateMachine.transition(AgentExecutionState.FAILED, execution);
            }
        }
    }

    /**
     * Finds the first recovery strategy that supports the given error category.
     */
    private RecoveryStrategy findStrategy(ToolErrorCategory category) {
        for (RecoveryStrategy strategy : recoveryStrategies) {
            if (strategy.supports(category)) {
                return strategy;
            }
        }
        return null;
    }

    /**
     * Resets retry counts for a specific execution (e.g., after successful recovery).
     */
    public void resetRetries(Long executionId) {
        globalRetryCounts.keySet().removeIf(key -> key.startsWith(executionId + ":"));
    }
}
