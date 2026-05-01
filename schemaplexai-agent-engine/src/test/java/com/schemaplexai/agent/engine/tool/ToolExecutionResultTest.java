package com.schemaplexai.agent.engine.tool;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolExecutionResultTest {

    @Test
    void shouldCreateSuccessResult() {
        ToolExecutionResult result = ToolExecutionResult.success("fileRead", "content here", 150, 42);

        assertTrue(result.success());
        assertEquals("fileRead", result.toolName());
        assertEquals("content here", result.output());
        assertNull(result.errorCategory());
        assertNull(result.errorMessage());
        assertEquals(150, result.latencyMs());
        assertEquals(42, result.tokenCount());
    }

    @Test
    void shouldCreateFailureResult() {
        ToolExecutionResult result = ToolExecutionResult.failure(
            "apiCall", ToolErrorCategory.PROVIDER_ERROR, "Rate limit exceeded", 2000, 0);

        assertFalse(result.success());
        assertEquals("apiCall", result.toolName());
        assertEquals(ToolErrorCategory.PROVIDER_ERROR, result.errorCategory());
        assertEquals("Rate limit exceeded", result.errorMessage());
        assertEquals(2000, result.latencyMs());
    }

    @Test
    void shouldCreateBlockedResult() {
        ToolExecutionResult result = ToolExecutionResult.blocked(
            "volumeDelete", ToolErrorCategory.UNAUTHORIZED_SCOPE, "Irreversible operation blocked by safety guard");

        assertFalse(result.success());
        assertTrue(result.blocked());
        assertEquals(ToolErrorCategory.UNAUTHORIZED_SCOPE, result.errorCategory());
    }
}
