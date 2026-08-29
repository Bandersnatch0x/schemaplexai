package com.schemaplexai.integration.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.integration.config.IntegrationConfig;
import com.schemaplexai.integration.dto.McpToolSchema;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.mapper.McpServerMapper;
import com.schemaplexai.integration.mcp.McpClientException;
import com.schemaplexai.integration.mcp.McpClientManager;
import com.schemaplexai.integration.mcp.McpStubServer;
import com.schemaplexai.integration.mcp.McpToolDiscoveryService;
import com.schemaplexai.integration.service.ToolExecutionService;
import com.schemaplexai.integration.service.impl.McpServerServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * End-to-end tests for MCP tool discovery and execution (issue 930).
 * <p>
 * A real in-process MCP stub server speaks the actual protocol over HTTP; the
 * full production chain runs unmocked except for the database mapper:
 * discover (real initialize + tools/list) → register into the execution-chain
 * registry → queryable → execute (real tools/call back to the stub).
 * The unreachable-server degradation path is covered with a closed port.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MCP End-to-End: discover → register → queryable → execute")
class McpEndToEndTest {

    @Mock
    private McpServerMapper mcpServerMapper;

    private McpStubServer stub;
    private McpServerServiceImpl mcpServerService;
    private McpClientManager mcpClientManager;
    private ToolExecutionService toolExecutionService;
    private McpToolDiscoveryService discoveryService;

    private SfMcpServer liveServer;
    private SfMcpServer deadServer;

    @BeforeEach
    void setUp() throws IOException {
        stub = McpStubServer.start()
                .addTool("calculator", "Performs arithmetic", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "a", Map.of("type", "number"),
                                "b", Map.of("type", "number"))))
                .addTool("echo", "Echoes the input", null)
                .withCallToolText("5");

        // Production wiring with the module's 30s-bounded RestTemplate
        RestTemplate restTemplate = new IntegrationConfig()
                .restTemplate(Duration.ofSeconds(30), Duration.ofSeconds(30));
        ObjectMapper objectMapper = new ObjectMapper();
        mcpClientManager = new McpClientManager(restTemplate, objectMapper);
        mcpServerService = new McpServerServiceImpl(restTemplate, mcpClientManager);
        ReflectionTestUtils.setField(mcpServerService, "baseMapper", mcpServerMapper);

        toolExecutionService = new ToolExecutionService(List.of(), objectMapper);
        toolExecutionService.init();

        discoveryService = new McpToolDiscoveryService(
                mcpServerMapper, mcpClientManager, mcpServerService, toolExecutionService);

        liveServer = new SfMcpServer();
        liveServer.setId(1L);
        liveServer.setName("live-stub");
        liveServer.setEndpoint(stub.endpoint());
        liveServer.setStatus("ACTIVE");

        deadServer = new SfMcpServer();
        deadServer.setId(2L);
        deadServer.setName("dead");
        deadServer.setEndpoint("http://127.0.0.1:" + findClosedPort());
        deadServer.setStatus("ACTIVE");
    }

    @AfterEach
    void tearDown() {
        if (stub != null) {
            stub.close();
        }
    }

    @Test
    @DisplayName("E2E: discover over real protocol → register → queryable → execute over real protocol")
    void discoverRegisterQueryExecute() {
        when(mcpServerMapper.selectList(any())).thenReturn(List.of(liveServer));
        when(mcpServerMapper.selectById(1L)).thenReturn(liveServer);

        // 1. Discovery talks real MCP to the stub and registers into the
        //    execution-chain registry
        discoveryService.syncAll();

        // 2. Queryable through the same registry the execution chain uses
        assertThat(toolExecutionService.exists("mcp:1:calculator")).isTrue();
        assertThat(toolExecutionService.exists("mcp:1:echo")).isTrue();
        assertThat(toolExecutionService.getRegisteredToolNames())
                .containsExactlyInAnyOrder("mcp:1:calculator", "mcp:1:echo");

        // 3. Execution routes a real tools/call to the stub server
        String result = toolExecutionService.executeTool(
                "mcp:1:calculator", "{\"a\":2,\"b\":3}");
        assertThat(result).contains("5");
        assertThat(stub.lastCallToolName).isEqualTo("calculator");
        assertThat(stub.lastCallArguments.path("a").asInt()).isEqualTo(2);
        assertThat(stub.lastCallArguments.path("b").asInt()).isEqualTo(3);

        // 4. The service-level discover API returns the same real tool list
        List<McpToolSchema> tools = mcpServerService.discoverTools(1L);
        assertThat(tools).extracting(McpToolSchema::getName)
                .containsExactly("calculator", "echo");
    }

    @Test
    @DisplayName("E2E: one unreachable server degrades structurally without blocking the live one")
    void unreachableServerIsolatedAndStructured() {
        List<SfMcpServer> servers = new ArrayList<>(List.of(deadServer, liveServer));
        when(mcpServerMapper.selectList(any())).thenReturn(servers);
        when(mcpServerMapper.selectById(2L)).thenReturn(deadServer);

        // Full sync completes despite the dead server and still registers the live tools
        discoveryService.syncAll();

        assertThat(toolExecutionService.exists("mcp:1:calculator")).isTrue();
        assertThat(toolExecutionService.getRegisteredToolNames())
                .noneMatch(name -> name.startsWith("mcp:2:"));

        // Direct discovery against the unreachable server fails structurally —
        // never a silent empty list
        assertThatThrownBy(() -> mcpServerService.discoverTools(2L))
                .isInstanceOf(McpClientException.class)
                .satisfies(ex -> {
                    McpClientException mce = (McpClientException) ex;
                    assertThat(mce.getKind()).isEqualTo(McpClientException.FailureKind.UNREACHABLE);
                    assertThat(mce.getEndpoint()).isEqualTo(deadServer.getEndpoint());
                });

        // Invocation degrades to the documented "Error: ..." contract
        String degraded = mcpServerService.invokeTool(2L, "calculator", Map.of());
        assertThat(degraded).startsWith("Error: ").contains("unreachable");
    }

    @Test
    @DisplayName("E2E: discovery for an unknown server id yields empty (no external call) while execution fails structurally")
    void unknownServerConfiguration() {
        when(mcpServerMapper.selectById(99L)).thenReturn(null);

        assertThat(mcpServerService.discoverTools(99L)).isEmpty();
        assertThat(mcpServerService.invokeTool(99L, "anything", Map.of()))
                .contains("not found");
    }

    private static int findClosedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
