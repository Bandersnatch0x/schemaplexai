package com.schemaplexai.agent.engine.tool.adapter;

import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.ToolResult;
import java.util.Map;

/**
 * Execution context passed to ToolAdapter implementations.
 * Contains runtime information needed for safe tool execution.
 */
public record ExecutionContext(
    String tenantId,
    Long executionId,
    String workspaceRoot,
    Map<String, Object> attributes
) {
    public ExecutionContext(String tenantId, Long executionId, String workspaceRoot) {
        this(tenantId, executionId, workspaceRoot, Map.of());
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
}
