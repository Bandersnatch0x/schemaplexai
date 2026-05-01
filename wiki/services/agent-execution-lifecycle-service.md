---
title: AgentExecutionLifecycleService
type: service
source: schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/lifecycle/AgentExecutionLifecycleService.java
creation_date: 2026-05-01
update_date: 2026-05-01
tags: [service, agent, lifecycle, pause, resume, cancel, snapshot]
confidence: high
---

# AgentExecutionLifecycleService

> One-sentence summary: Manages agent execution lifecycle operations — pause, resume, cancel, and snapshot persistence — backed by Redis state and MyBatis-Plus persistence.

## Responsibilities

1. **Pause execution** — Write pause reason to Redis, transition state to `PAUSED`
2. **Resume execution** — Clear Redis pause key, transition state to `READY`
3. **Cancel execution** — Clear Redis pause key, transition to `CANCELLED`, remove from state machine
4. **Save snapshot** — Serialize execution state to JSON, persist to `sf_agent_execution_snapshot`
5. **Get latest snapshot** — Retrieve most recent snapshot (stub implementation)

## Key Code

### Pause
```java
public void pauseExecution(Long executionId, PauseReason reason) {
    SfAgentExecution execution = executionMapper.selectById(executionId);
    String key = String.format(REDIS_KEY_EXECUTION_PAUSED, executionId);  // "sf:execution:paused:{id}"
    redisTemplate.opsForValue().set(key, reason.name(), Duration.ofHours(24));
    stateMachine.transition(AgentExecutionState.PAUSED, execution);
}
```

### Resume
```java
public void resumeExecution(Long executionId) {
    SfAgentExecution execution = executionMapper.selectById(executionId);
    String key = String.format(REDIS_KEY_EXECUTION_PAUSED, executionId);
    redisTemplate.delete(key);
    stateMachine.transition(AgentExecutionState.READY, execution);
}
```

### Cancel
```java
public void cancelExecution(Long executionId) {
    SfAgentExecution execution = executionMapper.selectById(executionId);
    String key = String.format(REDIS_KEY_EXECUTION_PAUSED, executionId);
    redisTemplate.delete(key);
    stateMachine.transition(AgentExecutionState.CANCELLED, execution);
    stateMachine.removeExecution(executionId);
}
```

### Save Snapshot
```java
public void saveSnapshot(ExecutionSnapshot snapshot) {
    SfAgentExecutionSnapshot entity = new SfAgentExecutionSnapshot();
    entity.setExecutionId(snapshot.getExecutionId());
    entity.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
    entity.setTenantId(null); // filled by TenantLineInterceptor
    snapshotMapper.insert(entity);
}
```

## Redis Keys

| Key Pattern | TTL | Purpose |
|-------------|-----|---------|
| `sf:execution:paused:{executionId}` | 24h | Stores `PauseReason` enum name |

## PauseReason Enum

Defined in the same package (`com.schemaplexai.agent.engine.lifecycle`):
- `USER_REQUESTED` — User manually paused
- `RATE_LIMITED` — Hit rate limit, auto-paused
- `BUDGET_EXCEEDED` — Token or cost budget exceeded
- `ERROR_RECOVERY` — Paused for error recovery

## Dependencies

| Component | Role |
|-----------|------|
| `AgentStateMachine` | State transitions |
| `SfAgentExecutionMapper` | Load execution record |
| `SfAgentExecutionSnapshotMapper` | Persist snapshots |
| `StringRedisTemplate` | Pause state in Redis |
| `ObjectMapper` | Snapshot JSON serialization |

## Known Issues

- `getLatestSnapshot()` returns `null` — complex query not yet implemented with `BaseMapperX`
- Snapshot `tenantId` is set to `null` and relies on `TenantLineInterceptor` auto-injection

## Backlinks

- Called by: [[controllers/agent-execution-controller]]
- Orchestrator: [[services/agent-runtime-orchestrator]]
- State machine: [[services/agent-state-machine]]
- Entity: [[entities/agent]]
