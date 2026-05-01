package com.schemaplexai.agent.engine.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ToolSandboxTest {

    private ContainerToolSandbox sandbox;

    @BeforeEach
    void setUp() {
        ToolWhitelist whitelist = new ToolWhitelist(Set.of("calculator", "weather"));
        sandbox = new ContainerToolSandbox(whitelist);
    }

    // --- execute() tests ---

    @Test
    void shouldRejectToolsNotInWhitelist() {
        ToolCall toolCall = new ToolCall("malicious", Map.of());

        ToolExecutionException ex = assertThrows(ToolExecutionException.class, () ->
                sandbox.execute(toolCall, SandboxConfig.defaultConfig()));

        assertEquals(ToolErrorCategory.PERMISSION_DENIED, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("not in the allowed list"));
    }

    @Test
    void shouldAllowWhitelistedTool() throws ToolExecutionException {
        ToolCall toolCall = new ToolCall("calculator", Map.of("expression", "2+2"));
        ToolResult result = sandbox.execute(toolCall, SandboxConfig.defaultConfig());

        assertTrue(result.success());
        assertNotNull(result.output());
    }

    @Test
    void shouldRejectInvalidToolDuringExecution() {
        ToolCall toolCall = new ToolCall("", Map.of());

        ToolExecutionException ex = assertThrows(ToolExecutionException.class, () ->
                sandbox.execute(toolCall, SandboxConfig.defaultConfig()));

        assertEquals(ToolErrorCategory.INVALID_ARGUMENT, ex.getErrorCategory());
    }

    // --- validate() tests ---

    @Test
    void shouldRejectEmptyToolName() {
        ToolCall toolCall = new ToolCall("", Map.of());
        ValidationResult result = sandbox.validate(toolCall);

        assertFalse(result.isValid());
    }

    @Test
    void shouldRejectNullToolName() {
        ToolCall toolCall = new ToolCall(null, Map.of());
        ValidationResult result = sandbox.validate(toolCall);

        assertFalse(result.isValid());
    }

    @Test
    void shouldAcceptValidToolCall() {
        ToolCall toolCall = new ToolCall("calculator", Map.of("expr", "1+1"));
        ValidationResult result = sandbox.validate(toolCall);

        assertTrue(result.isValid());
    }

    @Test
    void shouldRejectExcessiveParameters() {
        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < 25; i++) {
            params.put("param" + i, "value" + i);
        }

        ToolCall toolCall = new ToolCall("calculator", params);
        ValidationResult result = sandbox.validate(toolCall);

        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("Too many parameters"));
    }

    @Test
    void shouldRejectOversizedParameterValue() {
        String longValue = "x".repeat(10001);
        ToolCall toolCall = new ToolCall("calculator", Map.of("big", longValue));
        ValidationResult result = sandbox.validate(toolCall);

        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("exceeds max length"));
    }

    @Test
    void shouldAcceptToolWithNoParameters() {
        ToolCall toolCall = new ToolCall("calculator");
        ValidationResult result = sandbox.validate(toolCall);

        assertTrue(result.isValid());
    }

    @Test
    void shouldAcceptToolWithNullParametersMap() {
        ToolCall toolCall = new ToolCall("calculator", null);
        ValidationResult result = sandbox.validate(toolCall);

        assertTrue(result.isValid());
    }
}
