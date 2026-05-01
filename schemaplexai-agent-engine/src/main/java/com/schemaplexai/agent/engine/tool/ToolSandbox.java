package com.schemaplexai.agent.engine.tool;

/**
 * 工具执行沙箱接口，提供安全的工具执行环境。
 */
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
