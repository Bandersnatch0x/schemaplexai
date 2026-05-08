package com.schemaplexai.agent.engine.chain;

import java.time.Duration;
import java.util.Map;

/**
 * Immutable result of executing a prompt chain.
 *
 * @param chainId        the chain definition id
 * @param success        whether the chain completed all steps successfully
 * @param allOutputs     outputs from every step that executed (including partial on failure)
 * @param failedStepId   the id of the step that caused failure, or {@code null} on success
 * @param errorMessage   error details, or {@code null} on success
 * @param totalDuration  wall-clock duration of the entire chain execution
 */
public record ChainExecutionResult(
        String chainId,
        boolean success,
        Map<String, String> allOutputs,
        String failedStepId,
        String errorMessage,
        Duration totalDuration
) {
    /**
     * Creates a successful result.
     */
    public static ChainExecutionResult success(String chainId, Map<String, String> allOutputs, Duration totalDuration) {
        return new ChainExecutionResult(chainId, true, allOutputs, null, null, totalDuration);
    }

    /**
     * Creates a failure result.
     */
    public static ChainExecutionResult failure(String chainId, Map<String, String> allOutputs,
                                               String failedStepId, String errorMessage, Duration totalDuration) {
        return new ChainExecutionResult(chainId, false, allOutputs, failedStepId, errorMessage, totalDuration);
    }
}
