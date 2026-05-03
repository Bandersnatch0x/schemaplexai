package com.schemaplexai.agent.engine.tool;

/**
 * 工具参数定义，描述工具所需的一个参数。
 */
public record ToolParameter(
        String name,
        String type,
        String description,
        boolean required) {
}
