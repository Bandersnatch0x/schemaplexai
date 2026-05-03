package com.schemaplexai.agent.engine.exception;

import com.schemaplexai.agent.engine.context.AgentContext;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exponential backoff retry strategy for transient errors.
 * Supports TIMEOUT, RATE_LIMITED, and INTERNAL_ERROR categories.
 * Default max retries is 3 with exponential backoff starting at 1 second.
 */
public class RetryRecoveryStrategy implements RecoveryStrategy {

    private static final Set<ToolErrorCategory> SUPPORTED_CATEGORIES =
            Set.of(ToolErrorCategory.TIMEOUT,
                   ToolErrorCategory.RATE_LIMITED,
                   ToolErrorCategory.INTERNAL_ERROR);

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000L;

    private final int maxRetries;
    private final long baseDelayMs;
    private final Map<String, Integer> retryCounts = new ConcurrentHashMap<>();

    /**
     * Creates a retry strategy with custom settings.
     */
    public RetryRecoveryStrategy(int maxRetries, long baseDelayMs) {
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
    }

    /**
     * Creates a retry strategy with defaults (max 3 retries, 1s base delay).
     */
    public RetryRecoveryStrategy() {
        this(DEFAULT_MAX_RETRIES, BASE_DELAY_MS);
    }

    @Override
    public RecoveryResult recover(ToolExecutionException error, AgentContext context) {
        String contextKey = buildContextKey(context, error.getErrorCategory());
        int currentRetries = retryCounts.getOrDefault(contextKey, 0);

        if (currentRetries >= maxRetries) {
            retryCounts.remove(contextKey);
            return RecoveryResult.failed(
                    "Exceeded max retries (" + maxRetries + ") for " + error.getErrorCategory()
                    + ": " + error.getMessage());
        }

        int nextRetry = currentRetries + 1;
        retryCounts.put(contextKey, nextRetry);

        // Exponential backoff: delay = baseDelay * 2^(attempt-1)
        long delayMs = baseDelayMs * (1L << (nextRetry - 1));
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RecoveryResult.failed("Retry sleep interrupted: " + e.getMessage());
        }

        return RecoveryResult.retry(
                "Retry attempt " + nextRetry + "/" + maxRetries
                + " for " + error.getErrorCategory()
                + " (delay: " + delayMs + "ms)");
    }

    @Override
    public boolean supports(ToolErrorCategory category) {
        return SUPPORTED_CATEGORIES.contains(category);
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Resets retry count for a given context and category.
     */
    public void resetRetries(AgentContext context, ToolErrorCategory category) {
        retryCounts.remove(buildContextKey(context, category));
    }

    private String buildContextKey(AgentContext context, ToolErrorCategory category) {
        return (context != null ? context.getConversationId() : "unknown") + ":" + category.name();
    }
}
