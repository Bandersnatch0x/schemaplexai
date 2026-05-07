package com.schemaplexai.agent.engine.skill;

/**
 * Exception thrown when skill markdown validation fails.
 * Covers: missing fields, length violations, HTML injection attempts.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
