package com.schemaplexai.agent.engine.service;

import com.schemaplexai.agent.engine.entity.ExecutionSnapshot;
import com.schemaplexai.agent.engine.mapper.ExecutionSnapshotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Execution Snapshot Service — M6.1
 * <p>
 * Dual-tier persistence for agent execution state:
 * <ul>
 *   <li>L1 (Redis): Hot recovery with 1-hour TTL</li>
 *   <li>L2 (PostgreSQL): Durable checkpoint</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionSnapshotService {

    private static final Duration REDIS_TTL = Duration.ofHours(1);
    private static final String REDIS_KEY_PREFIX = "execution:snapshot:";

    private final StringRedisTemplate redisTemplate;
    private final ExecutionSnapshotMapper snapshotMapper;

    /**
     * Self-reference through the Spring proxy so {@link Async} on
     * {@link #persistToDatabase} is honored. A direct {@code this.} call
     * would bypass the proxy and persist synchronously on the caller thread
     * (review NEW-02). {@code @Lazy} breaks the self-injection cycle; in
     * plain unit tests without a container this stays null and the call
     * falls back to a direct (synchronous) invocation.
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private ExecutionSnapshotService self;

    /**
     * Save execution snapshot to Redis (synchronous) and PostgreSQL (asynchronous).
     *
     * @param executionId the execution identifier
     * @param stateJson   the serialized execution state
     * @param version     the snapshot version
     */
    public void saveSnapshot(Long executionId, String stateJson, Integer version) {
        String key = buildRedisKey(executionId);
        redisTemplate.opsForValue().set(key, stateJson, REDIS_TTL);
        if (self != null) {
            self.persistToDatabase(executionId, stateJson, version);
        } else {
            persistToDatabase(executionId, stateJson, version);
        }
    }

    /**
     * Restore the latest snapshot for an execution.
     * Prefers Redis (L1); falls back to PostgreSQL (L2) on cache miss.
     *
     * @param executionId the execution identifier
     * @return the latest snapshot JSON, or null if not found in either store
     */
    public String restoreSnapshot(Long executionId) {
        String key = buildRedisKey(executionId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        ExecutionSnapshot latest = snapshotMapper.selectLatestByExecutionId(executionId);
        if (latest != null) {
            return latest.getStateJson();
        }
        return null;
    }

    /**
     * Delete all snapshots for an execution from both Redis and PostgreSQL.
     *
     * @param executionId the execution identifier
     */
    public void deleteSnapshot(Long executionId) {
        String key = buildRedisKey(executionId);
        redisTemplate.delete(key);
        snapshotMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExecutionSnapshot>()
                        .eq(ExecutionSnapshot::getExecutionId, executionId)
        );
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private String buildRedisKey(Long executionId) {
        return REDIS_KEY_PREFIX + executionId;
    }

    @Async
    public void persistToDatabase(Long executionId, String stateJson, Integer version) {
        try {
            ExecutionSnapshot entity = new ExecutionSnapshot();
            entity.setExecutionId(executionId);
            entity.setStateJson(stateJson);
            entity.setVersion(version);
            snapshotMapper.insert(entity);
        } catch (Exception e) {
            log.error("Failed to persist execution snapshot to PG for execution {}", executionId, e);
        }
    }
}
