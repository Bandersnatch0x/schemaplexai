package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles the RETRYING state with exponential backoff and circuit breaker.
 *
 * Retry strategy (addressing Review Action Item #6):
 * - Only replays the FAILED tool call, NOT the full conversation history
 * - Token cost: ~200-500 tokens per retry vs ~5000-20000 for full history
 * - Retry context is passed via execution metadata (retryContext)
 */
@Slf4j
@Component
public class RetryingStateHandler implements AgentStateHandler {

    private final boolean enabled;
    private final int maxRetries;
    private final long baseDelayMs;
    private final long maxDelayMs;
    private final Map<Long, AtomicInteger> retryCounters = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> failureStreaks = new ConcurrentHashMap<>();

    public RetryingStateHandler(
            @Value("${agent.retry.enabled:true}") boolean enabled,
            @Value("${agent.retry.max-retries:3}") int maxRetries,
            @Value("${agent.retry.base-delay-ms:100}") long baseDelayMs,
            @Value("${agent.retry.max-delay-ms:30000}") long maxDelayMs) {
        this.enabled = enabled;
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
        this.maxDelayMs = maxDelayMs;
    }

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.RETRYING;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering RETRYING state, execution {}", execution.getAgentId(), execution.getId());

        if (!enabled) {
            log.warn("Retry is disabled, failing execution {}", execution.getId());
            stateMachine.transition(AgentExecutionState.FAILED, execution);
            return;
        }

        // Check if error is retryable
        String errorCategoryStr = (String) execution.getMetadata("lastErrorCategory");
        ToolErrorCategory errorCategory = parseErrorCategory(errorCategoryStr);
        if (errorCategory != null && !errorCategory.isRetryable()) {
            log.info("Error category {} is not retryable for execution {}", errorCategory, execution.getId());
            cleanup(execution.getId());
            stateMachine.transition(AgentExecutionState.FAILED, execution);
            return;
        }

        // Check max retries
        AtomicInteger counter = retryCounters.computeIfAbsent(execution.getId(), k -> new AtomicInteger(0));
        int retryCount = counter.incrementAndGet();
        if (retryCount > maxRetries) {
            log.warn("Max retries ({}) exceeded for execution {}", maxRetries, execution.getId());
            cleanup(execution.getId());
            stateMachine.transition(AgentExecutionState.FAILED, execution);
            return;
        }

        // Circuit breaker: 3 consecutive failures opens the circuit
        AtomicInteger streak = failureStreaks.computeIfAbsent(execution.getId(), k -> new AtomicInteger(0));
        if (streak.incrementAndGet() >= 3) {
            log.warn("Circuit breaker open for execution {} after {} consecutive failures",
                    execution.getId(), streak.get());
            cleanup(execution.getId());
            stateMachine.transition(AgentExecutionState.FAILED, execution);
            return;
        }

        // Exponential backoff: min(100ms * 2^retryCount, 30s)
        long delay = Math.min(baseDelayMs * (1L << (retryCount - 1)), maxDelayMs);
        log.info("Retry {}/{} for execution {}, waiting {}ms", retryCount, maxRetries, execution.getId(), delay);

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry interrupted for execution {}", execution.getId());
            stateMachine.transition(AgentExecutionState.FAILED, execution);
            return;
        }

        // Transition back to TOOL_CALLING with retry context
        execution.setMetadata("retryContext", String.valueOf(retryCount));
        stateMachine.transition(AgentExecutionState.TOOL_CALLING, execution);
    }

    /**
     * Clean up retry state when execution terminates (success or permanent failure).
     */
    public void clearRetryState(Long executionId) {
        retryCounters.remove(executionId);
        failureStreaks.remove(executionId);
    }

    private void cleanup(Long executionId) {
        retryCounters.remove(executionId);
        failureStreaks.remove(executionId);
    }

    private ToolErrorCategory parseErrorCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return null;
        try {
            return ToolErrorCategory.valueOf(categoryName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
