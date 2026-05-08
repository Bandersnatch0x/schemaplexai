package com.schemaplexai.agent.engine.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentExecutionStateTest {

    @Test
    void isTerminalReturnsTrueForCompleted() {
        assertTrue(AgentExecutionState.COMPLETED.isTerminal());
    }

    @Test
    void isTerminalReturnsTrueForFailed() {
        assertTrue(AgentExecutionState.FAILED.isTerminal());
    }

    @Test
    void isTerminalReturnsTrueForCancelled() {
        assertTrue(AgentExecutionState.CANCELLED.isTerminal());
    }

    @Test
    void isTerminalReturnsFalseForNonTerminalStates() {
        assertFalse(AgentExecutionState.QUEUED.isTerminal());
        assertFalse(AgentExecutionState.INITIALIZING.isTerminal());
        assertFalse(AgentExecutionState.READY.isTerminal());
        assertFalse(AgentExecutionState.THINKING.isTerminal());
        assertFalse(AgentExecutionState.TOOL_CALLING.isTerminal());
        assertFalse(AgentExecutionState.OBSERVATION.isTerminal());
        assertFalse(AgentExecutionState.PAUSED.isTerminal());
        assertFalse(AgentExecutionState.RESUMING.isTerminal());
        assertFalse(AgentExecutionState.GATE_BLOCKED.isTerminal());
        assertFalse(AgentExecutionState.RETRYING.isTerminal());
        assertFalse(AgentExecutionState.REFLECTING.isTerminal());
        assertFalse(AgentExecutionState.HANDOFF.isTerminal());
    }
}
