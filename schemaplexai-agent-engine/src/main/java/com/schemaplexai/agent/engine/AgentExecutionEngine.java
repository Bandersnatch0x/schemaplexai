package com.schemaplexai.agent.engine;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.orchestrator.AgentRuntimeOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecutionEngine {

    private final SfAgentExecutionMapper executionMapper;
    private final AgentRuntimeOrchestrator orchestrator;

    public SfAgentExecution startExecution(Long agentId, String tenantId, String prompt) {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setAgentId(agentId);
        execution.setConversationId(UUID.randomUUID().toString());
        execution.setState("INITIALIZING");
        execution.setTenantId(tenantId);
        executionMapper.insert(execution);

        // Run asynchronously in production; here synchronous for simplicity
        orchestrator.run(execution, tenantId, prompt);
        return execution;
    }
}
