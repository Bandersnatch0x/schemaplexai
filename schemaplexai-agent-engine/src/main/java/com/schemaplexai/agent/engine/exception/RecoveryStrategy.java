package com.schemaplexai.agent.engine.exception;

import com.schemaplexai.agent.engine.context.AgentContext;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionException;

/**
 * Defines a strategy for recovering from tool execution failures.
 * Each implementation handles a specific set of error categories
 * and decides whether to retry, fail, or fall back to an alternative state.
 */
public interface RecoveryStrategy {

    /**
     * Attempt to recover from a tool execution error.
     *
     * @param error   the tool execution exception with error category
     * @param context the current agent execution context
     * @return a recovery result indicating the next action
     */
    RecoveryResult recover(ToolExecutionException error, AgentContext context);

    /**
     * Whether this strategy supports the given error category.
     */
    boolean supports(ToolErrorCategory category);

    /**
     * Maximum number of retries this strategy will attempt.
     * Returns 0 for strategies that don't retry.
     */
    int getMaxRetries();
}
