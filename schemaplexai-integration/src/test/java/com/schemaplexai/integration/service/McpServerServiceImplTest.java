package com.schemaplexai.integration.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.dto.McpToolSchema;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.mapper.McpServerMapper;
import com.schemaplexai.integration.mcp.McpClient;
import com.schemaplexai.integration.mcp.McpClientException;
import com.schemaplexai.integration.mcp.McpClientManager;
import com.schemaplexai.integration.service.impl.McpServerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link McpServerServiceImpl}. Discovery and invocation are
 * delegated to the real {@link McpClient} (issue 930), mocked here at the
 * client boundary; protocol-level behavior is covered by McpClientTest and
 * the end-to-end suite.
 */
@ExtendWith(MockitoExtension.class)
class McpServerServiceImplTest {

    @Mock
    private McpServerMapper mcpServerMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private McpClientManager mcpClientManager;

    @Mock
    private McpClient mcpClient;

    @InjectMocks
    private McpServerServiceImpl mcpServerService;

    @BeforeEach
    void setUp() {
        // McpServerServiceImpl extends ServiceImpl which stores mapper in baseMapper field.
        // @InjectMocks can't inject into parent class fields, so set it manually.
        ReflectionTestUtils.setField(mcpServerService, "baseMapper", mcpServerMapper);
    }

    private static SfMcpServer server(Long id, String endpoint) {
        SfMcpServer server = new SfMcpServer();
        server.setId(id);
        server.setEndpoint(endpoint);
        return server;
    }

    // --- validateEndpoint ---

