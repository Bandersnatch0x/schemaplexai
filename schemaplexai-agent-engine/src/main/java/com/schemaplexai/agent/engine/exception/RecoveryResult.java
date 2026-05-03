package com.schemaplexai.agent.engine.exception;

import com.schemaplexai.agent.engine.state.AgentExecutionState;

/**
 * Result of an exception recovery attempt by a RecoveryStrategy.
 * Determines the next action the state machine should take.
 */
public record RecoveryResult(
    Type type,
    String message,
    AgentExecutionState nextState
) {

    public enum Type {
        /** Retry the failed operation. */
        RETRY,
        /** The operation failed irrecoverably. */
        FAILED,
        /** Fall back to an alternative path/state. */
        FALLBACK
    }

    public static RecoveryResult retry(String msg) {
        return new RecoveryResult(Type.RETRY, msg, null);
    }

    public static RecoveryResult failed(String msg) {
        return new RecoveryResult(Type.FAILED, msg, null);
    }

    public static RecoveryResult fallback(String msg, AgentExecutionState state) {
        return new RecoveryResult(Type.FALLBACK, msg, state);
    }
}
