package com.schemaplexai.agent.engine.state;

public enum AgentExecutionState {
    QUEUED,
    INITIALIZING,
    READY,
    THINKING,
    TOOL_CALLING,
    OBSERVATION,
    PAUSED,
    GATE_BLOCKED,
    RETRYING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
