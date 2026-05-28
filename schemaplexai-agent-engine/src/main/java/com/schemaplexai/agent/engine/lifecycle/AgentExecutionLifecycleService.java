package com.schemaplexai.agent.engine.lifecycle;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import com.schemaplexai.common.constants.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentExecutionLifecycleService {

    private final AgentStateMachine stateMachine;
    private final SfAgentExecutionMapper executionMapper;
    private final StringRedisTemplate redisTemplate;
    private final ExecutionSnapshotPersistence snapshotPersistence;

    public void pauseExecution(Long executionId, PauseReason reason) {
        SfAgentExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("Execution not found: " + executionId);
        }
        String key = String.format(CommonConstants.REDIS_KEY_EXECUTION_PAUSED, executionId);
        redisTemplate.opsForValue().set(key, reason.name(), Duration.ofHours(24));
        stateMachine.transition(AgentExecutionState.PAUSED, execution);
        log.info("Execution {} paused, reason: {}", executionId, reason);
    }

    public void resumeExecution(Long executionId) {
        SfAgentExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("Execution not found: " + executionId);
        }
        String key = String.format(CommonConstants.REDIS_KEY_EXECUTION_PAUSED, executionId);
        redisTemplate.delete(key);
        stateMachine.transition(AgentExecutionState.READY, execution);
        log.info("Execution {} resumed", executionId);
    }

    public void cancelExecution(Long executionId) {
        SfAgentExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("Execution not found: " + executionId);
        }
        String key = String.format(CommonConstants.REDIS_KEY_EXECUTION_PAUSED, executionId);
        redisTemplate.delete(key);
        stateMachine.transition(AgentExecutionState.CANCELLED, execution);
        stateMachine.removeExecution(executionId);
        log.info("Execution {} cancelled", executionId);
    }

    public void saveSnapshot(ExecutionSnapshot snapshot) {
        snapshotPersistence.saveSnapshot(snapshot);
    }

    public ExecutionSnapshot getLatestSnapshot(Long executionId) {
        return snapshotPersistence.getLatestSnapshot(executionId);
    }

}
