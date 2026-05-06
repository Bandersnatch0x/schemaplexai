package com.schemaplexai.agent.engine.reasoning;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.context.AgentContext;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.registry.ToolRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ReAct (Reasoning + Acting) strategy — interleaves reasoning with tool calls.
 * The LLM produces Thought → Action → Observation cycles until a final answer emerges.
 */
public class ReActStrategy implements ReasoningStrategy {

    private static final String NAME = "ReAct";
    private static final int DEFAULT_MAX_ITERATIONS = 10;

    private final LlmProvider llmProvider;
    private final ToolRegistry toolRegistry;
    private final String modelId;
    private final double temperature;
    private final int maxIterations;

    /**
     * @param llmProvider   the LLM provider for reasoning
     * @param toolRegistry  registry of available tools
     * @param modelId       model identifier (e.g. "gpt-4")
     * @param temperature   sampling temperature
     * @param maxIterations maximum ReAct loop iterations
     */
    public ReActStrategy(LlmProvider llmProvider, ToolRegistry toolRegistry,
                         String modelId, double temperature, int maxIterations) {
        this.llmProvider = llmProvider;
        this.toolRegistry = toolRegistry;
        this.modelId = modelId;
        this.temperature = temperature;
        this.maxIterations = maxIterations;
    }

    /**
     * Convenience constructor with default max iterations.
     */
    public ReActStrategy(LlmProvider llmProvider, ToolRegistry toolRegistry,
                         String modelId, double temperature) {
        this(llmProvider, toolRegistry, modelId, temperature, DEFAULT_MAX_ITERATIONS);
    }

    @Override
    public ThinkingResult think(AgentContext context, TokenBudget budget) {
        if (!budget.hasRemaining()) {
            return ThinkingResult.exhausted("Token budget exhausted before ReAct reasoning");
        }

        List<LlmMessage> messages = new ArrayList<>();

        // System-level instruction for ReAct format
        String systemPrompt = buildReActSystemPrompt();
        long systemTokens = estimateTokens(systemPrompt);
        if (!budget.consumeInput(systemTokens)) {
            return ThinkingResult.exhausted("Token budget exceeded on ReAct system prompt");
        }
        messages.add(new LlmMessage("system", systemPrompt));

        // User context as initial message
        String userContext = buildUserContext(context);
        long userTokens = estimateTokens(userContext);
        if (!budget.consumeInput(userTokens)) {
            return ThinkingResult.exhausted("Token budget exceeded on ReAct user context");
        }
        messages.add(new LlmMessage("user", userContext));

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            try {
                String response = llmProvider.generateWithMessages(messages, modelId, temperature);
                long outputTokens = estimateTokens(response);
                if (!budget.consumeOutput(outputTokens)) {
                    return ThinkingResult.exhausted(
                            "Token budget exceeded during ReAct iteration " + (iteration + 1));
                }

                messages.add(new LlmMessage("assistant", response));

                // Parse response for tool calls
                ToolCall toolCall = parseToolCall(response);
                if (toolCall != null) {
                    // Attempt tool execution
                    try {
                        String observation = executeTool(toolCall);
                        long obsTokens = estimateTokens(observation);
                        if (!budget.consumeOutput(obsTokens)) {
                            return ThinkingResult.exhausted(
                                    "Token budget exceeded on ReAct observation at iteration " + (iteration + 1));
                        }
                        messages.add(new LlmMessage("system",
                                "Observation: " + (observation != null ? observation : "(empty)")));
                    } catch (Exception e) {
                        messages.add(new LlmMessage("system",
                                "Error executing tool " + toolCall.toolName() + ": " + e.getMessage()));
                    }
                } else if (isFinalAnswer(response)) {
                    return ThinkingResult.completed(extractFinalAnswer(response));
                } else {
                    // No tool call and no final answer — treat as intermediate thought,
                    // let the loop continue
                }
            } catch (Exception e) {
                return ThinkingResult.error("ReAct reasoning failed at iteration "
                        + (iteration + 1) + ": " + e.getMessage());
            }
        }

        return ThinkingResult.exhausted("ReAct max iterations (" + maxIterations + ") exceeded");
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean canContinue(AgentContext context) {
        return context != null && toolRegistry != null;
    }

    private String buildReActSystemPrompt() {
        return """
               You are a reasoning agent that follows the ReAct (Reasoning + Acting) framework.
               For each step, you may:
               1. Think about the problem and produce a thought.
               2. Call a tool using the format: TOOL_CALL: <toolName> {"key": "value"}
               3. Provide a final answer using the format: FINAL_ANSWER: <your answer>

               Available tools: """ + toolRegistry.getRegisteredToolNames() + "\n\n"
               + "Always reason step by step. Only produce FINAL_ANSWER when you are confident.";
    }

