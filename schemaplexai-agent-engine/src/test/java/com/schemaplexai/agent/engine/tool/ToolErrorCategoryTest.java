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
        assertNotNull(ToolErrorCategory.UNAUTHORIZED_SCOPE);
    }

    @Test
    void unauthorizedScopeShouldBeSecurityRelated() {
        assertTrue(ToolErrorCategory.UNAUTHORIZED_SCOPE.isSecurityRelated(),
            "UNAUTHORIZED_SCOPE must be flagged as security-related");
    }

    @Test
    void invalidArgumentsShouldNotBeSecurityRelated() {
        assertFalse(ToolErrorCategory.INVALID_ARGUMENTS.isSecurityRelated(),
            "INVALID_ARGUMENTS is not a security issue");
    }
}
