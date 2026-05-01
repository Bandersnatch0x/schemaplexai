package com.schemaplexai.agent.engine.tool;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolErrorCategoryTest {

    @Test
    void shouldContainAllExpectedCategories() {
        assertNotNull(ToolErrorCategory.INVALID_ARGUMENTS);
        assertNotNull(ToolErrorCategory.UNEXPECTED_ENVIRONMENT);
        assertNotNull(ToolErrorCategory.PROVIDER_ERROR);
        assertNotNull(ToolErrorCategory.USER_ABORTED);
        assertNotNull(ToolErrorCategory.TIMEOUT);
        assertNotNull(ToolErrorCategory.IRREVERSIBLE_OPERATION);
        assertNotNull(ToolErrorCategory.ENVIRONMENT_MISMATCH);
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
    void invalidArgumentsShouldNotBeSecurityRelated() {
        assertFalse(ToolErrorCategory.INVALID_ARGUMENTS.isSecurityRelated(),
            "INVALID_ARGUMENTS is not a security issue");
    }

    @Test
    void timeoutShouldBeRetryable() {
        assertTrue(ToolErrorCategory.TIMEOUT.isRetryable(),
            "TIMEOUT should be retryable");
    }

    @Test
    void providerErrorShouldBeRetryable() {
        assertTrue(ToolErrorCategory.PROVIDER_ERROR.isRetryable(),
            "PROVIDER_ERROR should be retryable");
    }

    @Test
    void invalidArgumentsShouldNotBeRetryable() {
        assertFalse(ToolErrorCategory.INVALID_ARGUMENTS.isRetryable(),
            "INVALID_ARGUMENTS should not be retryable");
    }

    @Test
    void irreversibleOperationShouldNotBeRetryable() {
        assertFalse(ToolErrorCategory.IRREVERSIBLE_OPERATION.isRetryable(),
            "IRREVERSIBLE_OPERATION should not be retryable");
    }
}
