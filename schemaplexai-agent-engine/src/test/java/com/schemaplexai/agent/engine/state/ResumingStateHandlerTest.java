package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.entity.SfAgentExecutionSnapshot;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionSnapshotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumingStateHandlerTest {

    @Mock
    private SfAgentExecutionSnapshotMapper snapshotMapper;

    @Mock
    private AgentStateMachine stateMachine;

    @InjectMocks
    private ResumingStateHandler handler;

    private SfAgentExecution execution;

    @BeforeEach
    void setUp() {
        execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setAgentId(42L);
    }

    @Test
    void getStateShouldReturnResuming() {
        assertEquals(AgentExecutionState.RESUMING, handler.getState());
    }

    @Test
    void handleShouldTransitionToFailedWhenSnapshotIdIsNull() {
        execution.setSnapshotId(null);

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
        verify(snapshotMapper, never()).selectById(any());
    }

    @Test
    void handleShouldTransitionToFailedWhenSnapshotNotFound() {
        execution.setSnapshotId(100L);
        when(snapshotMapper.selectById(100L)).thenReturn(null);

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void handleShouldTransitionToFailedWhenSnapshotBelongsToDifferentExecution() {
        execution.setSnapshotId(100L);
        SfAgentExecutionSnapshot snapshot = new SfAgentExecutionSnapshot();
        snapshot.setExecutionId(999L);
        snapshot.setSnapshotJson("{\"data\":\"test\"}");
        when(snapshotMapper.selectById(100L)).thenReturn(snapshot);

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void handleShouldTransitionToFailedWhenSnapshotJsonIsNull() {
        execution.setSnapshotId(100L);
        SfAgentExecutionSnapshot snapshot = new SfAgentExecutionSnapshot();
        snapshot.setExecutionId(1L);
        snapshot.setSnapshotJson(null);
        when(snapshotMapper.selectById(100L)).thenReturn(snapshot);

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void handleShouldTransitionToFailedWhenSnapshotJsonIsBlank() {
        execution.setSnapshotId(100L);
        SfAgentExecutionSnapshot snapshot = new SfAgentExecutionSnapshot();
        snapshot.setExecutionId(1L);
        snapshot.setSnapshotJson("   ");
        when(snapshotMapper.selectById(100L)).thenReturn(snapshot);

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void handleShouldRestoreContextAndTransitionToThinking() {
        execution.setSnapshotId(100L);
        SfAgentExecutionSnapshot snapshot = new SfAgentExecutionSnapshot();
        snapshot.setExecutionId(1L);
        snapshot.setSnapshotJson("{\"data\":\"test\"}");
        when(snapshotMapper.selectById(100L)).thenReturn(snapshot);

        handler.handle(stateMachine, execution);

        assertEquals("{\"data\":\"test\"}", execution.getMetadata("restoredContext"));
        assertEquals(AgentExecutionState.RESUMING.name(), execution.getState());
        verify(stateMachine).saveExecution(execution);
        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);
    }

    @Test
    void handleShouldAcceptSnapshotWhenExecutionIdIsNull() {
        execution.setSnapshotId(100L);
        SfAgentExecutionSnapshot snapshot = new SfAgentExecutionSnapshot();
        snapshot.setExecutionId(null);
        snapshot.setSnapshotJson("{\"data\":\"test\"}");
        when(snapshotMapper.selectById(100L)).thenReturn(snapshot);

        handler.handle(stateMachine, execution);

        verify(stateMachine).saveExecution(execution);
        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);
    }
}
