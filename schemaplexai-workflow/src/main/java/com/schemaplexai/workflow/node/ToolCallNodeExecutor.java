package com.schemaplexai.workflow.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ToolCallNodeExecutor implements NodeExecutor {

    @Override
    public String getNodeType() {
        return "TOOL_CALL";
    }

    @Override
    public NodeExecutionResult execute(Map<String, Object> input, String tenantId) {
        String toolName = (String) input.get("toolName");
        if (toolName == null || toolName.isBlank()) {
            return NodeExecutionResult.failure("Missing or empty required field: toolName");
        }

        log.warn("TOOL_CALL node cannot execute without a configured tool runtime: toolName={}, tenantId={}",
                toolName, tenantId);
        return NodeExecutionResult.failure(
                "TOOL_CALL node execution is not implemented. Configure a tool runtime before enabling TOOL_CALL nodes.");
    }
}
