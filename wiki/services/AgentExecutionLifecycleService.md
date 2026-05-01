---
title: AgentExecutionLifecycleService
type: service
source: schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/lifecycle/AgentExecutionLifecycleService.java
creation_date: 2026-05-01
tags: [agent, engine, lifecycle, pause, resume, snapshot]
confidence: high
---

# AgentExecutionLifecycleService

> Manages agent execution lifecycle operations: pause, resume, cancel, and snapshot persistence.

## Responsibilities

1. Pause executions with a reason (stored in Redis)
2. Resume paused executions
3. Cancel executions and clean up
4. Save and retrieve execution snapshots

## Dependencies

| Dependency | Type | Purpose |
|------------|------|---------|
| `AgentStateMachine` | Component | State transitions |
| `SfAgentExecutionMapper` | Mapper | Execution persistence |
| `SfAgentExecutionSnapshotMapper` | Mapper | Snapshot persistence |
| `StringRedisTemplate` | Spring | Redis operations for pause state |
| `ObjectMapper` | Jackson | Snapshot JSON serialization |

## Methods

### `pauseExecution(Long executionId, PauseReason reason)`

- Validates execution exists
- Stores pause reason in Redis with 24h TTL (`REDIS_KEY_EXECUTION_PAUSED`)
- Transitions state to `PAUSED`

### `resumeExecution(Long executionId)`

- Validates execution exists
- Deletes pause key from Redis
- Transitions state to `READY`

### `cancelExecution(Long executionId)`

- Validates execution exists
- Deletes pause key from Redis
- Transitions state to `CANCELLED`
- Removes execution from state machine

### `saveSnapshot(ExecutionSnapshot snapshot)`

- Serializes snapshot to JSON
- Persists to `sf_agent_execution_snapshot`

### `getLatestSnapshot(Long executionId)`

- Currently returns `null` (placeholder; needs order-by query)

## Pause Reasons

| Reason | Description |
|--------|-------------|
| `USER_REQUEST` | User explicitly paused |
| `TOKEN_BUDGET_WARNING` | Token budget threshold reached |
| `QUALITY_GATE_BLOCKED` | Quality gate evaluation failed |
| `LOOP_DETECTED` | Repeating pattern detected |
| `MANUAL_APPROVAL_REQUIRED` | Human-in-the-loop checkpoint |

## Notes

- Pause state is stored in Redis (not DB) for fast access and TTL-based auto-expiry.
- Snapshots enable execution replay and debugging.
