package com.schemaplexai.agent.engine.lifecycle;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.ExecutionOutboxMapper;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.orchestrator.AgentRuntimeOrchestrator;
import com.schemaplexai.agent.engine.service.ExecutionEventService;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AgentExecutionLifecycleServiceTest {

    @Mock
    private AgentStateMachine stateMachine;

    @Mock
    private SfAgentExecutionMapper executionMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private ExecutionSnapshotPersistence snapshotPersistence;

    @Mock
    private ExecutionEventService executionEventService;

    @Mock
    private ExecutionOutboxMapper executionOutboxMapper;

    @Mock
    private AgentRuntimeOrchestrator orchestrator;

    @InjectMocks
    private AgentExecutionLifecycleService lifecycleService;

    @Test
    void getLatestSnapshotDelegatesToPersistence() {
        ExecutionSnapshot snapshot = ExecutionSnapshot.builder()
                .executionId(1L)
                .state(AgentExecutionState.PAUSED)
                .build();
        when(snapshotPersistence.getLatestSnapshot(1L)).thenReturn(snapshot);

        ExecutionSnapshot result = lifecycleService.getLatestSnapshot(1L);

        assertSame(snapshot, result);
        verify(snapshotPersistence).getLatestSnapshot(1L);
    }

    @Test
    void getLatestSnapshotReturnsNullWhenPersistenceHasNoSnapshot() {
        when(snapshotPersistence.getLatestSnapshot(1L)).thenReturn(null);

        ExecutionSnapshot result = lifecycleService.getLatestSnapshot(1L);

        assertNull(result);
        verify(snapshotPersistence).getLatestSnapshot(1L);
    }

    @Test
    void pauseExecutionSetsRedisAndTransitionsState() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        when(executionMapper.selectById(1L)).thenReturn(execution);

        lifecycleService.pauseExecution(1L, PauseReason.USER_REQUEST);

        verify(valueOps).set(anyString(), eq("USER_REQUEST"), any());
        verify(stateMachine).transition(AgentExecutionState.PAUSED, execution);
    }

    @Test
    void resumeExecutionClearsRedisAndTransitionsToResuming() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        when(executionMapper.selectById(1L)).thenReturn(execution);

        lifecycleService.resumeExecution(1L);

        verify(redisTemplate).delete(anyString());
        verify(stateMachine).transition(AgentExecutionState.RESUMING, execution);
    }

    @Test
    void cancelExecutionRemovesStateAndTransitionsToCancelled() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        when(executionMapper.selectById(1L)).thenReturn(execution);

        lifecycleService.cancelExecution(1L);

        verify(redisTemplate).delete(anyString());
        verify(orchestrator).cancel();
        verify(stateMachine).transition(AgentExecutionState.CANCELLED, execution);
        verify(stateMachine).removeExecution(1L);
    }

    @Test
    void saveSnapshotPersistsSerializedJson() {
        ExecutionSnapshot snapshot = ExecutionSnapshot.builder()
                .executionId(1L)
                .state(AgentExecutionState.THINKING)
                .build();

        lifecycleService.saveSnapshot(snapshot);

        verify(snapshotPersistence).saveSnapshot(snapshot);
    }
}
