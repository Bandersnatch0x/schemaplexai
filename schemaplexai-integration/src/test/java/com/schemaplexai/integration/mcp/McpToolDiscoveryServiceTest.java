package com.schemaplexai.integration.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.integration.dto.McpToolSchema;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.mapper.McpServerMapper;
import com.schemaplexai.integration.service.McpServerService;
import com.schemaplexai.integration.service.ToolExecutionService;
import com.schemaplexai.integration.tool.DiscoveredMcpToolExecutor;
import com.schemaplexai.integration.tool.ToolExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the integration-module {@link McpToolDiscoveryService}
 * (issue 930): discovered tools must land in the SAME registry the execution
 * chain resolves from, with whitelist filtering, skip-if-exists and
 * per-server failure isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("McpToolDiscoveryService — discovery to execution-chain registry")
class McpToolDiscoveryServiceTest {

    @Mock
    private McpServerMapper mcpServerMapper;

    @Mock
    private McpClientManager mcpClientManager;

    @Mock
    private McpClient mcpClient;

    @Mock
    private McpServerService mcpServerService;

    private ToolExecutionService toolExecutionService;

    private McpToolDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        toolExecutionService = new ToolExecutionService(List.of(), new ObjectMapper());
        toolExecutionService.init();
        discoveryService = new McpToolDiscoveryService(
                mcpServerMapper, mcpClientManager, mcpServerService, toolExecutionService);
    }

    private static SfMcpServer server(Long id, String endpoint) {
        SfMcpServer server = new SfMcpServer();
        server.setId(id);
        server.setName("server-" + id);
        server.setEndpoint(endpoint);
        server.setStatus("ACTIVE");
        return server;
    }

    private static McpToolSchema tool(String name) {
        return new McpToolSchema(name, "tool " + name, Map.of("type", "object"));
    }

    @Test
    @DisplayName("syncAll registers discovered tools into the execution-chain registry under mcp:<serverId>:<tool>")
    void syncAllHappyPathRegistersIntoExecutionChainRegistry() {
        SfMcpServer server = server(1L, "http://mcp-1:9000");
        when(mcpServerMapper.selectList(any())).thenReturn(List.of(server));
        when(mcpClientManager.forServer(server)).thenReturn(mcpClient);
        when(mcpClient.listTools()).thenReturn(List.of(tool("read_file"), tool("write_file")));

        discoveryService.syncAll();

        assertThat(toolExecutionService.exists("mcp:1:read_file")).isTrue();
        assertThat(toolExecutionService.exists("mcp:1:write_file")).isTrue();
        assertThat(toolExecutionService.getRegisteredToolNames())
                .containsExactlyInAnyOrder("mcp:1:read_file", "mcp:1:write_file");

        // The registered executor must route execution back through the service,
        // which resolves the endpoint from the server row (never the id).
        when(mcpServerService.invokeTool(eq(1L), eq("read_file"), anyMap()))
                .thenReturn("{\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}");
        String result = toolExecutionService.executeTool("mcp:1:read_file", "{\"path\":\"a.txt\"}");
        assertThat(result).contains("ok");
    }

    @Test
    @DisplayName("registered executor surfaces degraded 'Error:' answers as structured failures")
    void registeredExecutorTurnsErrorStringIntoFailure() {
        SfMcpServer server = server(1L, "http://mcp-1:9000");
        when(mcpServerMapper.selectList(any())).thenReturn(List.of(server));
        when(mcpClientManager.forServer(server)).thenReturn(mcpClient);
        when(mcpClient.listTools()).thenReturn(List.of(tool("flaky")));
        discoveryService.syncAll();

        when(mcpServerService.invokeTool(eq(1L), eq("flaky"), anyMap()))
                .thenReturn("Error: MCP endpoint http://mcp-1:9000 is unreachable");

        assertThatThrownBy(() -> toolExecutionService.executeTool("mcp:1:flaky", "{}"))
                .isInstanceOf(com.schemaplexai.common.exception.BaseException.class)
                .hasMessageContaining("unreachable");
    }

    @Test
    @DisplayName("server tool whitelist filters discovered tools (null/empty allows all)")
    void whitelistFiltersTools() {
        SfMcpServer server = server(2L, "http://mcp-2:9000");
        server.setToolWhitelist(List.of("allowed_tool"));
        when(mcpServerMapper.selectList(any())).thenReturn(List.of(server));
        when(mcpClientManager.forServer(server)).thenReturn(mcpClient);
        when(mcpClient.listTools()).thenReturn(
                List.of(tool("allowed_tool"), tool("blocked_tool")));

        discoveryService.syncAll();

        assertThat(toolExecutionService.exists("mcp:2:allowed_tool")).isTrue();
        assertThat(toolExecutionService.exists("mcp:2:blocked_tool")).isFalse();
    }

    @Test
    @DisplayName("already-registered tools are skipped on re-discovery (original executor kept)")
    void skipAlreadyRegisteredTools() {
        SfMcpServer server = server(3L, "http://mcp-3:9000");
        when(mcpServerMapper.selectList(any())).thenReturn(List.of(server));
        when(mcpClientManager.forServer(server)).thenReturn(mcpClient);
        when(mcpClient.listTools()).thenReturn(List.of(tool("echo")));

        discoveryService.syncAll();
        ToolExecutor first = toolExecutionService.getExecutor("mcp:3:echo");
        assertThat(first).isNotNull();

        discoveryService.syncAll();

        verify(mcpClient, times(2)).listTools();
        assertThat(toolExecutionService.getExecutor("mcp:3:echo")).isSameAs(first);
        assertThat(toolExecutionService.getRegisteredToolNames()).containsExactly("mcp:3:echo");
    }

    @Test
    @DisplayName("one unreachable server does not block discovery of the others (per-server isolation)")
    void perServerFailureIsolation() {
        SfMcpServer dead = server(4L, "http://dead:9000");
        SfMcpServer alive = server(5L, "http://alive:9000");
        when(mcpServerMapper.selectList(any())).thenReturn(List.of(dead, alive));

        McpClient deadClient = org.mockito.Mockito.mock(McpClient.class);
        McpClient aliveClient = org.mockito.Mockito.mock(McpClient.class);
        when(mcpClientManager.forServer(dead)).thenReturn(deadClient);
        when(mcpClientManager.forServer(alive)).thenReturn(aliveClient);
        when(deadClient.listTools()).thenThrow(new McpClientException(
                McpClientException.FailureKind.UNREACHABLE, dead.getEndpoint(),
                "MCP endpoint " + dead.getEndpoint() + " is unreachable"));
        when(aliveClient.listTools()).thenReturn(List.of(tool("live_tool")));

        discoveryService.syncAll();

        assertThat(toolExecutionService.exists("mcp:5:live_tool")).isTrue();
        assertThat(toolExecutionService.exists("mcp:4:live_tool")).isFalse();
        verify(mcpClientManager).invalidate("http://dead:9000");
    }

    @Test
    @DisplayName("no approved servers → nothing is discovered and nothing registered")
    void noApprovedServers() {
        when(mcpServerMapper.selectList(any())).thenReturn(List.of());

        discoveryService.syncAll();

        assertThat(toolExecutionService.getRegisteredToolNames()).isEmpty();
        verifyNoInteractions(mcpClientManager);
    }

    @Test
    @DisplayName("nameless discovered tools are skipped instead of producing broken qualified names")
    void namelessToolsSkipped() {
        SfMcpServer server = server(6L, "http://mcp-6:9000");
        when(mcpServerMapper.selectList(any())).thenReturn(List.of(server));
        when(mcpClientManager.forServer(server)).thenReturn(mcpClient);
        when(mcpClient.listTools()).thenReturn(List.of(
                new McpToolSchema(null, "no name", null), tool("named")));

        discoveryService.syncAll();

        assertThat(toolExecutionService.getRegisteredToolNames()).containsExactly("mcp:6:named");
    }

    @Test
    @DisplayName("discoverForServer returns tools without registering and propagates structured failures")
    void discoverForServerContract() {
        SfMcpServer server = server(7L, "http://mcp-7:9000");
        when(mcpClientManager.forServer(server)).thenReturn(mcpClient);
        when(mcpClient.listTools()).thenReturn(List.of(tool("probe")));

        List<McpToolSchema> tools = discoveryService.discoverForServer(server);

        assertThat(tools).extracting(McpToolSchema::getName).containsExactly("probe");
        assertThat(toolExecutionService.getRegisteredToolNames()).isEmpty();

        when(mcpClient.listTools()).thenThrow(new McpClientException(
                McpClientException.FailureKind.TIMEOUT, server.getEndpoint(), "too slow"));
        assertThatThrownBy(() -> discoveryService.discoverForServer(server))
                .isInstanceOf(McpClientException.class);
        verify(mcpClientManager).invalidate("http://mcp-7:9000");
    }

    @Test
    @DisplayName("fetch failure of one server mid-sync never surfaces as an exception from syncAll")
    void syncAllSwallowsPerServerExceptions() {
        SfMcpServer broken = server(8L, "http://broken:9000");
        when(mcpServerMapper.selectList(any())).thenReturn(List.of(broken));
        when(mcpClientManager.forServer(broken)).thenThrow(
                new com.schemaplexai.common.exception.BaseException(
                        com.schemaplexai.common.result.ResultCode.INTEGRATION_NOT_FOUND,
                        "MCP server has no configured endpoint"));

        // Must complete without throwing
        discoveryService.syncAll();

        assertThat(toolExecutionService.getRegisteredToolNames()).isEmpty();
    }
}
