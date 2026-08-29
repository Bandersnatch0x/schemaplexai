package com.schemaplexai.agent.engine;

import com.schemaplexai.agent.engine.config.AgentExecutionAsyncConfig;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.orchestrator.AgentRuntimeOrchestrator;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.common.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecutionEngine implements AgentExecutionStarter, AgentExecutionRunner {

    private final SfAgentExecutionMapper executionMapper;
    private final AgentRuntimeOrchestrator orchestrator;
    /**
     * Self-reference resolved lazily through the container (issue 909 / REQ-01).
     * {@link ObjectProvider#getObject()} returns the Spring-managed proxy of this
     * bean, so invoking {@link AgentExecutionRunner#runExecutionAsync} on it routes
     * through the AOP proxy and {@code @Async} is actually applied. A bare
     * {@code this.runExecutionAsync(...)} call bypasses the proxy and runs inline.
     */
    private final ObjectProvider<AgentExecutionRunner> executionRunnerProvider;

    @Async(AgentExecutionAsyncConfig.EXECUTOR_NAME)
    @Override
    public void runExecutionAsync(SfAgentExecution execution, String tenantId, String prompt) {
        // Establish the tenant context inside the async worker thread (issue 909).
        // Prefer the execution's own tenant; fall back to whatever the TaskDecorator
        // propagated from the submitting thread. The worker thread is pooled, so the
        // context MUST be cleared afterwards to avoid leaking into the next task.
        String effectiveTenant = tenantId != null ? tenantId : TenantContextHolder.getTenantId();
        if (effectiveTenant != null) {
            TenantContextHolder.setTenantId(effectiveTenant);
        }
        try {
            orchestrator.run(execution, tenantId, prompt);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public SfAgentExecution startExecution(Long agentId, String tenantId, String prompt) {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setAgentId(agentId);
        execution.setConversationId(UUID.randomUUID().toString());
        execution.setState(AgentExecutionState.QUEUED.name());
        execution.setTenantId(tenantId);
        executionMapper.insert(execution);

        // Issue 909 / REQ-01: dispatch through the container-resolved proxy so the
        // call is genuinely asynchronous. The previous this.runExecutionAsync(...)
        // self-invocation bypassed the @Async proxy and blocked the caller (HTTP/MQ)
        // thread for the entire execution.
        executionRunnerProvider.getObject().runExecutionAsync(execution, tenantId, prompt);
        return execution;
    }
}
