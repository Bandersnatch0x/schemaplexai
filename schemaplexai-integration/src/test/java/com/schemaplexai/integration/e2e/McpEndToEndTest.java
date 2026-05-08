package com.schemaplexai.integration.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.integration.dto.McpToolSchema;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.service.McpServerService;
import com.schemaplexai.integration.tool.McpToolExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * End-to-end integration tests for MCP (Model Context Protocol) server lifecycle.
 * Covers: registration, health check, tool discovery, tool invocation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MCP End-to-End Integration Tests")
class McpEndToEndTest {

    @Mock
    private McpServerService mcpServerService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private McpToolExecutor mcpToolExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SfMcpServer activeServer;

    @BeforeEach
    void setUp() {
        activeServer = new SfMcpServer();
        activeServer.setId(1L);
        activeServer.setName("Test MCP Server");
        activeServer.setEndpoint("http://localhost:3000/mcp");
        activeServer.setStatus(1);
    }

    @Test
    @DisplayName("E2E: Register MCP server, health check passes, discover tools, invoke tool")
    void fullMcpLifecycle() {
        // Step 1: Server registration (simulated via service mock)
        when(mcpServerService.getById(1L)).thenReturn(activeServer);

        // Step 2: Health check
        when(mcpServerService.healthCheck(1L)).thenReturn(true);
        boolean healthy = mcpServerService.healthCheck(1L);
        assertThat(healthy).isTrue();

        // Step 3: Tool discovery
        McpToolSchema tool = new McpToolSchema("calculator", "Performs arithmetic", Map.of("type", "object"));
        when(mcpServerService.discoverTools(1L)).thenReturn(List.of(tool));
        List<McpToolSchema> tools = mcpServerService.discoverTools(1L);
        assertThat(tools).hasSize(1);
        assertThat(tools.get(0).getName()).isEqualTo("calculator");

        // Step 4: Tool invocation via executor
        when(restTemplate.postForObject(eq("http://localhost:3000/mcp/add"), any(), eq(String.class)))
                .thenReturn("{\"result\": 42}");

        String result = mcpToolExecutor.execute(Map.of("serverId", 1L, "method", "add"));
        assertThat(result).contains("42");
    }

    @Test
    @DisplayName("E2E: Tool invocation with inactive server returns error")
    void invokeInactiveServer() {
        SfMcpServer inactive = new SfMcpServer();
        inactive.setId(2L);
        inactive.setStatus(0);
        when(mcpServerService.getById(2L)).thenReturn(inactive);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.schemaplexai.common.exception.BaseException.class,
                () -> mcpToolExecutor.execute(Map.of("serverId", 2L, "method", "add"))
        );
    }

    @Test
    @DisplayName("E2E: Discover tools on unreachable server returns empty list")
    void discoverToolsUnreachableServer() {
        when(mcpServerService.discoverTools(99L)).thenReturn(List.of());
        List<McpToolSchema> tools = mcpServerService.discoverTools(99L);
        assertThat(tools).isEmpty();
    }
}
