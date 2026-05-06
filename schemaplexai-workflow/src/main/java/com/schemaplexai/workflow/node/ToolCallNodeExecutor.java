package com.schemaplexai.workflow.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ToolCallNodeExecutor implements NodeExecutor {

    @Override
    public String getNodeType() {
        return "TOOL_CALL";
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeExecutionResult execute(Map<String, Object> input, String tenantId) {
        String toolName = (String) input.get("toolName");
        if (toolName == null || toolName.isBlank()) {
            return NodeExecutionResult.failure("Missing or empty required field: toolName");
        }

        Object toolParameters = input.get("toolParameters");
        Map<String, Object> parameters = (toolParameters instanceof Map)
                ? (Map<String, Object>) toolParameters
                : Map.of();

        Map<String, Object> toolResult = new HashMap<>();
        toolResult.put("tool", toolName);
        toolResult.put("status", "executed");
        toolResult.put("timestamp", Instant.now().toString());
        toolResult.put("parameters", parameters);

        Map<String, Object> output = new HashMap<>();
        output.put("toolResult", toolResult);

        log.info("Tool call node executed: toolName={}, tenantId={}", toolName, tenantId);
        return NodeExecutionResult.success(output);
    }
}
