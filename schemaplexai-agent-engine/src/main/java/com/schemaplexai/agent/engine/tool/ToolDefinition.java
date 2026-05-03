package com.schemaplexai.agent.engine.tool;

import java.util.List;
import java.util.Map;

/**
 * 工具定义 — 描述工具的元数据、参数和返回类型。
 * 提供转换为 OpenAI function calling 和 Anthropic tool use 格式的方法。
 */
public record ToolDefinition(
        String name,
        String description,
        List<ToolParameter> parameters,
        String returnType) {

    /**
     * 转换为 OpenAI function calling 的 JSON Schema 格式。
     * <pre>
     * {
     *   "type": "function",
     *   "function": {
     *     "name": "...",
     *     "description": "...",
     *     "parameters": {
     *       "type": "object",
     *       "properties": { ... },
     *       "required": [ ... ]
     *     }
     *   }
     * }
     * </pre>
     */
    public Map<String, Object> toOpenAiFunction() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", buildOpenAiParameters()
                )
        );
    }

    private Map<String, Object> buildOpenAiParameters() {
        Map<String, Object> properties = new java.util.LinkedHashMap<>();
        java.util.List<String> required = new java.util.ArrayList<>();

        for (ToolParameter p : parameters) {
            properties.put(p.name(), Map.of(
                    "type", p.type(),
                    "description", p.description()
            ));
            if (p.required()) {
                required.add(p.name());
            }
        }

        Map<String, Object> schema = new java.util.LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    /**
     * 转换为 Anthropic tool use 格式。
     * <pre>
     * {
     *   "name": "...",
     *   "description": "...",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": { ... },
     *     "required": [ ... ]
     *   }
     * }
     * </pre>
     */
    public Map<String, Object> toAnthropicTool() {
        Map<String, Object> properties = new java.util.LinkedHashMap<>();
        java.util.List<String> required = new java.util.ArrayList<>();

        for (ToolParameter p : parameters) {
            properties.put(p.name(), Map.of(
                    "type", p.type(),
                    "description", p.description()
            ));
            if (p.required()) {
                required.add(p.name());
            }
        }

        Map<String, Object> inputSchema = new java.util.LinkedHashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", properties);
        if (!required.isEmpty()) {
            inputSchema.put("required", required);
        }

        return Map.of(
                "name", name,
                "description", description,
                "input_schema", inputSchema
        );
    }
}
