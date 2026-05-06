package com.schemaplexai.agent.engine.integration;

import com.schemaplexai.agent.engine.AgentExecutionEngine;
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

    @InjectMocks
    private AgentExecutionEngine executionEngine;

    @Test
    @DisplayName("should initialize execution in QUEUED state")
    void shouldInitializeInQueuedState() {
        SfAgentExecution result = executionEngine.startExecution(1L, "tenant-1", "test prompt");

        assertEquals(AgentExecutionState.QUEUED.name(), result.getState());
        assertNotNull(result.getConversationId());
        assertFalse(result.getConversationId().isBlank());
        assertEquals(Long.valueOf(1L), result.getAgentId());
        assertEquals("tenant-1", result.getTenantId());
    }

    @Test
    @DisplayName("should persist execution before triggering orchestrator")
    void shouldPersistBeforeOrchestration() {
        executionEngine.startExecution(2L, "tenant-2", "persist test");

        verify(executionMapper, times(1)).insert(any(SfAgentExecution.class));
    }

    @Test
    @DisplayName("should pass correct arguments to async orchestrator")
    void shouldPassCorrectArgumentsToOrchestrator() {
        ArgumentCaptor<SfAgentExecution> executionCaptor = ArgumentCaptor.forClass(SfAgentExecution.class);
        ArgumentCaptor<String> tenantCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        executionEngine.startExecution(3L, "tenant-3", "async prompt");

        verify(orchestrator, times(1)).run(executionCaptor.capture(), tenantCaptor.capture(), promptCaptor.capture());

        assertEquals("tenant-3", tenantCaptor.getValue());
        assertEquals("async prompt", promptCaptor.getValue());
        assertEquals(AgentExecutionState.QUEUED.name(), executionCaptor.getValue().getState());
        assertEquals(Long.valueOf(3L), executionCaptor.getValue().getAgentId());
    }

    @Test
    @DisplayName("should generate unique conversation IDs per execution")
    void shouldGenerateUniqueConversationIds() {
        SfAgentExecution first = executionEngine.startExecution(1L, "tenant-1", "first");
        SfAgentExecution second = executionEngine.startExecution(1L, "tenant-1", "second");

        assertNotEquals(first.getConversationId(), second.getConversationId());
    }
}
