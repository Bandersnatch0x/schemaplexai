package com.schemaplexai.agent.engine.cost;

/**
 * Execution context bound to the current thread while a state handler runs.
 *
 * <p>The Agent state machine binds this before dispatching a state handler and
 * restores the previous value afterwards, so every LLM call performed on the
 * handler thread (inline reasoning, ReAct strategies, planning, reflection,
 * compaction) can attribute its token usage to the owning execution.
 *
 * <p>Cost collection spec §1 requires every LLM call to be attributable to a
 * tenant / execution; calls made outside a bound context (e.g. AgentLab
 * exploration) are not billed and produce no cost event.
 *
 * @param executionId the agent execution identifier
 * @param tenantId    the tenant identifier as carried on the execution row
 * @param agentId     the agent identifier
 */
public record LlmCallContext(Long executionId, String tenantId, Long agentId) {

    private static final ThreadLocal<LlmCallContext> CURRENT = new ThreadLocal<>();

    /**
     * Binds the given context to the current thread (may be null to clear).
     */
    public static void set(LlmCallContext context) {
        if (context == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(context);
        }
    }

    /**
     * Returns the context bound to the current thread, or null when no
     * execution is active on this thread.
     */
    public static LlmCallContext current() {
        return CURRENT.get();
    }

    /**
     * Removes any context bound to the current thread.
     */
    public static void clear() {
        CURRENT.remove();
    }
}
