package com.schemaplexai.agent.engine.tool;

public record ToolExecutionResult(
    String toolName,
    boolean success,
    boolean blocked,
    String output,
    ToolErrorCategory errorCategory,
    String errorMessage,
    long latencyMs,
    int tokenCount
) {

    public static ToolExecutionResult success(String toolName, String output, long latencyMs, int tokenCount) {
        return new ToolExecutionResult(toolName, true, false, output, null, null, latencyMs, tokenCount);
    }

    public static ToolExecutionResult failure(String toolName, ToolErrorCategory category,
                                               String errorMessage, long latencyMs, int tokenCount) {
        return new ToolExecutionResult(toolName, false, false, null, category, errorMessage, latencyMs, tokenCount);
    }

    public static ToolExecutionResult blocked(String toolName, ToolErrorCategory category, String errorMessage) {
        return new ToolExecutionResult(toolName, false, true, null, category, errorMessage, 0, 0);
    }
}
