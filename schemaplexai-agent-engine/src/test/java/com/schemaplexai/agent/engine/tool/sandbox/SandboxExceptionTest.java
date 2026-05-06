package com.schemaplexai.agent.engine.tool.sandbox;

import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SandboxExceptionTest {

    @Test
    void shouldCreateWithMessageAndCategory() {
        SandboxException ex = new SandboxException("error occurred", ToolErrorCategory.SANDBOX_ERROR);

        assertEquals("error occurred", ex.getMessage());
        assertEquals(ToolErrorCategory.SANDBOX_ERROR, ex.getCategory());
    }

    @Test
    void shouldCreateWithMessageCauseAndCategory() {
        Throwable cause = new RuntimeException("root cause");
        SandboxException ex = new SandboxException("error occurred", cause, ToolErrorCategory.PATH_VIOLATION);

        assertEquals("error occurred", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertEquals(ToolErrorCategory.PATH_VIOLATION, ex.getCategory());
    }

    @Test
    void shouldRejectNullCategory() {
        assertThrows(NullPointerException.class, () ->
                new SandboxException("error", null));
    }

    @Test
    void shouldRejectNullCategoryWithCause() {
        assertThrows(NullPointerException.class, () ->
                new SandboxException("error", new RuntimeException(), null));
    }
}
