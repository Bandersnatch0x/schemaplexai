package com.schemaplexai.agent.engine.security;

import com.schemaplexai.agent.engine.tool.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SseTokenValidator interface contract.
 * Uses an in-memory stub implementation since the real validator depends on JWT/auth infrastructure.
 */
class SseTokenValidatorTest {

    /**
     * Simple stub implementation for testing interface contract.
     */
    private static class StubSseTokenValidator implements SseTokenValidator {

        private static final String VALID_TOKEN = "valid-sse-token-123";
        private static final String VALID_EXECUTION_ID = "exec-456";

        @Override
        public ValidationResult validate(String token, String executionId) {
            if (token == null || token.isBlank()) {
                return ValidationResult.invalid("Token is required");
            }
            if (executionId == null || executionId.isBlank()) {
                return ValidationResult.invalid("Execution ID is required");
            }
            if (!VALID_TOKEN.equals(token)) {
                return ValidationResult.invalid("Invalid or expired token");
            }
            if (!VALID_EXECUTION_ID.equals(executionId)) {
                return ValidationResult.invalid("Token not authorized for this execution");
            }
            return ValidationResult.valid();
        }
    }

    private final SseTokenValidator validator = new StubSseTokenValidator();

    @Test
    void shouldPassValidTokenAndExecution() {
        ValidationResult result = validator.validate("valid-sse-token-123", "exec-456");
        assertTrue(result.isValid());
    }

    @Test
    void shouldRejectNullToken() {
        ValidationResult result = validator.validate(null, "exec-456");
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("Token is required"));
    }

    @Test
    void shouldRejectBlankToken() {
        ValidationResult result = validator.validate("", "exec-456");
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("Token is required"));
    }

    @Test
    void shouldRejectNullExecutionId() {
        ValidationResult result = validator.validate("valid-sse-token-123", null);
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("Execution ID is required"));
    }

    @Test
    void shouldRejectBlankExecutionId() {
        ValidationResult result = validator.validate("valid-sse-token-123", "");
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("Execution ID is required"));
    }

    @Test
    void shouldRejectInvalidToken() {
        ValidationResult result = validator.validate("wrong-token", "exec-456");
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("Invalid or expired token"));
    }

    @Test
    void shouldRejectMismatchedExecutionId() {
        ValidationResult result = validator.validate("valid-sse-token-123", "other-exec");
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("not authorized for this execution"));
    }
}
