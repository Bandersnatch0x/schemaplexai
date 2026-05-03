package com.schemaplexai.agent.engine.tool;

import java.util.List;
import java.util.Map;

/**
 * 工具注册表接口，管理工具定义的注册、查询和注销。
 * 支持导出为 OpenAI 和 Anthropic 的函数/工具格式。
 */
public interface ToolRegistry {

    /**
     * 注册单个工具定义。
     *
     * @param definition 工具定义
     * @throws IllegalArgumentException 如果工具名称为空或已存在
     */
    void register(ToolDefinition definition);

    /**
     * 批量注册工具定义。
     *
     * @param definitions 工具定义列表
     */
    void registerAll(List<ToolDefinition> definitions);

    /**
     * 根据工具名称获取工具定义。
     *
     * @param name 工具名称
     * @return 工具定义，不存在时返回 null
     */
    ToolDefinition get(String name);

    /**
     * 获取所有已注册的工具定义。
     *
     * @return 所有工具定义列表
     */
    List<ToolDefinition> getAll();

    /**
     * 获取所有工具定义的 OpenAI function calling 格式列表。
     *
     * @return OpenAI 函数定义列表
     */
    List<Map<String, Object>> getAllAsOpenAiFunctions();

    /**
     * 获取所有工具定义的 Anthropic tool use 格式列表。
     *
     * @return Anthropic 工具定义列表
     */
    List<Map<String, Object>> getAllAsAnthropicTools();

    /**
     * 检查工具是否已注册。
     *
     * @param name 工具名称
     * @return true 如果工具已注册
     */
    boolean exists(String name);

    /**
     * 注销指定工具。
     *
     * @param name 工具名称
     * @return 被注销的工具定义，不存在时返回 null
     */
    ToolDefinition unregister(String name);
}
