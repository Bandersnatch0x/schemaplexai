package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;

public interface AgentStateHandler {

    AgentExecutionState getState();

    void handle(AgentStateMachine stateMachine, SfAgentExecution execution);
}
