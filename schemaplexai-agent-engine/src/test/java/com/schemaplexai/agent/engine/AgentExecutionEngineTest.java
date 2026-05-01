package com.schemaplexai.agent.engine;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.orchestrator.AgentRuntimeOrchestrator;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentExecutionEngineTest {

    @Mock
    private SfAgentExecutionMapper executionMapper;

    @Mock
    private AgentRuntimeOrchestrator orchestrator;

    @InjectMocks
    private AgentExecutionEngine executionEngine;

    @Test
    void startExecutionShouldSetStateToQueued() {
        SfAgentExecution result = executionEngine.startExecution(1L, "tenant-1", "test prompt");

        assertEquals(AgentExecutionState.QUEUED.name(), result.getState(),
                "Execution state should be QUEUED on return");
    }

    @Test
    void startExecutionShouldSetAgentId() {
        SfAgentExecution result = executionEngine.startExecution(42L, "tenant-1", "test prompt");

        assertEquals(42L, result.getAgentId(), "Agent ID should be set");
    }

    @Test
    void startExecutionShouldSetTenantId() {
        SfAgentExecution result = executionEngine.startExecution(1L, "tenant-abc", "test prompt");

        assertEquals("tenant-abc", result.getTenantId(), "Tenant ID should be set");
    }

    @Test
    void startExecutionShouldGenerateConversationId() {
        SfAgentExecution result = executionEngine.startExecution(1L, "tenant-1", "test prompt");

        assertNotNull(result.getConversationId(), "Conversation ID should be generated");
        assertFalse(result.getConversationId().isBlank(), "Conversation ID should not be blank");
    }

    @Test
    void startExecutionShouldPersistToDatabase() {
        executionEngine.startExecution(1L, "tenant-1", "test prompt");

        verify(executionMapper, times(1)).insert(any(SfAgentExecution.class));
    }

    @Test
    void startExecutionShouldTriggerAsyncOrchestrator() {
        ArgumentCaptor<SfAgentExecution> executionCaptor = ArgumentCaptor.forClass(SfAgentExecution.class);
        ArgumentCaptor<String> tenantCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        executionEngine.startExecution(1L, "tenant-1", "async test prompt");

        verify(orchestrator, times(1)).run(executionCaptor.capture(), tenantCaptor.capture(), promptCaptor.capture());
        assertEquals("tenant-1", tenantCaptor.getValue(), "Tenant ID should be passed to orchestrator");
        assertEquals("async test prompt", promptCaptor.getValue(), "Prompt should be passed to orchestrator");
        assertEquals(AgentExecutionState.QUEUED.name(), executionCaptor.getValue().getState(),
                "Execution passed to orchestrator should have QUEUED state");
    }

    @Test
    void runExecutionAsyncShouldDelegateToOrchestrator() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(100L);
        execution.setAgentId(1L);
        execution.setState(AgentExecutionState.QUEUED.name());

        executionEngine.runExecutionAsync(execution, "tenant-1", "direct async prompt");

        verify(orchestrator, times(1)).run(execution, "tenant-1", "direct async prompt");
    }
}
