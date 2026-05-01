package com.schemaplexai.agent.engine.tool;

/**
 * 工具执行错误类别，用于分类错误并决定恢复策略。
 */
public enum ToolErrorCategory {

    /**
     * 权限被拒绝（工具不在白名单中）
     */
    PERMISSION_DENIED,

    /**
     * 无效参数
     */
    INVALID_ARGUMENT,

    /**
     * 执行超时
     */
    TIMEOUT,

    /**
     * 内部错误
     */
    INTERNAL_ERROR,

    /**
     * 速率限制
     */
    RATE_LIMITED,

    /**
     * 资源不足（内存/CPU）
     */
    RESOURCE_EXHAUSTED
}
