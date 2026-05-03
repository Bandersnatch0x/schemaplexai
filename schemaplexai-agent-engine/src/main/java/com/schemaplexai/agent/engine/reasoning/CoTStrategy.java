package com.schemaplexai.agent.engine.reasoning;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.context.AgentContext;
import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.tool.ToolCall;

import java.util.Map;

/**
 * Chain-of-Thought reasoning strategy — no tool calls.
 * The LLM produces a final answer through pure step-by-step reasoning.
 * Suitable for analysis, comparison, and explanation tasks.
 */
public class CoTStrategy implements ReasoningStrategy {

    private static final String NAME = "CoT";

    private final LlmProvider llmProvider;
    private final String modelId;
    private final double temperature;

    /**
     * @param llmProvider the LLM provider to use
     * @param modelId     model identifier (e.g. "gpt-4")
     * @param temperature sampling temperature
     */
    public CoTStrategy(LlmProvider llmProvider, String modelId, double temperature) {
        this.llmProvider = llmProvider;
        this.modelId = modelId;
        this.temperature = temperature;
    }

    @Override
    public ThinkingResult think(AgentContext context, TokenBudget budget) {
        if (!budget.hasRemaining()) {
            return ThinkingResult.exhausted("Token budget exhausted before CoT reasoning");
        }

        String prompt = buildCoTPrompt(context);
        long estimatedInput = estimateTokens(prompt);

        if (!budget.consumeInput(estimatedInput)) {
            return ThinkingResult.exhausted("Token budget exceeded on CoT input");
        }

        try {
            String response = llmProvider.generate(prompt, modelId, temperature);
            long estimatedOutput = estimateTokens(response);

            if (!budget.consumeOutput(estimatedOutput)) {
                return ThinkingResult.exhausted("Token budget exceeded on CoT output");
            }

            return ThinkingResult.completed(response);
        } catch (Exception e) {
            return ThinkingResult.error("CoT reasoning failed: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean canContinue(AgentContext context) {
        // CoT is stateless — always can continue while context exists
        return context != null;
    }

    private String buildCoTPrompt(AgentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a reasoning agent. Think step by step to reach a final answer.\n");
        sb.append("Do NOT request tool calls. Produce a complete, self-contained answer.\n");
        sb.append("Tenant: ").append(context.getTenantId()).append("\n");
        sb.append("Project: ").append(context.getProjectId()).append("\n");
        sb.append("User: ").append(context.getUserId()).append("\n");
        return sb.toString();
    }

    private long estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, text.length() / 4L);
    }
}
