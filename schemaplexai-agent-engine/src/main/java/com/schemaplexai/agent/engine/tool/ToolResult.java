package com.schemaplexai.agent.engine.tool;

/**
 * 工具执行结果。
 */
public record ToolResult(boolean success, String output, String errorMessage) {

    public static ToolResult success(String output) {
        return new ToolResult(true, output, null);
    }

    public static ToolResult error(String errorMessage) {
        return new ToolResult(false, null, errorMessage);
    }

    public boolean isError() {
        return !success;
    }
}
