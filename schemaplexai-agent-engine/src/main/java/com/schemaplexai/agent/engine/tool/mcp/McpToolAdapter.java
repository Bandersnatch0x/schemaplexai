package com.schemaplexai.agent.engine.tool.mcp;

import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.ToolExecutionException;
import com.schemaplexai.agent.engine.tool.ToolResult;
import com.schemaplexai.agent.engine.tool.adapter.ExecutionContext;
import com.schemaplexai.agent.engine.tool.adapter.ToolAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter that routes MCP tool calls through the ToolAdapter interface.
 * Validates tool access via McpServerRegistry before executing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpToolAdapter implements ToolAdapter {

    private final McpServerRegistry serverRegistry;
    private final McpClientManager clientManager;

    @Override
    public String getToolName() {
        return "mcp";
    }

    /**
     * Check whether this adapter supports the given tool reference.
     * Supports any tool reference starting with "mcp:".
     */
    public boolean supports(String toolRef) {
        return toolRef != null && toolRef.startsWith("mcp:");
    }

    @Override
    public ToolResult execute(ToolCall call, ExecutionContext ctx) throws ToolExecutionException {
        String toolRef = call.toolName();
        McpToolRef ref = McpToolRef.parse(toolRef);
        if (ref == null) {
            return ToolResult.error("Invalid MCP tool reference: " + toolRef);
        }

        Long tenantId = Long.valueOf(ctx.tenantId());

        // Validate server access
        if (!serverRegistry.isAllowed(ref.serverId(), tenantId)) {
            log.warn("MCP server not allowed: {} for tenant {}", ref.serverId(), tenantId);
            return ToolResult.error("MCP server not allowed: " + ref.serverId());
        }

        // Validate tool whitelist
        if (!serverRegistry.isToolAllowed(ref.serverId(), ref.toolName(), tenantId)) {
            log.warn("MCP tool not allowed: {} on server {} for tenant {}",
                    ref.toolName(), ref.serverId(), tenantId);
            return ToolResult.error("MCP tool not allowed: " + ref.toolName());
        }

        // Get or create client
        McpClient client = clientManager.create(ref.serverId());
        if (!client.isConnected()) {
            return ToolResult.error("MCP client disconnected: " + ref.serverId());
        }

        // TODO: Actual MCP protocol call will be implemented in T18
        // For now, return a placeholder response indicating the tool was validated
        log.info("MCP tool validated: {}:{}", ref.serverId(), ref.toolName());
        return ToolResult.success("MCP tool " + ref.toolName() + " validated (execution pending)");
    }
}
