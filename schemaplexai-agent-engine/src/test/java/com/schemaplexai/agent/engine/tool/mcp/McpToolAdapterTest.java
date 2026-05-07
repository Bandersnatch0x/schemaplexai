package com.schemaplexai.agent.engine.tool.mcp;

import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.ToolResult;
import com.schemaplexai.agent.engine.tool.adapter.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpToolAdapterTest {

    @Mock
    private McpServerRegistry serverRegistry;

    @Mock
    private McpClientManager clientManager;

    @Mock
    private McpClient mcpClient;

    private McpToolAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new McpToolAdapter(serverRegistry, clientManager);
    }

    @Test
    void shouldReturnMcpAsToolName() {
        assertThat(adapter.getToolName()).isEqualTo("mcp");
    }

    @Test
    void shouldSupportMcpPrefixedTools() {
        assertThat(adapter.supports("mcp:github:read_file")).isTrue();
        assertThat(adapter.supports("github:read_file")).isFalse();
        assertThat(adapter.supports(null)).isFalse();
    }

    @Test
    void shouldReturnErrorForInvalidRef() throws Exception {
        ToolCall call = new ToolCall("invalid", Map.of());
        ExecutionContext ctx = new ExecutionContext("1", 1L, "/tmp");

        ToolResult result = adapter.execute(call, ctx);

        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).contains("Invalid MCP tool reference");
    }

    @Test
    void shouldReturnErrorWhenServerNotAllowed() throws Exception {
        when(serverRegistry.isAllowed("github", 1L)).thenReturn(false);

        ToolCall call = new ToolCall("mcp:github:read_file", Map.of());
        ExecutionContext ctx = new ExecutionContext("1", 1L, "/tmp");

        ToolResult result = adapter.execute(call, ctx);

        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).contains("not allowed");
    }

    @Test
    void shouldReturnErrorWhenToolNotAllowed() throws Exception {
        when(serverRegistry.isAllowed("github", 1L)).thenReturn(true);
        when(serverRegistry.isToolAllowed("github", "read_file", 1L)).thenReturn(false);

        ToolCall call = new ToolCall("mcp:github:read_file", Map.of());
        ExecutionContext ctx = new ExecutionContext("1", 1L, "/tmp");

        ToolResult result = adapter.execute(call, ctx);

        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).contains("not allowed");
    }

    @Test
    void shouldReturnErrorWhenClientDisconnected() throws Exception {
        when(serverRegistry.isAllowed("github", 1L)).thenReturn(true);
        when(serverRegistry.isToolAllowed("github", "read_file", 1L)).thenReturn(true);
        when(clientManager.create("github")).thenReturn(mcpClient);
        when(mcpClient.isConnected()).thenReturn(false);

        ToolCall call = new ToolCall("mcp:github:read_file", Map.of());
        ExecutionContext ctx = new ExecutionContext("1", 1L, "/tmp");

        ToolResult result = adapter.execute(call, ctx);

        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).contains("disconnected");
    }

    @Test
    void shouldReturnSuccessWhenAllChecksPass() throws Exception {
        when(serverRegistry.isAllowed("github", 1L)).thenReturn(true);
        when(serverRegistry.isToolAllowed("github", "read_file", 1L)).thenReturn(true);
        when(clientManager.create("github")).thenReturn(mcpClient);
        when(mcpClient.isConnected()).thenReturn(true);

        ToolCall call = new ToolCall("mcp:github:read_file", Map.of());
        ExecutionContext ctx = new ExecutionContext("1", 1L, "/tmp");

        ToolResult result = adapter.execute(call, ctx);

        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("validated");
    }
}
