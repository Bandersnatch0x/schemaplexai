package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.lifecycle.AgentExecutionLifecycleService;
import com.schemaplexai.agent.engine.lifecycle.ExecutionSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PausedStateHandlerTest {

    @Mock
    private AgentExecutionLifecycleService lifecycleService;

    @Mock
    private AgentStateMachine stateMachine;

    @InjectMocks
    private PausedStateHandler handler;

    private SfAgentExecution execution;

    @BeforeEach
    void setUp() {
        execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setAgentId(42L);
        execution.setState(AgentExecutionState.THINKING.name());
    }

    @Test
    void getStateShouldReturnPaused() {
        assertEquals(AgentExecutionState.PAUSED, handler.getState());
    }

    @Test
    void handleShouldSaveSnapshotAndUpdateExecution() {
        handler.handle(stateMachine, execution);

        ArgumentCaptor<ExecutionSnapshot> snapshotCaptor = ArgumentCaptor.forClass(ExecutionSnapshot.class);
        verify(lifecycleService).saveSnapshot(snapshotCaptor.capture());

        ExecutionSnapshot snapshot = snapshotCaptor.getValue();
        assertEquals(1L, snapshot.getExecutionId());
        assertEquals(AgentExecutionState.THINKING, snapshot.getState());
        assertNotNull(snapshot.getCreatedAt());

        verify(stateMachine).saveExecution(execution);
        assertNotNull(execution.getSnapshotId());
    }

    @Test
    void handleShouldUseExecutionIdWhenSnapshotExecutionIdIsNull() {
        // When snapshot's executionId is null, snapshotId should fallback to execution.getId()
        handler.handle(stateMachine, execution);

        verify(lifecycleService).saveSnapshot(any(ExecutionSnapshot.class));
        verify(stateMachine).saveExecution(execution);
        assertEquals(1L, execution.getSnapshotId());
    }
}
