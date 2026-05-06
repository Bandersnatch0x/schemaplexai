package com.schemaplexai.agent.engine.tool.sandbox;

import com.schemaplexai.agent.engine.tool.ToolErrorCategory;

import java.util.Objects;

/**
 * 沙箱执行过程中抛出的统一异常类型。
 *
 * <p>携带 {@link ToolErrorCategory}，便于 ToolAdapter 层根据类别决定恢复策略
 * （重试、报错、调用方降级）。
 */
public class SandboxException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ToolErrorCategory category;

    public SandboxException(String message, ToolErrorCategory category) {
        super(message);
        this.category = Objects.requireNonNull(category, "category required");
    }

    public SandboxException(String message, Throwable cause, ToolErrorCategory category) {
        super(message, cause);
        this.category = Objects.requireNonNull(category, "category required");
    }

    public ToolErrorCategory getCategory() {
        return category;
    }
}
