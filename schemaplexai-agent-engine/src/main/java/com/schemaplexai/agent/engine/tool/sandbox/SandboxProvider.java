package com.schemaplexai.agent.engine.tool.sandbox;

/**
 * 沙箱会话工厂。
 *
 * <p>不同 Provider 实现（本地进程、容器、E2B、Daytona 等）以同一接口暴露给
 * ToolAdapter 与 Orchestrator，便于在不动调用方代码的前提下切换底层执行环境。
 *
 * @see SandboxSession
 * @see com.schemaplexai.agent.engine.tool.sandbox.provider.LocalProcessSandbox
 */
public interface SandboxProvider {

    /**
     * 创建并打开一个新的沙箱会话。
     *
     * @param config 会话配置
     * @return 已打开的 {@link SandboxSession}，调用方负责 {@link SandboxSession#close()}
     * @throws SandboxException 创建失败（资源不足、配置错误、底层 IO 异常等）
     */
    SandboxSession create(SandboxSessionConfig config) throws SandboxException;

    /**
     * Provider 标识，例如 {@code local} / {@code e2b} / {@code daytona}。
     */
    String providerId();
}
