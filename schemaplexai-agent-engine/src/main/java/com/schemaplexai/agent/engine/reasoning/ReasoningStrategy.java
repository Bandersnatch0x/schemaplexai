package com.schemaplexai.agent.engine.reasoning;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.context.AgentContext;

/**
 * Defines a reasoning strategy used during the THINKING state.
 * Each strategy decides how the LLM processes context and produces
 * either a final answer or a tool call.
 */
public interface ReasoningStrategy {

    /**
     * Execute a reasoning step given execution context and token budget.
     *
     * @param context current agent execution context
     * @param budget  token budget tracker for this execution
     * @return the result of reasoning (completed, tool call, exhausted, or error)
     */
    ThinkingResult think(AgentContext context, TokenBudget budget);

    /**
     * Human-readable name of this strategy (e.g. "ReAct", "CoT").
     */
    String getName();

    /**
     * Whether the strategy can continue given the current execution context.
     * May return false when max iterations are reached or context is too large.
     */
    boolean canContinue(AgentContext context);
}
