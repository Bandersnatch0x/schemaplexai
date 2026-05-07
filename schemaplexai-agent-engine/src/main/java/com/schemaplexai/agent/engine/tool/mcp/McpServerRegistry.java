package com.schemaplexai.agent.engine.tool.mcp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.mapper.McpServerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Registry that validates MCP server access via whitelist checks against the database.
 *
 * Allowed servers have status=1 (active) and match the tenant.
 * Tools are further restricted by the server's {@code toolWhitelist} field.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpServerRegistry {

    private final McpServerMapper mcpServerMapper;

    /**
     * Check whether an MCP server endpoint is allowed for the given tenant.
     * A server is allowed when it exists in the DB with status=1.
     */
    public boolean isAllowed(String endpoint, Long tenantId) {
        if (endpoint == null || endpoint.isBlank()) {
            return false;
        }
        SfMcpServer server = queryServer(endpoint, tenantId);
        return server != null && Integer.valueOf(1).equals(server.getStatus());
    }

    /**
     * Check whether a specific tool is allowed on a server for the given tenant.
     * The tool must be in the server's toolWhitelist, or the whitelist must be
     * null/empty (meaning all tools are allowed).
     */
    public boolean isToolAllowed(String endpoint, String toolName, Long tenantId) {
        SfMcpServer server = queryServer(endpoint, tenantId);
        if (server == null || !Integer.valueOf(1).equals(server.getStatus())) {
            return false;
        }
        List<String> whitelist = server.getToolWhitelist();
        // null or empty whitelist means all tools are allowed
        if (whitelist == null || whitelist.isEmpty()) {
            return true;
        }
        return whitelist.contains(toolName);
    }

    /**
     * Get the MCP server entity if it is active, null otherwise.
     */
    public SfMcpServer getServer(String endpoint, Long tenantId) {
        SfMcpServer server = queryServer(endpoint, tenantId);
        if (server != null && Integer.valueOf(1).equals(server.getStatus())) {
            return server;
        }
        return null;
    }

    // ── internal ────────────────────────────────────────────────────

    private SfMcpServer queryServer(String endpoint, Long tenantId) {
        if (endpoint == null || endpoint.isBlank()) {
            return null;
        }
        return mcpServerMapper.selectOne(
                new LambdaQueryWrapper<SfMcpServer>()
                        .eq(SfMcpServer::getEndpoint, endpoint)
                        .eq(SfMcpServer::getTenantId, String.valueOf(tenantId))
        );
    }
}