    private String buildUserContext(AgentContext context) {
        return "Tenant: " + context.getTenantId() + "\n"
                + "Project: " + context.getProjectId() + "\n"
                + "User: " + context.getUserId() + "\n"
                + "Please process the request.";
    }

    /**
     * Attempts to parse a tool call from the LLM response.
     * Supports the format: TOOL_CALL: <toolName> {"key": "value"}
     */
    private ToolCall parseToolCall(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }
        String text = response.trim();

        // Check for structured TOOL_CALL marker
        if (text.contains("TOOL_CALL:")) {
            int startIdx = text.indexOf("TOOL_CALL:") + "TOOL_CALL:".length();
            String afterMarker = text.substring(startIdx).trim();
            int spaceIdx = afterMarker.indexOf(' ');

            String toolName;
            Map<String, Object> parameters;
            if (spaceIdx > 0) {
                toolName = afterMarker.substring(0, spaceIdx).trim();
                String paramsStr = afterMarker.substring(spaceIdx + 1).trim();
                parameters = parseJsonParams(paramsStr);
            } else {
                toolName = afterMarker.trim();
                parameters = Map.of();
            }
            return new ToolCall(toolName, parameters);
        }

        // Check for legacy markers
        if (text.contains("<tool>")) {
            int startIdx = text.indexOf("<tool>") + "<tool>".length();
            int endIdx = text.indexOf("</tool>");
            if (endIdx > startIdx) {
                String toolName = text.substring(startIdx, endIdx).trim();
                return new ToolCall(toolName, Map.of());
            }
        }

        return null;
    }

    /**
     * Simple JSON parameter parser for inline tool call params.
     * Handles the form: {"key": "value", "key2": "value2"}
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonParams(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) {
            return Map.of();
        }
        jsonStr = jsonStr.trim();
        if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
            String inner = jsonStr.substring(1, jsonStr.length() - 1).trim();
            if (inner.isEmpty()) {
                return Map.of();
            }
            Map<String, Object> params = new java.util.LinkedHashMap<>();
            // Parse "key": "value" pairs separated by commas
            int pos = 0;
            while (pos < inner.length()) {
                // Skip whitespace and commas
                while (pos < inner.length() && (inner.charAt(pos) == ' ' || inner.charAt(pos) == ',')) {
                    pos++;
                }
                if (pos >= inner.length()) break;

                // Read key (must be quoted)
                if (inner.charAt(pos) != '"') break;
                int keyEnd = inner.indexOf('"', pos + 1);
                if (keyEnd < 0) break;
                String key = inner.substring(pos + 1, keyEnd);
                pos = keyEnd + 1;

                // Skip colon and whitespace
                while (pos < inner.length() && (inner.charAt(pos) == ' ' || inner.charAt(pos) == ':')) {
                    pos++;
                }
                if (pos >= inner.length()) break;

                // Read value (quoted string or unquoted)
                String value;
                if (inner.charAt(pos) == '"') {
                    int valEnd = inner.indexOf('"', pos + 1);
                    if (valEnd < 0) break;
                    value = inner.substring(pos + 1, valEnd);
                    pos = valEnd + 1;
                } else {
                    int valEnd = pos;
                    while (valEnd < inner.length() && inner.charAt(valEnd) != ',' && inner.charAt(valEnd) != '}') {
                        valEnd++;
                    }
                    value = inner.substring(pos, valEnd).trim();
                    pos = valEnd;
                }
                params.put(key, value);
            }
            return params;
        }
        return Map.of();
    }

    private boolean isFinalAnswer(String response) {
        if (response == null || response.isBlank()) {
            return false;
        }
        return response.contains("FINAL_ANSWER:")
                || response.contains("Final Answer:")
                || response.contains("final_answer:");
    }

    private String extractFinalAnswer(String response) {
        String[] markers = {"FINAL_ANSWER:", "Final Answer:", "final_answer:"};
        for (String marker : markers) {
            if (response.contains(marker)) {
                int idx = response.indexOf(marker) + marker.length();
                return response.substring(idx).trim();
            }
        }
        return response.trim();
    }

    private String executeTool(ToolCall toolCall) throws Exception {
        if (toolRegistry == null) {
            return "No tool registry available";
        }
        // Tool resolution only — actual execution happens in ToolCallingStateHandler
        var adapter = toolRegistry.resolve(toolCall.toolName());
        if (adapter == null) {
            return "Tool not found: " + toolCall.toolName();
        }
        return "Tool '" + toolCall.toolName() + "' resolved (execution delegated to ToolCallingStateHandler)";
    }

    private long estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, text.length() / 4L);
    }
}
