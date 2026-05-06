package com.schemaplexai.agent.engine.tool;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolErrorCategoryTest {

    @Test
    void shouldContainAllExpectedCategories() {
        assertNotNull(ToolErrorCategory.INVALID_ARGUMENT);
        assertNotNull(ToolErrorCategory.UNEXPECTED_ENVIRONMENT);
        assertNotNull(ToolErrorCategory.INTERNAL_ERROR);
        assertNotNull(ToolErrorCategory.RATE_LIMITED);
        assertNotNull(ToolErrorCategory.TIMEOUT);
        assertNotNull(ToolErrorCategory.IRREVERSIBLE_OPERATION);
        assertNotNull(ToolErrorCategory.ENVIRONMENT_MISMATCH);
        assertNotNull(ToolErrorCategory.PERMISSION_DENIED);
        assertNotNull(ToolErrorCategory.RESOURCE_EXHAUSTED);
    }

    @Test
    void irreversibleOperationShouldBeSecurityRelated() {
        assertTrue(ToolErrorCategory.IRREVERSIBLE_OPERATION.isSecurityRelated(),
            "IRREVERSIBLE_OPERATION must be flagged as security-related");
    }

    @Test
    void environmentMismatchShouldBeSecurityRelated() {
        assertTrue(ToolErrorCategory.ENVIRONMENT_MISMATCH.isSecurityRelated(),
            "ENVIRONMENT_MISMATCH must be flagged as security-related");
    }

    @Test
    void invalidArgumentShouldNotBeSecurityRelated() {
        assertFalse(ToolErrorCategory.INVALID_ARGUMENT.isSecurityRelated(),
            "INVALID_ARGUMENT is not a security issue");
    }

    @Test
    void timeoutShouldBeRetryable() {
        assertTrue(ToolErrorCategory.TIMEOUT.isRetryable(),
            "TIMEOUT should be retryable");
    }

    @Test
    void internalErrorShouldBeRetryable() {
        assertTrue(ToolErrorCategory.INTERNAL_ERROR.isRetryable(),
            "INTERNAL_ERROR should be retryable");
    }

    @Test
    void invalidArgumentShouldNotBeRetryable() {
        assertFalse(ToolErrorCategory.INVALID_ARGUMENT.isRetryable(),
            "INVALID_ARGUMENT should not be retryable");
    }

    @Test
    void irreversibleOperationShouldNotBeRetryable() {
        assertFalse(ToolErrorCategory.IRREVERSIBLE_OPERATION.isRetryable(),
            "IRREVERSIBLE_OPERATION should not be retryable");
    }

    @Test
    void permissionDeniedShouldBeSecurityRelated() {
        assertTrue(ToolErrorCategory.PERMISSION_DENIED.isSecurityRelated(),
            "PERMISSION_DENIED must be flagged as security-related");
    }

    @Test
    void rateLimitedShouldBeRetryable() {
        assertTrue(ToolErrorCategory.RATE_LIMITED.isRetryable(),
            "RATE_LIMITED should be retryable");
    }
}
