package com.schemaplexai.agent.engine.admission;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

@Data
public class TokenBudget {

    private final long maxInputTokens;
    private final long maxOutputTokens;
    private final AtomicLong consumedInputTokens = new AtomicLong(0);
    private final AtomicLong consumedOutputTokens = new AtomicLong(0);

    public TokenBudget(long maxInputTokens, long maxOutputTokens) {
        this.maxInputTokens = maxInputTokens;
        this.maxOutputTokens = maxOutputTokens;
    }

    public boolean consumeInput(long tokens) {
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

    public boolean consumeOutput(long tokens) {
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

    public boolean isInputExceeded() {
        return consumedInputTokens.get() > maxInputTokens;
    }

    public boolean isOutputExceeded() {
        return consumedOutputTokens.get() > maxOutputTokens;
    }

    public boolean isExceeded() {
        return isInputExceeded() || isOutputExceeded();
    }

    public long remainingInput() {
        long remaining = maxInputTokens - consumedInputTokens.get();
        return Math.max(remaining, 0);
    }

    public long remainingOutput() {
        long remaining = maxOutputTokens - consumedOutputTokens.get();
        return Math.max(remaining, 0);
    }
}
