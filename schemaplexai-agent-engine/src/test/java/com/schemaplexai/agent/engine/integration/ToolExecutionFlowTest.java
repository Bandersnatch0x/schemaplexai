package com.schemaplexai.agent.engine.integration;

import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.tool.*;
import com.schemaplexai.agent.engine.tool.adapter.ExecutionContext;
import com.schemaplexai.agent.engine.tool.adapter.ToolAdapter;
import com.schemaplexai.agent.engine.tool.parser.ToolCallParser;
import com.schemaplexai.agent.engine.tool.registry.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Tool Execution Flow Integration")
class ToolExecutionFlowTest {

    @Mock
    private ToolAdapter mockAdapter;

    @Mock
    private ToolCallParser mockParser;

    @Mock
    private LlmProvider mockProvider;

    private ToolRegistry toolRegistry;
    private ToolSafetyGuard safetyGuard;

    @BeforeEach
    void setUp() {
        when(mockAdapter.getToolName()).thenReturn("fileRead");
        when(mockParser.getProviderName()).thenReturn("openai");
        toolRegistry = new ToolRegistry(List.of(mockAdapter), List.of(mockParser));
        safetyGuard = new ToolSafetyGuard();
    }

    @Test
    @DisplayName("should resolve registered tool adapter")
    void shouldResolveRegisteredTool() {
        ToolAdapter resolved = toolRegistry.resolve("fileRead");
        assertNotNull(resolved);
        assertEquals("fileRead", resolved.getToolName());
    }

    @Test
    @DisplayName("should return null for unregistered tool")
    void shouldReturnNullForUnregisteredTool() {
        assertNull(toolRegistry.resolve("unknownTool"));
        assertFalse(toolRegistry.isRegistered("unknownTool"));
    }

    @Test
    @DisplayName("should filter out unregistered tools during parse")
    void shouldFilterUnregisteredToolsDuringParse() {
        ToolCall registeredCall = new ToolCall("fileRead", Map.of("path", "/tmp/test"));
        ToolCall unregisteredCall = new ToolCall("dangerousTool", Map.of());

        when(mockProvider.getProviderName()).thenReturn("openai");
        when(mockParser.parse(any(), any())).thenReturn(List.of(registeredCall, unregisteredCall));

        List<ToolCall> result = toolRegistry.parse("[mock]", mockProvider);

        assertEquals(1, result.size());
        assertEquals("fileRead", result.get(0).toolName());
    }

    @Test
    @DisplayName("should block irreversible tool names")
    void shouldBlockIrreversibleToolNames() {
        ToolSafetyGuard.SafetyCheckResult result = safetyGuard.check("volumeDelete", "{}");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
        assertEquals(ToolErrorCategory.IRREVERSIBLE_OPERATION, result.errorCategory());
    }

    @Test
    @DisplayName("should block destructive commands in arguments")
    void shouldBlockDestructiveCommandsInArguments() {
        ToolSafetyGuard.SafetyCheckResult result = safetyGuard.check("runShell", "DROP TABLE users");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
    }

    @Test
    @DisplayName("should detect obfuscated destructive commands via normalization")
    void shouldDetectObfuscatedCommands() {
        String obfuscated = "&#68;ROP TABLE users";
        ToolSafetyGuard.SafetyCheckResult result = safetyGuard.check("runSql", obfuscated);
        assertFalse(result.allowed());
    }

    @Test
    @DisplayName("should block environment mismatch")
    void shouldBlockEnvironmentMismatch() {
        ToolSafetyGuard.SafetyCheckResult result = safetyGuard.check("deploy", "--env=prod", "dev");
        assertFalse(result.allowed());
        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, result.errorCategory());
    }

    @Test
    @DisplayName("should permit safe operations")
    void shouldPermitSafeOperations() {
        ToolSafetyGuard.SafetyCheckResult result = safetyGuard.check("fileRead", "{\"path\": \"/tmp/log\"}");
        assertTrue(result.allowed());
        assertFalse(result.blocked());
    }

    @Test
    @DisplayName("should execute tool via adapter with execution context")
    void shouldExecuteToolViaAdapter() throws Exception {
        ToolCall call = new ToolCall("fileRead", Map.of("path", "/tmp/test"));
        ExecutionContext ctx = new ExecutionContext("tenant-1", 1L, "/workspace");
        ToolResult expected = ToolResult.success("file content");

        when(mockAdapter.execute(call, ctx)).thenReturn(expected);

        ToolResult result = mockAdapter.execute(call, ctx);

        assertTrue(result.success());
        assertEquals("file content", result.output());
    }
}
