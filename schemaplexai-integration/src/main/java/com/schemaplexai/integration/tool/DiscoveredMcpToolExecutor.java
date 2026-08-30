package com.schemaplexai.integration.tool;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.service.McpServerService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;

/**
 * Runtime-registered executor for one discovered MCP tool (issue 930).
 * <p>
 * Instances are created by MCP tool discovery — one per qualified tool name
 * {@code mcp:<serverId>:<toolName>} — and registered into the same
 * {@code ToolExecutionService} registry the execution chain resolves from,
 * eliminating the dead-archive where discovered tools used to be invisible
 * to execution.
 * <p>
 * Endpoint resolution: the numeric server id is used only to look up the
 * {@code sf_mcp_server} configuration row; the actual HTTP endpoint is always
 * taken from that row's {@code endpoint} column (never the id itself).
 */
@Slf4j
public class DiscoveredMcpToolExecutor implements ToolExecutor {

    /** Prefix shared with the engine-side qualified name format (SPEC-MCP). */
    public static final String QUALIFIED_NAME_PREFIX = "mcp:";

    private final String qualifiedName;
    private final Long serverId;
    private final String toolName;
    private final McpServerService mcpServerService;

    public DiscoveredMcpToolExecutor(Long serverId, String toolName, McpServerService mcpServerService) {
        this.serverId = Objects.requireNonNull(serverId, "serverId must not be null");
        this.toolName = Objects.requireNonNull(toolName, "toolName must not be null");
        this.mcpServerService = Objects.requireNonNull(mcpServerService, "mcpServerService must not be null");
        this.qualifiedName = QUALIFIED_NAME_PREFIX + serverId + ":" + toolName;
    }

    @Override
    public String getToolName() {
        return qualifiedName;
    }

    public Long getServerId() {
        return serverId;
    }

    /** The tool name as advertised by the remote MCP server. */
    public String getRemoteToolName() {
        return toolName;
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        String result = mcpServerService.invokeTool(
                serverId, toolName, parameters != null ? parameters : Map.of());
        if (result != null && result.startsWith("Error: ")) {
            // invokeTool degrades to an "Error: ..." string; the execution
            // chain needs a real failure instead of an error that looks like
            // output, so surface it as a structured tool-execution failure.
            log.warn("Discovered MCP tool {} invocation degraded: {}", qualifiedName, result);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED, result);
        }
        return result;
    }
}
