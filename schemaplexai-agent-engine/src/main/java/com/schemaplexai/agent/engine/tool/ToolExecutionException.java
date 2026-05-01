package com.schemaplexai.agent.engine.tool;

/**
 * 工具执行异常，携带错误类别信息。
 */
public class ToolExecutionException extends Exception {

    private final ToolErrorCategory errorCategory;

    public ToolExecutionException(ToolErrorCategory errorCategory, String message) {
        super(message);
        this.errorCategory = errorCategory;
    }

    public ToolExecutionException(ToolErrorCategory errorCategory, String message, Throwable cause) {
        super(message, cause);
        this.errorCategory = errorCategory;
    }

    public ToolErrorCategory getErrorCategory() {
        return errorCategory;
    }
}
