package com.schemaplexai.agent.engine;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.orchestrator.AgentRuntimeOrchestrator;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.common.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentExecutionEngineTest {

    @Mock
    private SfAgentExecutionMapper executionMapper;

    @Mock
    private AgentRuntimeOrchestrator orchestrator;

    @Mock
    private ObjectProvider<AgentExecutionRunner> executionRunnerProvider;

    @Mock
    private AgentExecutionRunner asyncRunnerProxy;

    @InjectMocks
    private AgentExecutionEngine executionEngine;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void startExecutionShouldSetStateToQueued() {
        when(executionRunnerProvider.getObject()).thenReturn(asyncRunnerProxy);

        SfAgentExecution result = executionEngine.startExecution(1L, "tenant-1", "test prompt");

        assertEquals(AgentExecutionState.QUEUED.name(), result.getState(),
                "Execution state should be QUEUED on return");
    }

    @Test
    void startExecutionShouldSetAgentId() {
        when(executionRunnerProvider.getObject()).thenReturn(asyncRunnerProxy);

        SfAgentExecution result = executionEngine.startExecution(42L, "tenant-1", "test prompt");

        assertEquals(42L, result.getAgentId(), "Agent ID should be set");
    }

    @Test
    void startExecutionShouldSetTenantId() {
        when(executionRunnerProvider.getObject()).thenReturn(asyncRunnerProxy);

        SfAgentExecution result = executionEngine.startExecution(1L, "tenant-abc", "test prompt");

        assertEquals("tenant-abc", result.getTenantId(), "Tenant ID should be set");
    }

    @Test
    void startExecutionShouldGenerateConversationId() {
        when(executionRunnerProvider.getObject()).thenReturn(asyncRunnerProxy);

        SfAgentExecution result = executionEngine.startExecution(1L, "tenant-1", "test prompt");

        assertNotNull(result.getConversationId(), "Conversation ID should be generated");
        assertFalse(result.getConversationId().isBlank(), "Conversation ID should not be blank");
    }

    @Test
    void startExecutionShouldPersistToDatabase() {
        when(executionRunnerProvider.getObject()).thenReturn(asyncRunnerProxy);

        executionEngine.startExecution(1L, "tenant-1", "test prompt");

        verify(executionMapper, times(1)).insert(any(SfAgentExecution.class));
    }

    @Test
    void startExecutionShouldDispatchThroughAsyncProxyNotInline() {
        // Issue 909 / REQ-01: startExecution must hand the execution to the
        // container-resolved @Async proxy — never run the orchestrator inline on the
        // caller thread (the old this.runExecutionAsync self-invocation bypassed the
        // proxy and blocked HTTP/MQ threads for the whole execution).
        when(executionRunnerProvider.getObject()).thenReturn(asyncRunnerProxy);
        ArgumentCaptor<SfAgentExecution> executionCaptor = ArgumentCaptor.forClass(SfAgentExecution.class);
        ArgumentCaptor<String> tenantCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        executionEngine.startExecution(1L, "tenant-1", "async test prompt");

        verify(asyncRunnerProxy, times(1)).runExecutionAsync(
                executionCaptor.capture(), tenantCaptor.capture(), promptCaptor.capture());
        assertEquals("tenant-1", tenantCaptor.getValue(), "Tenant ID should be passed to the async runner");
        assertEquals("async test prompt", promptCaptor.getValue(), "Prompt should be passed to the async runner");
        assertEquals(AgentExecutionState.QUEUED.name(), executionCaptor.getValue().getState(),
                "Execution handed to the async runner should have QUEUED state");
        verify(orchestrator, never()).run(any(), any(), any());
    }

    @Test
    void runExecutionAsyncShouldDelegateToOrchestratorWithTenantContext() {
        // Issue 909: inside the async worker the tenant context must be established
        // for downstream tenant-scoped work and cleared afterwards (pooled thread).
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(100L);
        execution.setAgentId(1L);
        execution.setState(AgentExecutionState.QUEUED.name());

        AtomicReference<String> tenantDuringRun = new AtomicReference<>();
        doAnswer(invocation -> {
            tenantDuringRun.set(TenantContextHolder.getTenantId());
            return null;
        }).when(orchestrator).run(eq(execution), eq("tenant-1"), eq("direct async prompt"));

        executionEngine.runExecutionAsync(execution, "tenant-1", "direct async prompt");

        verify(orchestrator, times(1)).run(execution, "tenant-1", "direct async prompt");
        assertEquals("tenant-1", tenantDuringRun.get(),
                "Tenant context must be set inside the async execution body");
        assertNull(TenantContextHolder.getTenantId(),
                "Tenant context must be cleared after the async execution completes");
    }

    @Test
    void runExecutionAsyncShouldClearTenantContextEvenWhenOrchestratorFails() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(101L);
        execution.setState(AgentExecutionState.QUEUED.name());
        doThrow(new RuntimeException("engine down")).when(orchestrator).run(any(), any(), any());

        assertThrows(RuntimeException.class,
                () -> executionEngine.runExecutionAsync(execution, "tenant-x", "boom"));
        assertNull(TenantContextHolder.getTenantId(),
                "Tenant context must be cleared even when the execution fails");
    }

    @Test
    void runExecutionAsyncShouldUsePropagatedTenantWhenParameterMissing() {
        // Sub-agent path passes a null tenantId; the TaskDecorator-propagated context
        // (simulated here) must then be used and still cleared afterwards.
        TenantContextHolder.setTenantId("tenant-propagated");
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(102L);
        execution.setState(AgentExecutionState.QUEUED.name());

        AtomicReference<String> tenantDuringRun = new AtomicReference<>();
        doAnswer(invocation -> {
            tenantDuringRun.set(TenantContextHolder.getTenantId());
            return null;
        }).when(orchestrator).run(any(), any(), any());

        executionEngine.runExecutionAsync(execution, null, "sub-agent prompt");

        assertEquals("tenant-propagated", tenantDuringRun.get());
        assertNull(TenantContextHolder.getTenantId());
    }
}
