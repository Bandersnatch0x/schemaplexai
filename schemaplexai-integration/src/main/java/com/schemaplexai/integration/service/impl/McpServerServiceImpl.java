package com.schemaplexai.integration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.dto.McpToolSchema;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.mapper.McpServerMapper;
import com.schemaplexai.integration.service.McpServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class McpServerServiceImpl extends ServiceImpl<McpServerMapper, SfMcpServer> implements McpServerService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean healthCheck(Long serverId) {
        SfMcpServer server = baseMapper.selectById(serverId);
        if (server == null || server.getEndpoint() == null || server.getEndpoint().isBlank()) {
            return false;
        }

        try {
            restTemplate.getForObject(server.getEndpoint() + "/health", String.class);
            log.info("MCP server {} health check passed", serverId);
            return true;
        } catch (ResourceAccessException e) {
            log.warn("MCP server {} health check failed: connection refused", serverId);
            return false;
        } catch (Exception e) {
            log.warn("MCP server {} health check failed: {}", serverId, e.getMessage());
            return false;
        }
    }

    @Override
    public void validateEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "MCP endpoint is required");
        }
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            throw new BaseException(ResultCode.PARAM_ERROR, "MCP endpoint must start with http:// or https://");
        }
    }

    @Override
    public List<McpToolSchema> discoverTools(Long serverId) {
        SfMcpServer server = baseMapper.selectById(serverId);
        if (server == null || server.getEndpoint() == null || server.getEndpoint().isBlank()) {
            log.warn("MCP server {} not found or endpoint missing", serverId);
            return Collections.emptyList();
        }

        String baseUrl = server.getEndpoint();
        String requestBody = "{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":1}";

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/discover"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("MCP server {} discover returned status {}", serverId, response.statusCode());
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode resultNode = root.path("result");
            if (resultNode.isMissingNode() || !resultNode.isArray()) {
                log.warn("MCP server {} discover response missing result array", serverId);
                return Collections.emptyList();
            }

            List<McpToolSchema> tools = new ArrayList<>();
            for (JsonNode toolNode : resultNode) {
                McpToolSchema tool = new McpToolSchema();
                tool.setName(toolNode.path("name").asText(null));
                tool.setDescription(toolNode.path("description").asText(null));
                JsonNode inputSchema = toolNode.path("inputSchema");
                if (!inputSchema.isMissingNode()) {
                    tool.setInputSchema(objectMapper.convertValue(inputSchema, Map.class));
                }
                tools.add(tool);
            }

            return tools;
        } catch (Exception e) {
            log.error("Failed to discover tools from MCP server {}", serverId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public String invokeTool(Long serverId, String toolName, Map<String, Object> arguments) {
        SfMcpServer server = baseMapper.selectById(serverId);
        if (server == null || server.getEndpoint() == null || server.getEndpoint().isBlank()) {
            log.warn("MCP server {} not found or endpoint missing", serverId);
            return "Error: MCP server not found or endpoint missing";
        }

        String baseUrl = server.getEndpoint();
        String requestBody;
        try {
            Map<String, Object> params = Map.of(
                    "name", toolName,
                    "arguments", arguments
            );
            Map<String, Object> rpcBody = Map.of(
                    "jsonrpc", "2.0",
                    "method", "tools/call",
                    "params", params,
                    "id", 1
            );
            requestBody = objectMapper.writeValueAsString(rpcBody);
        } catch (Exception e) {
            log.error("Failed to serialize invoke request for MCP server {}", serverId, e);
            return "Error: failed to build request";
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/invoke"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("MCP server {} invoke returned status {}", serverId, response.statusCode());
                return "Error: HTTP " + response.statusCode();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode resultNode = root.path("result");
            if (!resultNode.isMissingNode()) {
                return resultNode.asText();
            }
            JsonNode errorNode = root.path("error");
            if (!errorNode.isMissingNode()) {
                return "Error: " + errorNode.path("message").asText("unknown error");
            }
            return response.body();
        } catch (Exception e) {
            log.error("Failed to invoke tool on MCP server {}", serverId, e);
            return "Error: " + e.getMessage();
        }
    }
}
