package com.schemaplexai.agent.engine.exception;

import com.schemaplexai.agent.engine.context.AgentContext;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback recovery strategy — returns a preset response when a tool fails.
 * Used for non-retryable errors like PERMISSION_DENIED, INVALID_ARGUMENT, RESOURCE_EXHAUSTED.
 * Routes execution to a specified fallback state (typically REFLECTING or COMPLETED).
 */
public class FallbackRecoveryStrategy implements RecoveryStrategy {

    private static final Set<ToolErrorCategory> SUPPORTED_CATEGORIES =
            Set.of(ToolErrorCategory.PERMISSION_DENIED,
                   ToolErrorCategory.INVALID_ARGUMENT,
                   ToolErrorCategory.RESOURCE_EXHAUSTED);

    private final Map<ToolErrorCategory, String> fallbackMessages;
    private final AgentExecutionState fallbackState;

    /**
     * Creates a fallback strategy with custom fallback messages and state.
     *
     * @param fallbackState the state to transition to on fallback
     */
    public FallbackRecoveryStrategy(AgentExecutionState fallbackState) {
        this.fallbackState = fallbackState;
        this.fallbackMessages = new ConcurrentHashMap<>();
        initDefaultMessages();
    }

    /**
     * Creates a fallback strategy with REFLECTING as the default fallback state.
     */
    public FallbackRecoveryStrategy() {
        this(AgentExecutionState.REFLECTING);
    }

    private void initDefaultMessages() {
        fallbackMessages.put(ToolErrorCategory.PERMISSION_DENIED,
                "The requested operation requires permissions that are not currently available.");
        fallbackMessages.put(ToolErrorCategory.INVALID_ARGUMENT,
                "The provided parameters were not valid for the requested operation.");
        fallbackMessages.put(ToolErrorCategory.RESOURCE_EXHAUSTED,
                "The system does not have sufficient resources to complete this operation.");
    }

    /**
     * Registers a custom fallback message for an error category.
     */
    public void registerMessage(ToolErrorCategory category, String message) {
        fallbackMessages.put(category, message);
    }

    @Override
    public RecoveryResult recover(ToolExecutionException error, AgentContext context) {
        String fallbackMessage = fallbackMessages.getOrDefault(
                error.getErrorCategory(),
                "Operation failed: " + error.getMessage());

        return RecoveryResult.fallback(
                "[FALLBACK] " + error.getErrorCategory() + ": " + fallbackMessage,
                fallbackState);
    }

    @Override
    public boolean supports(ToolErrorCategory category) {
        return SUPPORTED_CATEGORIES.contains(category);
    }

    @Override
    public int getMaxRetries() {
        return 0;
    }
}
