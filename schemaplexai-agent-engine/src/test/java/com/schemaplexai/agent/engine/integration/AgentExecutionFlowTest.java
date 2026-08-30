package com.schemaplexai.agent.engine.integration;

import com.schemaplexai.agent.engine.AgentExecutionEngine;
import com.schemaplexai.agent.engine.AgentExecutionRunner;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.orchestrator.AgentRuntimeOrchestrator;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Agent Execution Flow Integration")
class AgentExecutionFlowTest {

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

    @Test
    @DisplayName("should initialize execution in QUEUED state")
    void shouldInitializeInQueuedState() {
        when(executionRunnerProvider.getObject()).thenReturn(asyncRunnerProxy);

        SfAgentExecution result = executionEngine.startExecution(1L, "tenant-1", "test prompt");

        assertEquals(AgentExecutionState.QUEUED.name(), result.getState());
        assertNotNull(result.getConversationId());
        assertFalse(result.getConversationId().isBlank());
        assertEquals(Long.valueOf(1L), result.getAgentId());
        assertEquals("tenant-1", result.getTenantId());
    }

    @Test
    @DisplayName("should persist execution before dispatching async run")
    void shouldPersistBeforeOrchestration() {
        when(executionRunnerProvider.getObject()).thenReturn(asyncRunnerProxy);

        executionEngine.startExecution(2L, "tenant-2", "persist test");

        verify(executionMapper, times(1)).insert(any(SfAgentExecution.class));
    }

    @Test
    @DisplayName("should pass correct arguments to the async runner proxy")
    void shouldPassCorrectArgumentsToAsyncRunner() {
        // Issue 909 / REQ-01: startExecution dispatches via the container-resolved
        // @Async proxy, never driving the orchestrator inline on the caller thread.
        when(executionRunnerProvider.getObject()).thenReturn(asyncRunnerProxy);
        ArgumentCaptor<SfAgentExecution> executionCaptor = ArgumentCaptor.forClass(SfAgentExecution.class);
        ArgumentCaptor<String> tenantCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        executionEngine.startExecution(3L, "tenant-3", "async prompt");

        verify(asyncRunnerProxy, times(1)).runExecutionAsync(
                executionCaptor.capture(), tenantCaptor.capture(), promptCaptor.capture());

        assertEquals("tenant-3", tenantCaptor.getValue());
        assertEquals("async prompt", promptCaptor.getValue());
        assertEquals(AgentExecutionState.QUEUED.name(), executionCaptor.getValue().getState());
        assertEquals(Long.valueOf(3L), executionCaptor.getValue().getAgentId());
        verify(orchestrator, never()).run(any(), any(), any());
    }

    @Test
    @DisplayName("should generate unique conversation IDs per execution")
    void shouldGenerateUniqueConversationIds() {
        when(executionRunnerProvider.getObject()).thenReturn(asyncRunnerProxy);

        SfAgentExecution first = executionEngine.startExecution(1L, "tenant-1", "first");
        SfAgentExecution second = executionEngine.startExecution(1L, "tenant-1", "second");

        assertNotEquals(first.getConversationId(), second.getConversationId());
    }
}
