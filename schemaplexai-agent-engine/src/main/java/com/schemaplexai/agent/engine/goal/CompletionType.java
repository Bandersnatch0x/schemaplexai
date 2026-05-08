package com.schemaplexai.agent.engine.goal;

/**
 * Classification of how an execution completed relative to its goal.
 */
public enum CompletionType {

    /** All milestones achieved and success criteria met. */
    FULLY_ACHIEVED,

    /** Some milestones achieved but goal not fully met. */
    PARTIALLY_ACHIEVED,

    /** Execution stopped because it hit the maximum iteration limit. */
    MAX_ITERATIONS,

    /** Execution stopped due to timeout. */
    TIMEOUT
}
