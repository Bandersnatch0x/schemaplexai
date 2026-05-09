package com.schemaplexai.integration.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.service.McpServerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpToolExecutorTest {

    @Mock
    private McpServerService mcpServerService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private McpToolExecutor executor;

    @Test
    void getToolName_returnsMcp() {
        assertThat(executor.getToolName()).isEqualTo("mcp");
    }

    @Test
    void execute_serverNotFound_throwsNotFound() {
        when(mcpServerService.getById(1L)).thenReturn(null);

        Map<String, Object> params = Map.of("serverId", 1, "method", "test");
        assertThatThrownBy(() -> executor.execute(params))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTEGRATION_NOT_FOUND.getCode());
    }

    @Test
    void execute_serverInactive_throwsNotFound() {
        SfMcpServer server = new SfMcpServer();
        server.setId(1L);
        server.setStatus(0);
        when(mcpServerService.getById(1L)).thenReturn(server);

        Map<String, Object> params = Map.of("serverId", 1, "method", "test");
        assertThatThrownBy(() -> executor.execute(params))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTEGRATION_NOT_FOUND.getCode());
    }

    @Test
    void execute_serverNullStatus_throwsNotFound() {
        SfMcpServer server = new SfMcpServer();
        server.setId(1L);
        server.setStatus(null);
        when(mcpServerService.getById(1L)).thenReturn(server);

        Map<String, Object> params = Map.of("serverId", 1, "method", "test");
        assertThatThrownBy(() -> executor.execute(params))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTEGRATION_NOT_FOUND.getCode());
    }

    @Test
    void execute_success_returnsResponse() {
        SfMcpServer server = new SfMcpServer();
        server.setId(1L);
        server.setStatus(1);
        server.setEndpoint("http://localhost:3000");
        when(mcpServerService.getById(1L)).thenReturn(server);
        when(restTemplate.postForObject(eq("http://localhost:3000/test"), any(HttpEntity.class), eq(String.class)))
                .thenReturn("{\"result\": \"ok\"}");

        Map<String, Object> params = Map.of("serverId", 1, "method", "test");
        String result = executor.execute(params);

        assertThat(result).isEqualTo("{\"result\": \"ok\"}");
    }

    @Test
    void execute_nullResponse_returnsEmptyJson() {
        SfMcpServer server = new SfMcpServer();
        server.setId(1L);
        server.setStatus(1);
        server.setEndpoint("http://localhost:3000");
        when(mcpServerService.getById(1L)).thenReturn(server);
        when(restTemplate.postForObject(eq("http://localhost:3000/test"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(null);

        Map<String, Object> params = Map.of("serverId", 1, "method", "test");
        String result = executor.execute(params);

        assertThat(result).isEqualTo("{}");
    }

    @Test
    void execute_restTemplateThrows_throwsToolExecutionFailed() {
        SfMcpServer server = new SfMcpServer();
        server.setId(1L);
        server.setStatus(1);
        server.setEndpoint("http://localhost:3000");
        when(mcpServerService.getById(1L)).thenReturn(server);
        when(restTemplate.postForObject(eq("http://localhost:3000/test"), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("connection refused"));

        Map<String, Object> params = Map.of("serverId", 1, "method", "test");
        assertThatThrownBy(() -> executor.execute(params))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
    }
}
