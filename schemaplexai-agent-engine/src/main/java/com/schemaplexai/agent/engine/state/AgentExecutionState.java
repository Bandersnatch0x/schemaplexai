package com.schemaplexai.agent.engine.state;

public enum AgentExecutionState {
    QUEUED,
    INITIALIZING,
    READY,
    PLANNING,
    THINKING,
    TOOL_CALLING,
    OBSERVATION,
    PAUSED,
    RESUMING,
    GATE_BLOCKED,
    RETRYING,
    REFLECTING,
    HANDOFF,
    GROUP_CHAT,
    COMPLETED,
    FAILED,
    CANCELLED,
    REJECTED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == REJECTED;
    }
}
