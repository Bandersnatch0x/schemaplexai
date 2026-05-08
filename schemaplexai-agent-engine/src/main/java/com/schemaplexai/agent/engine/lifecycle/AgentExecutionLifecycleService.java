package com.schemaplexai.agent.engine.lifecycle;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.entity.SfAgentExecutionSnapshot;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionSnapshotMapper;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import com.schemaplexai.common.constants.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.schemaplexai.agent.engine.util.HashUtils;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentExecutionLifecycleService {

    private final AgentStateMachine stateMachine;
    private final SfAgentExecutionMapper executionMapper;
    private final SfAgentExecutionSnapshotMapper snapshotMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

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
        try {
            String snapshotJson = objectMapper.writeValueAsString(snapshot);
            SfAgentExecutionSnapshot entity = new SfAgentExecutionSnapshot();
            entity.setExecutionId(snapshot.getExecutionId());
            entity.setSnapshotJson(snapshotJson);
            entity.setSnapshotHash(HashUtils.sha256(snapshotJson));
            entity.setTenantId(null); // will be filled by interceptor
            snapshotMapper.insert(entity);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize execution snapshot for execution {}", snapshot.getExecutionId(), e);
        }
    }

    public ExecutionSnapshot getLatestSnapshot(Long executionId) {
        SfAgentExecutionSnapshot entity = snapshotMapper.selectOne(
                new LambdaQueryWrapper<SfAgentExecutionSnapshot>()
                        .eq(SfAgentExecutionSnapshot::getExecutionId, executionId)
                        .orderByDesc(SfAgentExecutionSnapshot::getCreatedAt)
                        .last("LIMIT 1")
        );
        if (entity == null || entity.getSnapshotJson() == null) {
            return null;
        }

        String snapshotJson = entity.getSnapshotJson();
        String storedHash = entity.getSnapshotHash();
        if (storedHash != null && !storedHash.isBlank()) {
            String computedHash = HashUtils.sha256(snapshotJson);
            if (!HashUtils.constantTimeEquals(storedHash, computedHash)) {
                log.error("Snapshot hash mismatch for execution {}. Data may have been tampered.", executionId);
                return null;
            }
        } else {
            log.warn("Snapshot hash missing for execution {} (legacy data). Allowing through without integrity check.", executionId);
        }

        try {
            return objectMapper.readValue(snapshotJson, ExecutionSnapshot.class);
        } catch (Exception e) {
            log.error("Failed to deserialize snapshot for execution {}", executionId, e);
            return null;
        }
    }

}
