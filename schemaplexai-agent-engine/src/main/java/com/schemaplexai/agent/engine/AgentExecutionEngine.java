package com.schemaplexai.agent.engine;

import com.schemaplexai.agent.engine.config.AgentExecutionAsyncConfig;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.orchestrator.AgentRuntimeOrchestrator;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecutionEngine {

    private final SfAgentExecutionMapper executionMapper;
    private final AgentRuntimeOrchestrator orchestrator;

    @Async(AgentExecutionAsyncConfig.EXECUTOR_NAME)
    public void runExecutionAsync(SfAgentExecution execution, String tenantId, String prompt) {
        orchestrator.run(execution, tenantId, prompt);
    }

    public SfAgentExecution startExecution(Long agentId, String tenantId, String prompt) {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setAgentId(agentId);
        execution.setConversationId(UUID.randomUUID().toString());
        execution.setState(AgentExecutionState.QUEUED.name());
        execution.setTenantId(tenantId);
        executionMapper.insert(execution);

        runExecutionAsync(execution, tenantId, prompt);
        return execution;
    }
}
