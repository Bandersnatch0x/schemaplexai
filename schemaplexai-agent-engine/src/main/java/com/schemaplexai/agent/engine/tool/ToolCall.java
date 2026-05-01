package com.schemaplexai.agent.engine.tool;

import java.util.Map;

/**
 * 工具调用请求，包含工具名和参数。
 */
public record ToolCall(String toolName, Map<String, Object> parameters) {

    public ToolCall(String toolName) {
        this(toolName, Map.of());
    }
}
