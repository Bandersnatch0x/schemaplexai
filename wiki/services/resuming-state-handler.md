---
title: ResumingStateHandler
type: service
source: com.schemaplexai.agent.engine.state.ResumingStateHandler
creation_date: 2026-05-06
tags: [agent-engine, state-handler, snapshot, resume]
confidence: high
---

# ResumingStateHandler

Handles the `RESUMING` state — loads persisted snapshot and restores execution context.

## State Transition Path

```
PAUSED → (Resume API called) → RESUMING → (snapshot restored) → THINKING
```

## Resume Flow

1. **Validate snapshot ID**: Check `execution.snapshotId` exists
2. **Load snapshot**: Query `sf_agent_execution_snapshot` by ID
3. **Cross-tenant validation**: Verify `snapshot.executionId == execution.id` (prevents snapshot injection)
4. **Integrity check**: Ensure `snapshotJson` is non-null and non-empty
5. **Restore context**: Attach snapshot JSON to execution metadata as `restoredContext`
6. **Transition**: Move to `THINKING` to continue from pause point

## Security: Cross-Tenant Snapshot Injection Prevention

```java
if (snapshot.getExecutionId() != null 
    && !snapshot.getExecutionId().equals(execution.getId())) {
    log.error("Snapshot {} belongs to execution {}, not {}", ...);
    stateMachine.transition(AgentExecutionState.FAILED, execution);
    return;
}
```

This prevents a malicious tenant from loading another tenant's snapshot by guessing the snapshot ID.

## Schema Dependency

Reads from `sf_agent_execution_snapshot`:

| Column | Purpose |
|--------|---------|
| `id` | Snapshot primary key |
| `execution_id` | Foreign key to execution (used for validation) |
| `snapshot_json` | Serialized `ExecutionSnapshot` JSON |

## Failure Handling

Any validation failure transitions execution to `FAILED`:
- Missing snapshot ID
- Snapshot not found in DB
- Snapshot belongs to different execution (injection attempt)
- Snapshot JSON is corrupt (null/blank)

## See Also

- [[services/agent-execution-lifecycle-service]] — `resumeExecution()` API triggers this handler
- [[services/agent-state-machine]] — orchestrates the PAUSED → RESUMING → THINKING transition
