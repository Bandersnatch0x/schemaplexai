package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.tool.ToolDefinition;

import java.util.List;

public interface LlmProvider {

    String generate(String prompt, String modelId, Double temperature);

    String generateWithMessages(List<LlmMessage> messages, String modelId, Double temperature);

    /**
     * Generate a response with tool definitions injected into the prompt context.
     * Tool descriptions are formatted and appended to the system/user messages so the LLM
     * can produce ReAct-formatted (Thought/Action/Action Input) responses.
     *
     * @param messages   conversation messages
     * @param tools      available tool definitions to describe to the LLM
     * @param modelId    model identifier
     * @param temperature sampling temperature
     * @return LLM response text (may contain ReAct-formatted tool calls or Final Answer)
     */
    String generateWithTools(List<LlmMessage> messages, List<ToolDefinition> tools, String modelId, Double temperature);

    boolean isHealthy();

    String getProviderName();
}
