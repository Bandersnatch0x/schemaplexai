package com.schemaplexai.agent.engine.tool;

/**
 * 工具执行沙箱接口，提供安全的工具执行环境。
 *
 * @deprecated 自 2026-Q2 起停用。OpenAI Agents SDK 2026 模型采用 session-scoped 沙箱
 * （workspace + exec/IO/artifacts/close 生命周期），而本接口为 single-shot 设计。
 * 请改用 {@link com.schemaplexai.agent.engine.tool.sandbox.SandboxProvider} 与
 * {@link com.schemaplexai.agent.engine.tool.sandbox.SandboxSession}。
 * 默认实现 {@link com.schemaplexai.agent.engine.tool.sandbox.provider.LocalProcessSandbox}
 * 已注册为 {@code "localProcessSandbox"} bean。详见 ADR-A2
 * （.claude/changes/agents-sdk-2026-alignment/design.md）。
 */
@Deprecated(since = "2026.04", forRemoval = false)
public interface ToolSandbox {

    /**
     * 在沙箱中执行工具调用
     * @param toolCall 工具调用请求
     * @param config 沙箱配置
     * @return 执行结果
     * @throws ToolExecutionException 执行失败时抛出
     */
    ToolResult execute(ToolCall toolCall, SandboxConfig config) throws ToolExecutionException;

    /**
     * 验证工具调用是否允许执行
     * @param toolCall 工具调用请求
     * @return 验证结果
     */
    ValidationResult validate(ToolCall toolCall);
}
