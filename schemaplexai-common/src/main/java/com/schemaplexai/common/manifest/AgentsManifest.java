package com.schemaplexai.common.manifest;

import java.util.List;
import java.util.Objects;

/**
 * AGENTS.md 解析后的不可变结构体。
 *
 * <p>对应 OpenAI Agents SDK 2026 的 AGENTS.md 协议（YAML frontmatter + Markdown body）。
 * 字段映射到 SfAgent / SfAgentConfig / SfAgentToolBinding 实体（详见 design.md §3.2）。
 *
 * <p>所有可选字段均允许 {@code null}；列表字段使用空列表（永不为 null）。
 *
 * @param name 必填，agent 唯一标识
 * @param description 可选，agent 描述
 * @param modelId 可选，模型 ID（如 "claude-sonnet-4-6"），映射到 SfAgentConfig.modelId
 * @param type 可选，agent 类型（如 "review"），映射到 SfAgent.type，默认 "general"
 * @param maxRounds 可选，最大执行轮次，映射到 SfAgentConfig.maxRounds（Long）
 * @param maxTools 可选，单轮最大工具数，映射到 SfAgentConfig.maxTools（Long）
 * @param maxInputTokens 可选，输入 token 上限
 * @param maxOutputTokens 可选，输出 token 上限
 * @param temperature 可选，模型温度
 * @param executionMode 可选，执行模式（如 "single"、"batch"）
 * @param tools 可选，工具绑定列表（允许为空但不为 null）
 * @param systemPrompt Markdown body 全文，作为 SfAgentConfig.systemPrompt
 */
public record AgentsManifest(
        String name,
        String description,
        String modelId,
        String type,
        Long maxRounds,
        Long maxTools,
        Long maxInputTokens,
        Long maxOutputTokens,
        Double temperature,
        String executionMode,
        List<ToolBinding> tools,
        String systemPrompt
) {

    public AgentsManifest {
        Objects.requireNonNull(name, "name required");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        tools = tools == null ? List.of() : List.copyOf(tools);
    }

    /**
     * 单个工具绑定。
     *
     * @param name 工具名（如 "file_read"），必填
     * @param type 工具类型（如 "builtin"、"mcp"），必填
     * @param configJson 可选 JSON 配置串
     */
    public record ToolBinding(String name, String type, String configJson) {
        public ToolBinding {
            Objects.requireNonNull(name, "tool name required");
            Objects.requireNonNull(type, "tool type required");
            if (name.isBlank()) {
                throw new IllegalArgumentException("tool name must not be blank");
            }
        }
    }
}
