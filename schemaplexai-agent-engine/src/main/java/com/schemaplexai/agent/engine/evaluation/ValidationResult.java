package com.schemaplexai.agent.engine.evaluation;

/**
 * Generic validation result used by guardrails and evaluators.
 */
public record ValidationResult(boolean success, String errorMessage) {

    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult invalid(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }
}