    @Test
    void validateEndpoint_nullEndpoint_throwsParamError() {
        assertThatThrownBy(() -> mcpServerService.validateEndpoint(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void validateEndpoint_blankEndpoint_throwsParamError() {
        assertThatThrownBy(() -> mcpServerService.validateEndpoint("   "))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void validateEndpoint_emptyEndpoint_throwsParamError() {
        assertThatThrownBy(() -> mcpServerService.validateEndpoint(""))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void validateEndpoint_httpEndpoint_passes() {
        mcpServerService.validateEndpoint("http://localhost:8080/mcp");
    }

    @Test
    void validateEndpoint_httpsEndpoint_passes() {
        mcpServerService.validateEndpoint("https://mcp.example.com/api");
    }

    @Test
    void validateEndpoint_ftpEndpoint_throwsParamError() {
        assertThatThrownBy(() -> mcpServerService.validateEndpoint("ftp://server/file"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void validateEndpoint_noProtocol_throwsParamError() {
        assertThatThrownBy(() -> mcpServerService.validateEndpoint("localhost:8080/mcp"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // --- healthCheck ---

    @Test
    void healthCheck_serverNotFound_returnsFalse() {
        when(mcpServerMapper.selectById(100L)).thenReturn(null);

        boolean result = mcpServerService.healthCheck(100L);

        assertThat(result).isFalse();
    }

    @Test
    void healthCheck_nullEndpoint_returnsFalse() {
        when(mcpServerMapper.selectById(200L)).thenReturn(server(200L, null));

        boolean result = mcpServerService.healthCheck(200L);

        assertThat(result).isFalse();
    }

    @Test
    void healthCheck_blankEndpoint_returnsFalse() {
        when(mcpServerMapper.selectById(300L)).thenReturn(server(300L, ""));

        boolean result = mcpServerService.healthCheck(300L);

        assertThat(result).isFalse();
    }

    @Test
    void healthCheck_success_returnsTrue() {
        when(mcpServerMapper.selectById(400L)).thenReturn(server(400L, "http://localhost:3000"));
        when(restTemplate.getForObject("http://localhost:3000/health", String.class))
                .thenReturn("ok");

        boolean result = mcpServerService.healthCheck(400L);

        assertThat(result).isTrue();
    }

    @Test
    void healthCheck_resourceAccessException_returnsFalse() {
        when(mcpServerMapper.selectById(500L)).thenReturn(server(500L, "http://localhost:3000"));
        when(restTemplate.getForObject("http://localhost:3000/health", String.class))
                .thenThrow(new ResourceAccessException("Connection refused"));

        boolean result = mcpServerService.healthCheck(500L);

        assertThat(result).isFalse();
    }

    @Test
    void healthCheck_genericException_returnsFalse() {
        when(mcpServerMapper.selectById(600L)).thenReturn(server(600L, "http://localhost:3000"));
        when(restTemplate.getForObject("http://localhost:3000/health", String.class))
                .thenThrow(new RuntimeException("boom"));

        boolean result = mcpServerService.healthCheck(600L);

        assertThat(result).isFalse();
    }

    // --- discoverTools: configuration gaps (no external call) ---

    @Test
    void discoverTools_serverNotFound_returnsEmptyList() {
        when(mcpServerMapper.selectById(100L)).thenReturn(null);

        List<McpToolSchema> result = mcpServerService.discoverTools(100L);

        assertThat(result).isEmpty();
        verifyNoInteractions(mcpClientManager);
    }

    @Test
    void discoverTools_nullEndpoint_returnsEmptyList() {
        when(mcpServerMapper.selectById(200L)).thenReturn(server(200L, null));

        List<McpToolSchema> result = mcpServerService.discoverTools(200L);

        assertThat(result).isEmpty();
        verifyNoInteractions(mcpClientManager);
    }

    // --- discoverTools: real protocol delegation ---

    @Test
    void discoverTools_success_returnsToolsFromClient() {
        SfMcpServer server = server(700L, "http://localhost:3000");
        when(mcpServerMapper.selectById(700L)).thenReturn(server);
        when(mcpClientManager.forServer(server)).thenReturn(mcpClient);
        when(mcpClient.listTools()).thenReturn(List.of(
                new McpToolSchema("tool1", "desc1", Map.of("type", "object"))));

        List<McpToolSchema> result = mcpServerService.discoverTools(700L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("tool1");
        assertThat(result.get(0).getDescription()).isEqualTo("desc1");
        assertThat(result.get(0).getInputSchema()).containsEntry("type", "object");
    }

    // --- discoverTools: structured failure, never silent empty (issue 930) ---

    @Test
    void discoverTools_unreachable_throwsStructuredAndInvalidates() {
        SfMcpServer server = server(1000L, "http://localhost:3000");
        when(mcpServerMapper.selectById(1000L)).thenReturn(server);
        when(mcpClientManager.forServer(server)).thenReturn(mcpClient);
        when(mcpClient.listTools()).thenThrow(new McpClientException(
                McpClientException.FailureKind.UNREACHABLE, server.getEndpoint(),
                "MCP endpoint " + server.getEndpoint() + " is unreachable"));

        assertThatThrownBy(() -> mcpServerService.discoverTools(1000L))
                .isInstanceOf(McpClientException.class)
                .satisfies(ex -> assertThat(((McpClientException) ex).getKind())
                        .isEqualTo(McpClientException.FailureKind.UNREACHABLE));
        verify(mcpClientManager).invalidate("http://localhost:3000");
    }

    @Test
    void discoverTools_timeout_throwsStructuredAndInvalidates() {
        SfMcpServer server = server(1001L, "http://localhost:3000");
        when(mcpServerMapper.selectById(1001L)).thenReturn(server);
        when(mcpClientManager.forServer(server)).thenReturn(mcpClient);
        when(mcpClient.listTools()).thenThrow(new McpClientException(
                McpClientException.FailureKind.TIMEOUT, server.getEndpoint(),
                "MCP endpoint " + server.getEndpoint() + " is too slow (30s budget exceeded)"));

        assertThatThrownBy(() -> mcpServerService.discoverTools(1001L))
                .isInstanceOf(McpClientException.class)
                .satisfies(ex -> assertThat(((McpClientException) ex).getKind())
                        .isEqualTo(McpClientException.FailureKind.TIMEOUT));
        verify(mcpClientManager).invalidate("http://localhost:3000");
    }

    @Test
    void discoverTools_protocolError_throwsStructuredAndInvalidates() {
        SfMcpServer server = server(1002L, "http://localhost:3000");
        when(mcpServerMapper.selectById(1002L)).thenReturn(server);
        when(mcpClientManager.forServer(server)).thenReturn(mcpClient);
        when(mcpClient.listTools()).thenThrow(new McpClientException(
                McpClientException.FailureKind.PROTOCOL_ERROR, server.getEndpoint(),
                "MCP tools/list response from " + server.getEndpoint() + " carries no result"));

        assertThatThrownBy(() -> mcpServerService.discoverTools(1002L))
                .isInstanceOf(McpClientException.class)
                .satisfies(ex -> assertThat(((McpClientException) ex).getKind())
                        .isEqualTo(McpClientException.FailureKind.PROTOCOL_ERROR));
        verify(mcpClientManager).invalidate("http://localhost:3000");
    }

    // --- invokeTool ---

    @Test
    void invokeTool_serverNotFound_returnsErrorMessage() {
        when(mcpServerMapper.selectById(100L)).thenReturn(null);

        String result = mcpServerService.invokeTool(100L, "testTool", Map.of());

        assertThat(result).contains("not found");
    }

    @Test
    void invokeTool_nullEndpoint_returnsErrorMessage() {
        when(mcpServerMapper.selectById(200L)).thenReturn(server(200L, null));

        String result = mcpServerService.invokeTool(200L, "testTool", Map.of());

        assertThat(result).contains("not found");
    }

    @Test
    void invokeTool_success_returnsClientResult() {
        SfMcpServer server = server(1100L, "http://localhost:3000");
        when(mcpServerMapper.selectById(1100L)).thenReturn(server);
        when(mcpClientManager.forServer(server)).thenReturn(mcpClient);
        when(mcpClient.callTool("testTool", Map.of("key", "value")))
                .thenReturn("{\"content\":[{\"type\":\"text\",\"text\":\"tool-output\"}]}");

        String result = mcpServerService.invokeTool(1100L, "testTool", Map.of("key", "value"));

        assertThat(result).contains("tool-output");
    }

    @Test
    void invokeTool_clientFailure_degradesToErrorStringAndInvalidates() {
        SfMcpServer server = server(1400L, "http://localhost:3000");
        when(mcpServerMapper.selectById(1400L)).thenReturn(server);
        when(mcpClientManager.forServer(server)).thenReturn(mcpClient);
        when(mcpClient.callTool("testTool", Map.of())).thenThrow(new McpClientException(
                McpClientException.FailureKind.UNREACHABLE, server.getEndpoint(),
                "MCP endpoint " + server.getEndpoint() + " is unreachable"));

        String result = mcpServerService.invokeTool(1400L, "testTool", Map.of());

        assertThat(result).startsWith("Error: ").contains("unreachable");
        verify(mcpClientManager).invalidate("http://localhost:3000");
    }

    @Test
    void invokeTool_jsonRpcError_degradesToErrorString() {
        SfMcpServer server = server(1200L, "http://localhost:3000");
        when(mcpServerMapper.selectById(1200L)).thenReturn(server);
        when(mcpClientManager.forServer(server)).thenReturn(mcpClient);
        when(mcpClient.callTool("testTool", Map.of())).thenThrow(new McpClientException(
                McpClientException.FailureKind.PROTOCOL_ERROR, server.getEndpoint(),
                "MCP tools/call failed on " + server.getEndpoint() + ": tool not found"));

        String result = mcpServerService.invokeTool(1200L, "testTool", Map.of());

        assertThat(result).startsWith("Error: ").contains("tool not found");
    }
}
