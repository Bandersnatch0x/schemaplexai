package com.schemaplexai.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.dto.McpToolSchema;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.mapper.McpServerMapper;
import com.schemaplexai.integration.service.impl.McpServerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpServerServiceImplTest {

    @Mock
    private McpServerMapper mcpServerMapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private McpServerServiceImpl mcpServerService;

    @BeforeEach
    void setUp() {
        // McpServerServiceImpl extends ServiceImpl which stores mapper in baseMapper field.
        // @InjectMocks can't inject into parent class fields, so set it manually.
        ReflectionTestUtils.setField(mcpServerService, "baseMapper", mcpServerMapper);
    }

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

    @Test
    void healthCheck_serverNotFound_returnsFalse() {
        when(mcpServerMapper.selectById(100L)).thenReturn(null);

        boolean result = mcpServerService.healthCheck(100L);

        assertThat(result).isFalse();
    }

    @Test
    void healthCheck_nullEndpoint_returnsFalse() {
        SfMcpServer server = new SfMcpServer();
        server.setId(200L);
        server.setEndpoint(null);
        when(mcpServerMapper.selectById(200L)).thenReturn(server);

        boolean result = mcpServerService.healthCheck(200L);

        assertThat(result).isFalse();
    }

    @Test
    void healthCheck_blankEndpoint_returnsFalse() {
        SfMcpServer server = new SfMcpServer();
        server.setId(300L);
        server.setEndpoint("");
        when(mcpServerMapper.selectById(300L)).thenReturn(server);

        boolean result = mcpServerService.healthCheck(300L);

        assertThat(result).isFalse();
    }

    @Test
    void discoverTools_serverNotFound_returnsEmptyList() {
        when(mcpServerMapper.selectById(100L)).thenReturn(null);

        List<McpToolSchema> result = mcpServerService.discoverTools(100L);

        assertThat(result).isEmpty();
    }

    @Test
    void discoverTools_nullEndpoint_returnsEmptyList() {
        SfMcpServer server = new SfMcpServer();
        server.setId(200L);
        server.setEndpoint(null);
        when(mcpServerMapper.selectById(200L)).thenReturn(server);

        List<McpToolSchema> result = mcpServerService.discoverTools(200L);

        assertThat(result).isEmpty();
    }

    @Test
    void invokeTool_serverNotFound_returnsErrorMessage() {
        when(mcpServerMapper.selectById(100L)).thenReturn(null);

        String result = mcpServerService.invokeTool(100L, "testTool", Map.of());

        assertThat(result).contains("not found");
    }

    @Test
    void invokeTool_nullEndpoint_returnsErrorMessage() {
        SfMcpServer server = new SfMcpServer();
        server.setId(200L);
        server.setEndpoint(null);
        when(mcpServerMapper.selectById(200L)).thenReturn(server);

        String result = mcpServerService.invokeTool(200L, "testTool", Map.of());

        assertThat(result).contains("not found");
    }

    // --- healthCheck success path ---

    @Test
    void healthCheck_success_returnsTrue() {
        SfMcpServer server = new SfMcpServer();
        server.setId(400L);
        server.setEndpoint("http://localhost:3000");
        when(mcpServerMapper.selectById(400L)).thenReturn(server);
        when(restTemplate.getForObject("http://localhost:3000/health", String.class))
                .thenReturn("ok");

        boolean result = mcpServerService.healthCheck(400L);

        assertThat(result).isTrue();
    }

    @Test
    void healthCheck_resourceAccessException_returnsFalse() {
        SfMcpServer server = new SfMcpServer();
        server.setId(500L);
        server.setEndpoint("http://localhost:3000");
        when(mcpServerMapper.selectById(500L)).thenReturn(server);
        when(restTemplate.getForObject("http://localhost:3000/health", String.class))
                .thenThrow(new ResourceAccessException("Connection refused"));

        boolean result = mcpServerService.healthCheck(500L);

        assertThat(result).isFalse();
    }

    @Test
    void healthCheck_genericException_returnsFalse() {
        SfMcpServer server = new SfMcpServer();
        server.setId(600L);
        server.setEndpoint("http://localhost:3000");
        when(mcpServerMapper.selectById(600L)).thenReturn(server);
        when(restTemplate.getForObject("http://localhost:3000/health", String.class))
                .thenThrow(new RuntimeException("boom"));

        boolean result = mcpServerService.healthCheck(600L);

        assertThat(result).isFalse();
    }

    // --- discoverTools success path ---

    @Test
    @SuppressWarnings("unchecked")
    void discoverTools_success_returnsTools() throws Exception {
        SfMcpServer server = new SfMcpServer();
        server.setId(700L);
        server.setEndpoint("http://localhost:3000");
        when(mcpServerMapper.selectById(700L)).thenReturn(server);

        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"jsonrpc\":\"2.0\",\"result\":[{\"name\":\"tool1\",\"description\":\"desc1\",\"inputSchema\":{\"type\":\"object\"}}],\"id\":1}");
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
            HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
            when(mockBuilder.connectTimeout(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockClient);
            httpClientStatic.when(HttpClient::newBuilder).thenReturn(mockBuilder);

            List<McpToolSchema> result = mcpServerService.discoverTools(700L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("tool1");
            assertThat(result.get(0).getDescription()).isEqualTo("desc1");
            assertThat(result.get(0).getInputSchema()).containsEntry("type", "object");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void discoverTools_non200Status_returnsEmpty() throws Exception {
        SfMcpServer server = new SfMcpServer();
        server.setId(800L);
        server.setEndpoint("http://localhost:3000");
        when(mcpServerMapper.selectById(800L)).thenReturn(server);

        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
            HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
            when(mockBuilder.connectTimeout(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockClient);
            httpClientStatic.when(HttpClient::newBuilder).thenReturn(mockBuilder);

            List<McpToolSchema> result = mcpServerService.discoverTools(800L);

            assertThat(result).isEmpty();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void discoverTools_missingResultArray_returnsEmpty() throws Exception {
        SfMcpServer server = new SfMcpServer();
        server.setId(900L);
        server.setEndpoint("http://localhost:3000");
        when(mcpServerMapper.selectById(900L)).thenReturn(server);

        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"jsonrpc\":\"2.0\",\"result\":\"not-array\",\"id\":1}");
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
            HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
            when(mockBuilder.connectTimeout(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockClient);
            httpClientStatic.when(HttpClient::newBuilder).thenReturn(mockBuilder);

            List<McpToolSchema> result = mcpServerService.discoverTools(900L);

            assertThat(result).isEmpty();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void discoverTools_httpClientThrows_returnsEmpty() throws Exception {
        SfMcpServer server = new SfMcpServer();
        server.setId(1000L);
        server.setEndpoint("http://localhost:3000");
        when(mcpServerMapper.selectById(1000L)).thenReturn(server);

        HttpClient mockClient = mock(HttpClient.class);
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("connection failed"));

        try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
            HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
            when(mockBuilder.connectTimeout(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockClient);
            httpClientStatic.when(HttpClient::newBuilder).thenReturn(mockBuilder);

            List<McpToolSchema> result = mcpServerService.discoverTools(1000L);

            assertThat(result).isEmpty();
        }
    }

    // --- invokeTool success path ---

    @Test
    @SuppressWarnings("unchecked")
    void invokeTool_success_returnsResult() throws Exception {
        SfMcpServer server = new SfMcpServer();
        server.setId(1100L);
        server.setEndpoint("http://localhost:3000");
        when(mcpServerMapper.selectById(1100L)).thenReturn(server);

        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"jsonrpc\":\"2.0\",\"result\":\"tool-output\",\"id\":1}");
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
            HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
            when(mockBuilder.connectTimeout(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockClient);
            httpClientStatic.when(HttpClient::newBuilder).thenReturn(mockBuilder);

            String result = mcpServerService.invokeTool(1100L, "testTool", Map.of("key", "value"));

            assertThat(result).isEqualTo("tool-output");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void invokeTool_errorNode_returnsErrorMessage() throws Exception {
        SfMcpServer server = new SfMcpServer();
        server.setId(1200L);
        server.setEndpoint("http://localhost:3000");
        when(mcpServerMapper.selectById(1200L)).thenReturn(server);

        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"jsonrpc\":\"2.0\",\"error\":{\"message\":\"tool not found\"},\"id\":1}");
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
            HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
            when(mockBuilder.connectTimeout(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockClient);
            httpClientStatic.when(HttpClient::newBuilder).thenReturn(mockBuilder);

            String result = mcpServerService.invokeTool(1200L, "testTool", Map.of());

            assertThat(result).isEqualTo("Error: tool not found");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void invokeTool_non200Status_returnsError() throws Exception {
        SfMcpServer server = new SfMcpServer();
        server.setId(1300L);
        server.setEndpoint("http://localhost:3000");
        when(mcpServerMapper.selectById(1300L)).thenReturn(server);

        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
            HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
            when(mockBuilder.connectTimeout(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockClient);
            httpClientStatic.when(HttpClient::newBuilder).thenReturn(mockBuilder);

            String result = mcpServerService.invokeTool(1300L, "testTool", Map.of());

            assertThat(result).isEqualTo("Error: HTTP 500");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void invokeTool_httpClientThrows_returnsErrorMessage() throws Exception {
        SfMcpServer server = new SfMcpServer();
        server.setId(1400L);
        server.setEndpoint("http://localhost:3000");
        when(mcpServerMapper.selectById(1400L)).thenReturn(server);

        HttpClient mockClient = mock(HttpClient.class);
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("invoke failed"));

        try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
            HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
            when(mockBuilder.connectTimeout(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockClient);
            httpClientStatic.when(HttpClient::newBuilder).thenReturn(mockBuilder);

            String result = mcpServerService.invokeTool(1400L, "testTool", Map.of());

            assertThat(result).isEqualTo("Error: invoke failed");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void invokeTool_noResultOrError_returnsBody() throws Exception {
        SfMcpServer server = new SfMcpServer();
        server.setId(1500L);
        server.setEndpoint("http://localhost:3000");
        when(mcpServerMapper.selectById(1500L)).thenReturn(server);

        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"jsonrpc\":\"2.0\",\"id\":1}");
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        try (MockedStatic<HttpClient> httpClientStatic = mockStatic(HttpClient.class)) {
            HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
            when(mockBuilder.connectTimeout(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockClient);
            httpClientStatic.when(HttpClient::newBuilder).thenReturn(mockBuilder);

            String result = mcpServerService.invokeTool(1500L, "testTool", Map.of());

            assertThat(result).isEqualTo("{\"jsonrpc\":\"2.0\",\"id\":1}");
        }
    }
}
