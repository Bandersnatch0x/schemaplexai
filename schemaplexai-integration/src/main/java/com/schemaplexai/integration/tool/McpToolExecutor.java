package com.schemaplexai.integration.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.service.McpServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpToolExecutor implements ToolExecutor {

    private final McpServerService mcpServerService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String getToolName() {
        return "mcp";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        Long serverId = ((Number) parameters.get("serverId")).longValue();
        String method = (String) parameters.get("method");

        SfMcpServer server = mcpServerService.getById(serverId);
        if (server == null) {
            throw new BaseException(ResultCode.INTEGRATION_NOT_FOUND, "MCP server not found: " + serverId);
        }
        if (server.getStatus() == null || server.getStatus() != 1) {
            throw new BaseException(ResultCode.INTEGRATION_NOT_FOUND, "MCP server is not active: " + serverId);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(parameters, headers);
            String response = restTemplate.postForObject(server.getEndpoint() + "/" + method, request, String.class);
            log.info("MCP server {} responded for method {}", serverId, method);
            return response != null ? response : "{}";
        } catch (Exception e) {
            log.error("MCP tool execution failed: serverId={}, method={}", serverId, method, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED, "MCP call failed: " + e.getMessage());
        }
    }
}
