package com.schemaplexai.agent.engine.reasoning;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.context.AgentContext;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks accumulated token usage and remaining budget for an agent execution.
 * Used by reasoning strategies to respect input/output token limits.
 */
public class TokenBudget {

    private final long maxInputTokens;
    private final long maxOutputTokens;
    private final AtomicLong consumedInputTokens = new AtomicLong(0);
    private final AtomicLong consumedOutputTokens = new AtomicLong(0);

    /**
     * Creates a new token budget with the given limits.
     *
     * @param maxInputTokens  maximum input tokens allowed
     * @param maxOutputTokens maximum output tokens allowed
     */
    public TokenBudget(long maxInputTokens, long maxOutputTokens) {
        if (maxInputTokens < 0 || maxOutputTokens < 0) {
            throw new IllegalArgumentException("Token limits must be non-negative");
        }
        this.maxInputTokens = maxInputTokens;
        this.maxOutputTokens = maxOutputTokens;
    }

    /**
     * Records consumption of input tokens. Returns false if the budget is exceeded.
     */
    public boolean consumeInput(long tokens) {
        if (tokens < 0) {
            throw new IllegalArgumentException("Cannot consume negative tokens");
        }
        while (true) {
            long current = consumedInputTokens.get();
            long next = current + tokens;
            if (next > maxInputTokens) {
                return false;
            }
            if (consumedInputTokens.compareAndSet(current, next)) {
                return true;
            }
        }
    }

    /**
     * Records consumption of output tokens. Returns false if the budget is exceeded.
     */
    public boolean consumeOutput(long tokens) {
        if (tokens < 0) {
            throw new IllegalArgumentException("Cannot consume negative tokens");
        }
        while (true) {
            long current = consumedOutputTokens.get();
            long next = current + tokens;
            if (next > maxOutputTokens) {
                return false;
            }
            if (consumedOutputTokens.compareAndSet(current, next)) {
                return true;
            }
        }
    }

    /**
     * Returns true if both input and output budgets still have capacity.
     */
    public boolean hasRemaining() {
        return getRemainingInput() > 0 || getRemainingOutput() > 0;
    }

    /**
     * Returns remaining input token budget (never negative).
     */
    public long getRemainingInput() {
        long remaining = maxInputTokens - consumedInputTokens.get();
        return Math.max(remaining, 0);
    }

    /**
     * Returns remaining output token budget (never negative).
     */
    public long getRemainingOutput() {
        long remaining = maxOutputTokens - consumedOutputTokens.get();
        return Math.max(remaining, 0);
    }

    public long getMaxInputTokens() {
        return maxInputTokens;
    }

    public long getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public AtomicLong getConsumedInputTokens() {
        return consumedInputTokens;
    }

    public AtomicLong getConsumedOutputTokens() {
        return consumedOutputTokens;
    }
}
