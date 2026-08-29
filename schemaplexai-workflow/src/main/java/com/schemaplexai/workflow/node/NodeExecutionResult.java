package com.schemaplexai.workflow.node;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionResult {

    private boolean success;
    private boolean timeout;
    private boolean retryable;
    private String message;
    private Map<String, Object> output;

    public NodeExecutionResult(boolean success, String message, Map<String, Object> output) {
        this(success, false, false, message, output);
    }

    public static NodeExecutionResult success(Map<String, Object> output) {
        return new NodeExecutionResult(true, false, false, null, output);
    }

    public static NodeExecutionResult success() {
        return new NodeExecutionResult(true, false, false, null, Map.of());
    }

    /**
     * A deterministic failure (bad configuration / validation). Not retried, because
     * re-running it cannot succeed.
     */
    public static NodeExecutionResult failure(String message) {
        return new NodeExecutionResult(false, false, false, message, Map.of());
    }

    /**
     * A transient failure (network / upstream / IO) that may succeed on retry. The
     * engine re-runs such failures up to the retry budget with exponential backoff
     * (spec §8: 3 retries).
     */
    public static NodeExecutionResult retryableFailure(String message) {
        return new NodeExecutionResult(false, false, true, message, Map.of());
    }

    /**
     * Terminal TIMEOUT state (spec §3.2): the bounded call exceeded its configured
     * timeout. Distinct from a plain failure so the engine records TIMEOUT and skips
     * retries — re-running a timed-out long call would only compound the overrun.
     */
    public static NodeExecutionResult timeout(String message) {
        return new NodeExecutionResult(false, true, false, message, Map.of());
    }
}
