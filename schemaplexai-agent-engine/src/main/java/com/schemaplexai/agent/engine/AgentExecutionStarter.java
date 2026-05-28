package com.schemaplexai.agent.engine;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;

public interface AgentExecutionStarter {

    SfAgentExecution startExecution(Long agentId, String tenantId, String prompt);
}
