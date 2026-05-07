package com.schemaplexai.agent.engine.tool.adapter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionContextTest {

    @Test
    void shouldCreateWithDefaults() {
        ExecutionContext ctx = new ExecutionContext("tenant1", 123L, "/workspace");

        assertEquals("tenant1", ctx.tenantId());
        assertEquals(123L, ctx.executionId());
        assertEquals("/workspace", ctx.workspaceRoot());
        assertNotNull(ctx.attributes());
        assertTrue(ctx.attributes().isEmpty());
        assertNotNull(ctx.guardrails());
        assertTrue(ctx.guardrails().isEmpty());
    }

    @Test
    void shouldCreateWithAttributes() {
        Map<String, Object> attrs = Map.of("key", "value");
        ExecutionContext ctx = new ExecutionContext("tenant1", 123L, "/workspace", attrs);

        assertEquals("value", ctx.getAttribute("key"));
        assertNotNull(ctx.guardrails());
        assertTrue(ctx.guardrails().isEmpty());
    }

    @Test
    void shouldCreateWithGuardrails() {
        Map<String, Object> attrs = Map.of("key", "value");
        Map<String, Object> guardrails = Map.of("maxDepth", 3);
        ExecutionContext ctx = new ExecutionContext("tenant1", 123L, "/workspace", attrs, guardrails);

        assertEquals("value", ctx.getAttribute("key"));
        assertEquals(Integer.valueOf(3), ctx.getGuardrail("maxDepth"));
    }

    @Test
    void shouldReturnNullForMissingAttribute() {
        ExecutionContext ctx = new ExecutionContext("tenant1", 123L, "/workspace");

        assertNull(ctx.getAttribute("missing"));
    }

    @Test
    void shouldSupportTypedAttributeAccess() {
        Map<String, Object> attrs = Map.of("number", 42);
        ExecutionContext ctx = new ExecutionContext("tenant1", 123L, "/workspace", attrs);

        Integer value = ctx.getAttribute("number");
        assertEquals(42, value);
    }
}
