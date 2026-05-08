package com.schemaplexai.agent.engine.goal;

/**
 * Immutable signal indicating whether goal completion has been detected.
 *
 * @param complete whether the execution should stop
 * @param reason   human-readable reason for the signal
 * @param type     classification of the completion
 */
public record CompletionSignal(
        boolean complete,
        String reason,
        CompletionType type
) {

    public static CompletionSignal notComplete() {
        return new CompletionSignal(false, null, null);
    }

    public static CompletionSignal fullyAchieved(String reason) {
        return new CompletionSignal(true, reason, CompletionType.FULLY_ACHIEVED);
    }

    public static CompletionSignal partiallyAchieved(String reason) {
        return new CompletionSignal(true, reason, CompletionType.PARTIALLY_ACHIEVED);
    }

    public static CompletionSignal maxIterations(String reason) {
        return new CompletionSignal(true, reason, CompletionType.MAX_ITERATIONS);
    }

    public static CompletionSignal timeout(String reason) {
        return new CompletionSignal(true, reason, CompletionType.TIMEOUT);
    }
}
