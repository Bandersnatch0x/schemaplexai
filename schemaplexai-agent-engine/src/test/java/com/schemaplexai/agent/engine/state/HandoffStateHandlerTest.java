package com.schemaplexai.agent.engine.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.schemaplexai.agent.engine.AgentExecutionEngine;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.entity.SfAgentExecutionSnapshot;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionSnapshotMapper;
import com.schemaplexai.agent.engine.orchestrator.AgentRouter;
import com.schemaplexai.agent.engine.sse.ExecutionEventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandoffStateHandlerTest {

    @Mock
    private SfAgentExecutionMapper executionMapper;

    @Mock
    private SfAgentExecutionSnapshotMapper snapshotMapper;

    @Mock
    private AgentExecutionEngine executionEngine;

    @Mock
    private ExecutionEventBus eventBus;

    private AgentRouter agentRouter;
    private HandoffStateHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        agentRouter = new AgentRouter();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        handler = new HandoffStateHandler(
                executionMapper, snapshotMapper, agentRouter,
                executionEngine, eventBus, objectMapper
        );
    }

    @Test
    void returnsCorrectState() {
        assertThat(handler.getState()).isEqualTo(AgentExecutionState.HANDOFF);
    }

    @Test
    void handoff_dispatchesToSpecialistAgent() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("handoffReason", "Database expertise needed");
        execution.setMetadata("handoffPrompt", "Optimize this SQL query");
        execution.setMetadata("availableAgents", List.of(
                new AgentRouter.AgentCapability("db-agent", "Database specialist",
                        Set.of("sql", "database", "optimize", "query"), 2)
        ));

        AgentStateMachine stateMachine = mock(AgentStateMachine.class);
        SfAgentExecution newExecution = new SfAgentExecution();
        newExecution.setId(999L);
        when(executionEngine.startExecution(anyLong(), anyString(), anyString()))
                .thenReturn(newExecution);
        when(snapshotMapper.insert(any())).thenReturn(1);

        handler.handle(stateMachine, execution);

        // Verify snapshot was saved
        ArgumentCaptor<SfAgentExecutionSnapshot> snapshotCaptor = ArgumentCaptor.forClass(SfAgentExecutionSnapshot.class);
        verify(snapshotMapper).insert(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().getExecutionId()).isEqualTo(execution.getId());

        // Verify specialist agent was dispatched
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(executionEngine).startExecution(anyLong(), eq("tenant-1"), promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("Optimize this SQL query");

        // Verify original execution completed
        verify(stateMachine).saveExecution(execution);
        verify(eventBus).publishExecutionCompleted(execution.getId(), "COMPLETED");
        verify(stateMachine).removeExecution(execution.getId());
    }

    @Test
    void handoff_noPrompt_transitionsToFailed() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("handoffReason", "some reason");
        // No handoffPrompt

        AgentStateMachine stateMachine = mock(AgentStateMachine.class);

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
        verifyNoInteractions(snapshotMapper);
        verifyNoInteractions(executionEngine);
    }

    @Test
    void handoff_noAvailableAgents_completesWithoutDispatch() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("handoffReason", "test");
        execution.setMetadata("handoffPrompt", "Some task");

        AgentStateMachine stateMachine = mock(AgentStateMachine.class);
        when(snapshotMapper.insert(any())).thenReturn(1);

        handler.handle(stateMachine, execution);

        verify(snapshotMapper).insert(any());
        verifyNoInteractions(executionEngine);
        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
    }

    @Test
    void handoff_includesContextInPrompt() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("handoffReason", "test");
        execution.setMetadata("handoffPrompt", "Main task");
        execution.setMetadata("handoffContext", "Previous context data");
        execution.setMetadata("availableAgents", List.of(
                new AgentRouter.AgentCapability("agent-1", "General",
                        Set.of("main", "task"), 2)
        ));

        AgentStateMachine stateMachine = mock(AgentStateMachine.class);
        when(executionEngine.startExecution(anyLong(), anyString(), anyString()))
                .thenReturn(new SfAgentExecution());
        when(snapshotMapper.insert(any())).thenReturn(1);

        handler.handle(stateMachine, execution);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(executionEngine).startExecution(anyLong(), eq("tenant-1"), promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("Main task");
        assertThat(promptCaptor.getValue()).contains("Previous context data");
    }

    @Test
    void handoff_dispatchFailure_recordsError() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("handoffReason", "test");
        execution.setMetadata("handoffPrompt", "Task");
        execution.setMetadata("availableAgents", List.of(
                new AgentRouter.AgentCapability("agent-1", "General",
                        Set.of("task"), 2)
        ));

        AgentStateMachine stateMachine = mock(AgentStateMachine.class);
        when(executionEngine.startExecution(anyLong(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Dispatch failed"));
        when(snapshotMapper.insert(any())).thenReturn(1);

        handler.handle(stateMachine, execution);

        assertThat(execution.getMetadata("handoffError")).isEqualTo("Dispatch failed");
        // Still completes despite error
        verify(eventBus).publishExecutionCompleted(execution.getId(), "COMPLETED");
    }

    @Test
    void handoff_blankReason_defaultsToGeneric() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("handoffReason", "");
        execution.setMetadata("handoffPrompt", "Task");
        execution.setMetadata("availableAgents", List.of(
                new AgentRouter.AgentCapability("agent-1", "General",
                        Set.of("task"), 2)
        ));

        AgentStateMachine stateMachine = mock(AgentStateMachine.class);
        when(executionEngine.startExecution(anyLong(), anyString(), anyString()))
                .thenReturn(new SfAgentExecution());
        when(snapshotMapper.insert(any())).thenReturn(1);

        handler.handle(stateMachine, execution);

        verify(eventBus).publishOutput(eq(execution.getId()), contains("Task outside agent specialty"), anyLong());
    }

    private SfAgentExecution createExecution() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(42L);
        execution.setAgentId(1L);
        execution.setTenantId("tenant-1");
        execution.setConversationId("conv-1");
        execution.setState(AgentExecutionState.THINKING.name());
        return execution;
    }
}
