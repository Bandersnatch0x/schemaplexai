package com.schemaplexai.agent.engine.lifecycle;

import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.entity.ExecutionOutbox;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.ExecutionOutboxMapper;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.orchestrator.AgentRuntimeOrchestrator;
import com.schemaplexai.agent.engine.service.ExecutionEventService;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSession;
import com.schemaplexai.common.constants.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentExecutionLifecycleService {

    private final AgentStateMachine stateMachine;
    private final SfAgentExecutionMapper executionMapper;
    private final StringRedisTemplate redisTemplate;
    private final ExecutionSnapshotPersistence snapshotPersistence;
    private final ExecutionEventService executionEventService;
    private final ExecutionOutboxMapper executionOutboxMapper;
    private final AgentRuntimeOrchestrator orchestrator;

    /** Tracks active sandbox sessions keyed by execution ID for cleanup on cancel. */
    private final Map<Long, SandboxSession> activeSandboxSessions = new ConcurrentHashMap<>();

    /**
     * Register a sandbox session for lifecycle management (cleanup on cancel).
     */
    public void registerSandboxSession(Long executionId, SandboxSession session) {
        if (session != null) {
            activeSandboxSessions.put(executionId, session);
            log.debug("Registered sandbox session {} for execution {}", session.sessionId(), executionId);
        }
    }

    /**
     * Unregister a sandbox session (called when session closes naturally).
     */
    public void unregisterSandboxSession(Long executionId) {
        activeSandboxSessions.remove(executionId);
    }

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
        // Fix: PAUSED -> RESUMING (not READY) so ResumingStateHandler loads snapshot and restores context
        stateMachine.transition(AgentExecutionState.RESUMING, execution);
        log.info("Execution {} resumed, transitioning from PAUSED to RESUMING", executionId);
    }

    public void cancelExecution(Long executionId) {
        SfAgentExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("Execution not found: " + executionId);
        }
        String key = String.format(CommonConstants.REDIS_KEY_EXECUTION_PAUSED, executionId);
        redisTemplate.delete(key);

        // Signal the orchestrator to stop its loop
        orchestrator.cancel();

        // Clean up sandbox session if present
        SandboxSession session = activeSandboxSessions.remove(executionId);
        if (session != null) {
            try {
                session.close();
                log.info("Closed sandbox session {} for cancelled execution {}", session.sessionId(), executionId);
            } catch (Exception e) {
                log.warn("Error closing sandbox session for execution {}: {}", executionId, e.getMessage());
            }
        }

        // Publish cancellation event to outbox for downstream consumers
        publishCancellationEvent(executionId, execution.getTenantId());

        stateMachine.transition(AgentExecutionState.CANCELLED, execution);
        stateMachine.removeExecution(executionId);
        log.info("Execution {} cancelled", executionId);
    }

    private void publishCancellationEvent(Long executionId, String tenantId) {
        try {
            ExecutionEvent event = new ExecutionEvent();
            event.setEventId(UUID.randomUUID());
            event.setExecutionId(executionId);
            event.setSeq(0);
            event.setEventType("EXECUTION_CANCELLED");
            event.setPayload("{\"executionId\":" + executionId + ",\"reason\":\"user_request\"}");
            event.setOccurredAt(Instant.now());
            event.setTenantId(tenantId != null ? Long.valueOf(tenantId) : null);

            ExecutionOutbox outbox = new ExecutionOutbox();
            outbox.setEventId(event.getEventId());
            outbox.setExecutionId(executionId);
            outbox.setSeq(0);
            outbox.setTopic("execution.events");
            outbox.setPayload(event.getPayload());
            outbox.setCreatedAt(Instant.now());
            outbox.setRetryCount(0);
            executionOutboxMapper.insert(outbox);

            log.debug("Published cancellation event to outbox for execution {}", executionId);
        } catch (Exception e) {
            log.warn("Failed to publish cancellation event for execution {}: {}", executionId, e.getMessage());
        }
    }

    public void saveSnapshot(ExecutionSnapshot snapshot) {
        snapshotPersistence.saveSnapshot(snapshot);
    }

    public ExecutionSnapshot getLatestSnapshot(Long executionId) {
        return snapshotPersistence.getLatestSnapshot(executionId);
    }

}
