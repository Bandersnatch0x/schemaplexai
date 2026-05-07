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
    Map<String, Object> attributes,
    Map<String, Object> guardrails
) {
    public ExecutionContext(String tenantId, Long executionId, String workspaceRoot) {
        this(tenantId, executionId, workspaceRoot, Map.of(), Map.of());
    }

    public ExecutionContext(String tenantId, Long executionId, String workspaceRoot, Map<String, Object> attributes) {
        this(tenantId, executionId, workspaceRoot, attributes, Map.of());
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getGuardrail(String key) {
        return (T) guardrails.get(key);
    }
}
