package com.schemaplexai.agent.engine.tool.sandbox;

/**
 * 沙箱会话的网络访问策略。
 *
 * <p>注意：在 {@link com.schemaplexai.agent.engine.tool.sandbox.provider.LocalProcessSandbox}
 * 中此策略仅作为软隔离（通过环境变量与文档化约束实现）。强网络隔离需要由后续的
 * E2B / Daytona Provider 通过容器级 net namespace 提供。
 */
public enum NetworkPolicy {

    /** 默认值：禁止任何网络访问 */
    NONE,

    /** 仅允许 loopback (127.0.0.1) */
    LOOPBACK,

    /** 完全开放（仅在受信工作流中使用） */
    OPEN
}
