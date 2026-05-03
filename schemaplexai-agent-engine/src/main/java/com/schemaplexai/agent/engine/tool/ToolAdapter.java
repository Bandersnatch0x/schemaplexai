package com.schemaplexai.agent.engine.tool;

/**
 * 工具适配器接口，连接工具定义与实际执行逻辑。
 * 每个实现对应一个工具的执行策略。
 */
public interface ToolAdapter {

    /**
     * 执行工具调用并返回结果。
     *
     * @param toolCall 工具调用请求
     * @return 工具执行结果
     */
    ToolResult execute(ToolCall toolCall);

    /**
     * 判断此适配器是否支持处理给定的工具调用。
     *
     * @param toolCall 工具调用请求
     * @return true 表示此适配器可以处理该调用
     */
    boolean supports(ToolCall toolCall);

    /**
     * 返回此适配器对应的工具名称。
     */
    String toolName();
}
