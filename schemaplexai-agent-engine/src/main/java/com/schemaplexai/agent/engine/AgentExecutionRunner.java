package com.schemaplexai.agent.engine;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;

public interface AgentExecutionRunner {

    void runExecutionAsync(SfAgentExecution execution, String tenantId, String prompt);
}
