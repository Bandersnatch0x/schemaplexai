package com.schemaplexai.integration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.dto.McpToolSchema;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.mapper.McpServerMapper;
import com.schemaplexai.integration.mcp.McpClient;
import com.schemaplexai.integration.mcp.McpClientException;
import com.schemaplexai.integration.mcp.McpClientManager;
import com.schemaplexai.integration.service.McpServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class McpServerServiceImpl extends ServiceImpl<McpServerMapper, SfMcpServer> implements McpServerService {

    private final RestTemplate restTemplate;
    private final McpClientManager mcpClientManager;

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

    /**
     * Discover tools from the server via the real MCP protocol (initialize
     * handshake + {@code tools/list}, issue 930).
     * <p>
     * Failure semantics: a missing server/endpoint configuration yields an
     * empty list (nothing to discover, no external call attempted), but any
     * external failure — unreachable, timeout, non-2xx, invalid protocol —
     * surfaces as a structured {@link McpClientException} instead of a silent
     * empty list, and the poisoned client session is invalidated.
     */
    @Override
    public List<McpToolSchema> discoverTools(Long serverId) {
        SfMcpServer server = baseMapper.selectById(serverId);
        if (server == null || server.getEndpoint() == null || server.getEndpoint().isBlank()) {
            log.warn("MCP server {} not found or endpoint missing", serverId);
            return Collections.emptyList();
        }

        McpClient client = mcpClientManager.forServer(server);
        try {
            List<McpToolSchema> tools = client.listTools();
            log.info("Discovered {} tool(s) from MCP server {} ({})",
                    tools.size(), serverId, server.getEndpoint());
            return tools;
        } catch (McpClientException e) {
            mcpClientManager.invalidate(server.getEndpoint());
            log.warn("MCP discovery failed structurally for server {} ({}): kind={}, {}",
                    serverId, server.getEndpoint(), e.getKind(), e.getMessage());
            throw e;
        }
    }

    /**
     * Invoke a tool on the server via the real MCP protocol ({@code tools/call}).
     * <p>
     * Degrade contract preserved (issues 917/918): failures do not propagate as
     * exceptions but return an {@code "Error: ..."} string, while the poisoned
     * client session is invalidated for the next attempt.
     */
    @Override
    public String invokeTool(Long serverId, String toolName, Map<String, Object> arguments) {
        SfMcpServer server = baseMapper.selectById(serverId);
        if (server == null || server.getEndpoint() == null || server.getEndpoint().isBlank()) {
            log.warn("MCP server {} not found or endpoint missing", serverId);
            return "Error: MCP server not found or endpoint missing";
        }

        McpClient client = mcpClientManager.forServer(server);
        try {
            String result = client.callTool(toolName, arguments);
            log.info("MCP server {} tool {} invoked successfully", serverId, toolName);
            return result;
        } catch (McpClientException e) {
            mcpClientManager.invalidate(server.getEndpoint());
            log.error("Failed to invoke tool {} on MCP server {} ({}): kind={}",
                    toolName, serverId, server.getEndpoint(), e.getKind());
            return "Error: " + e.getMessage();
        }
    }
}
