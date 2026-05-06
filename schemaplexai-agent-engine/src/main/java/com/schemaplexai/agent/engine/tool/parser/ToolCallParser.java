package com.schemaplexai.agent.engine.tool.parser;

import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.model.LlmProvider;
import java.util.List;

/**
 * Unified interface for parsing LLM tool call responses.
 * Implementations handle provider-specific formats:
 * - OpenAiToolCallParser: OpenAI JSON tool_calls
 * - AnthropicToolCallParser: Anthropic XML tool_use
 *
 * Parsers are auto-discovered by ToolRegistry and routed by LlmProvider.
 */
public interface ToolCallParser {

    /**
     * Parse tool calls from an LLM response content string.
     *
     * @param content  the raw LLM response content
     * @param provider the LLM provider that generated the response
     * @return list of parsed tool calls, or empty list if none found / parse error
     */
    List<ToolCall> parse(String content, LlmProvider provider);

    /**
     * Returns the provider name this parser supports.
     * Used by ToolRegistry for provider-based routing.
     */
    String getProviderName();
}
