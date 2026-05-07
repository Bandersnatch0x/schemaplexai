package com.schemaplexai.agent.engine.tool.mcp;

import com.schemaplexai.agent.engine.evaluation.ValidationResult;
import com.schemaplexai.agent.engine.guardrails.GuardrailsEngine;
import com.schemaplexai.agent.engine.tool.ToolDefinition;
import com.schemaplexai.agent.engine.tool.ToolRegistry;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.mapper.McpServerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpToolDiscoveryService")
class McpToolDiscoveryTest {

    @Mock
    private McpServerMapper mcpServerMapper;

    @Mock
    private McpClientManager clientManager;

    @Mock
    private ToolRegistry toolRegistry;

    @Mock
    private GuardrailsEngine guardrailsEngine;

    @Mock
    private McpClient mcpClient;

    private McpToolDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        discoveryService = new McpToolDiscoveryService(
                mcpServerMapper, clientManager, toolRegistry, guardrailsEngine);
    }

    // ── syncAll() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("syncAll")
    class SyncAllTests {

        @Test
        @DisplayName("should discover and register tools from approved servers")
        void shouldDiscoverAndRegisterTools() {
            SfMcpServer server = createServer(1L, "http://server-1:8080", 1L);
            when(mcpServerMapper.selectList(any())).thenReturn(List.of(server));
            when(clientManager.create("http://server-1:8080")).thenReturn(mcpClient);
            when(mcpClient.isConnected()).thenReturn(true);
            when(mcpClient.listTools()).thenReturn(List.of(
                    new McpClient.McpTool("read_file", "Reads a file"),
                    new McpClient.McpTool("write_file", "Writes a file")
            ));
            when(guardrailsEngine.validateInput(anyString())).thenReturn(ValidationResult.valid());
            when(toolRegistry.exists(anyString())).thenReturn(false);

            discoveryService.syncAll();

            ArgumentCaptor<ToolDefinition> captor = ArgumentCaptor.forClass(ToolDefinition.class);
            verify(toolRegistry, times(2)).register(captor.capture());
            List<ToolDefinition> registered = captor.getAllValues();
            assertThat(registered).extracting(ToolDefinition::name)
                    .containsExactly("mcp:1:read_file", "mcp:1:write_file");
        }

        @Test
        @DisplayName("should skip tools that fail guardrails validation")
        void shouldSkipToolsFailingGuardrails() {
            SfMcpServer server = createServer(1L, "http://server-1:8080", 1L);
            when(mcpServerMapper.selectList(any())).thenReturn(List.of(server));
            when(clientManager.create("http://server-1:8080")).thenReturn(mcpClient);
            when(mcpClient.isConnected()).thenReturn(true);
            when(mcpClient.listTools()).thenReturn(List.of(
                    new McpClient.McpTool("safe_tool", "A safe tool"),
                    new McpClient.McpTool("bad_tool", "A bad tool")
            ));
            when(guardrailsEngine.validateInput("A safe tool")).thenReturn(ValidationResult.valid());
            when(guardrailsEngine.validateInput("A bad tool"))
                    .thenReturn(ValidationResult.invalid("unsafe"));
            when(toolRegistry.exists(anyString())).thenReturn(false);

            discoveryService.syncAll();

            ArgumentCaptor<ToolDefinition> captor = ArgumentCaptor.forClass(ToolDefinition.class);
            verify(toolRegistry, times(1)).register(captor.capture());
            assertThat(captor.getValue().name()).isEqualTo("mcp:1:safe_tool");
        }

        @Test
        @DisplayName("should skip tools not in server whitelist")
        void shouldSkipToolsNotInWhitelist() {
            SfMcpServer server = createServer(1L, "http://server-1:8080", 1L);
            server.setToolWhitelist(List.of("allowed_tool"));
            when(mcpServerMapper.selectList(any())).thenReturn(List.of(server));
            when(clientManager.create("http://server-1:8080")).thenReturn(mcpClient);
            when(mcpClient.isConnected()).thenReturn(true);
            when(mcpClient.listTools()).thenReturn(List.of(
                    new McpClient.McpTool("allowed_tool", "Allowed"),
                    new McpClient.McpTool("blocked_tool", "Blocked")
            ));
            when(guardrailsEngine.validateInput(anyString())).thenReturn(ValidationResult.valid());
            when(toolRegistry.exists(anyString())).thenReturn(false);

            discoveryService.syncAll();

            ArgumentCaptor<ToolDefinition> captor = ArgumentCaptor.forClass(ToolDefinition.class);
            verify(toolRegistry, times(1)).register(captor.capture());
            assertThat(captor.getValue().name()).isEqualTo("mcp:1:allowed_tool");
        }

        @Test
        @DisplayName("should skip disconnected clients")
        void shouldSkipDisconnectedClients() {
            SfMcpServer server = createServer(1L, "http://server-1:8080", 1L);
            when(mcpServerMapper.selectList(any())).thenReturn(List.of(server));
            when(clientManager.create("http://server-1:8080")).thenReturn(mcpClient);
            when(mcpClient.isConnected()).thenReturn(false);

            discoveryService.syncAll();

            verify(mcpClient, never()).listTools();
            verify(toolRegistry, never()).register(any());
        }

        @Test
        @DisplayName("should isolate failures per server")
        void shouldIsolateFailuresPerServer() {
            SfMcpServer server1 = createServer(1L, "http://server-1:8080", 1L);
            SfMcpServer server2 = createServer(2L, "http://server-2:8080", 1L);
            when(mcpServerMapper.selectList(any())).thenReturn(List.of(server1, server2));

            McpClient client2 = mock(McpClient.class);
            when(clientManager.create("http://server-1:8080")).thenThrow(new RuntimeException("boom"));
            when(clientManager.create("http://server-2:8080")).thenReturn(client2);
            when(client2.isConnected()).thenReturn(true);
            when(client2.listTools()).thenReturn(List.of(
                    new McpClient.McpTool("ok_tool", "OK")
            ));
            when(guardrailsEngine.validateInput(anyString())).thenReturn(ValidationResult.valid());
            when(toolRegistry.exists(anyString())).thenReturn(false);

            discoveryService.syncAll();

            verify(toolRegistry, times(1)).register(any());
        }

        @Test
        @DisplayName("should do nothing when no approved servers exist")
        void shouldDoNothingWhenNoServers() {
            when(mcpServerMapper.selectList(any())).thenReturn(List.of());

            discoveryService.syncAll();

            verify(clientManager, never()).create(anyString());
            verify(toolRegistry, never()).register(any());
        }

        @Test
        @DisplayName("should not re-register existing tools")
        void shouldNotReregisterExistingTools() {
            SfMcpServer server = createServer(1L, "http://server-1:8080", 1L);
            when(mcpServerMapper.selectList(any())).thenReturn(List.of(server));
            when(clientManager.create("http://server-1:8080")).thenReturn(mcpClient);
            when(mcpClient.isConnected()).thenReturn(true);
            when(mcpClient.listTools()).thenReturn(List.of(
                    new McpClient.McpTool("existing_tool", "Already registered")
            ));
            when(guardrailsEngine.validateInput(anyString())).thenReturn(ValidationResult.valid());
            when(toolRegistry.exists("mcp:1:existing_tool")).thenReturn(true);

            discoveryService.syncAll();

            verify(toolRegistry, never()).register(any());
        }
    }

    // ── discoverForServer() ─────────────────────────────────────────

    @Nested
    @DisplayName("discoverForServer")
    class DiscoverForServerTests {

        @Test
        @DisplayName("should return discovered tools for a single server")
        void shouldReturnDiscoveredTools() {
            SfMcpServer server = createServer(1L, "http://server-1:8080", 1L);
            when(clientManager.create("http://server-1:8080")).thenReturn(mcpClient);
            when(mcpClient.isConnected()).thenReturn(true);
            when(mcpClient.listTools()).thenReturn(List.of(
                    new McpClient.McpTool("tool_a", "Tool A"),
                    new McpClient.McpTool("tool_b", "Tool B")
            ));
            when(guardrailsEngine.validateInput(anyString())).thenReturn(ValidationResult.valid());
            when(toolRegistry.exists(anyString())).thenReturn(false);

            List<ToolDefinition> result = discoveryService.discoverForServer(server);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("mcp:1:tool_a");
            assertThat(result.get(1).name()).isEqualTo("mcp:1:tool_b");
        }

        @Test
        @DisplayName("should return empty list when client is disconnected")
        void shouldReturnEmptyWhenDisconnected() {
            SfMcpServer server = createServer(1L, "http://server-1:8080", 1L);
            when(clientManager.create("http://server-1:8080")).thenReturn(mcpClient);
            when(mcpClient.isConnected()).thenReturn(false);

            List<ToolDefinition> result = discoveryService.discoverForServer(server);

            assertThat(result).isEmpty();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private SfMcpServer createServer(Long id, String endpoint, Long tenantId) {
        SfMcpServer server = new SfMcpServer();
        server.setId(id);
        server.setEndpoint(endpoint);
        server.setTenantId(String.valueOf(tenantId));
        server.setStatus(1);
        return server;
    }
}
