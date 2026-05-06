package com.schemaplexai.agent.engine.tool;

/**
 * 工具执行错误类别，用于分类错误并决定恢复策略。
 */
public enum ToolErrorCategory {

    /**
     * 权限被拒绝（工具不在白名单中）
     */
    PERMISSION_DENIED(true, false),

    /**
     * 无效参数
     */
    INVALID_ARGUMENT(false, false),

    /**
     * 执行超时
     */
    TIMEOUT(false, true),

    /**
     * 内部错误
     */
    INTERNAL_ERROR(false, true),

    /**
     * 速率限制
     */
    RATE_LIMITED(false, true),

    /**
     * 资源不足（内存/CPU）
     */
    RESOURCE_EXHAUSTED(false, true),

    /**
     * 不可逆操作被安全策略阻止
     */
    IRREVERSIBLE_OPERATION(true, false),

    /**
     * 环境不匹配（如dev令牌调用prod工具）
     */
    ENVIRONMENT_MISMATCH(true, false),

    /**
     * 意外环境错误
     */
    UNEXPECTED_ENVIRONMENT(true, false);

    private final boolean securityRelated;
    private final boolean retryable;

    ToolErrorCategory(boolean securityRelated, boolean retryable) {
        this.securityRelated = securityRelated;
        this.retryable = retryable;
    }

    public boolean isSecurityRelated() {
        return securityRelated;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
