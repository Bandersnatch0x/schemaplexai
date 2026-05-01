package com.schemaplexai.agent.engine.context;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Validation result used across input validation, RAG permission checks, etc.
 */
@Data
@AllArgsConstructor
public class ValidationResult {

    private final boolean valid;
    private final String errorMessage;

    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult invalid(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }
}
