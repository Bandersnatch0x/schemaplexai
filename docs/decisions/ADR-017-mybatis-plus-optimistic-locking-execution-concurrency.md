---
topic: mybatis-plus-optimistic-locking-execution-concurrency
stage: decision
version: v1.0
status: 已批准
supersedes: ""
---

# ADR-017: MyBatis-Plus Optimistic Locking for Execution Concurrency Control

> **日期**: 2026-05-09
> **决策人**: 架构评审委员会
> **状态**: 已批准

---

## 背景

The `sf_agent_execution` table is the aggregate root for execution state. Multiple actors can concurrently issue commands against the same execution:

- **Engine internally**: StateMachine transitions (RUNNING → PAUSED, etc.)
- **Core externally**: User commands (pause, resume, cancel, approve) forwarded via MQ
- **Multiple Engine instances**: In a horizontally scaled deployment, two pods might process events for the same execution (though distributed lock minimizes this)

Without concurrency control, these concurrent operations could produce:
- Lost updates: two state transitions applied, one overwrites the other
- Stale command execution: a "pause" command applied after execution already completed
- Inconsistent state: Engine internal state disagrees with persisted state

### Existing Infrastructure

- MyBatis-Plus 3.5.5 is the ORM (already in use)
- MyBatis-Plus provides built-in optimistic locking via `@Version` annotation
- `sf_agent_execution` already has a `version` column (INT, default 0)

## 决策

**Use MyBatis-Plus `@Version` optimistic locking** as the primary concurrency control mechanism for `sf_agent_execution`, supplemented by Redis distributed locks as a throughput optimization.

### 实现细节

1. **Entity configuration**

```java
@TableName("sf_agent_execution")
public class SfAgentExecution extends BaseEntity {
    private ExecutionState state;
    
    @Version
    private Integer version;        // MyBatis-Plus auto-increments on update
    
    private Integer lastEventSeq;   // Tracks latest event sequence number
    // ...
}
```

2. **Update pattern**

```java
// MyBatis-Plus automatically adds: WHERE version = #{expectedVersion}
// And sets: SET version = version + 1
int rows = executionMapper.updateById(execution);

if (rows == 0) {
    // Version mismatch — concurrent modification detected
    throw new OptimisticLockException(
        "Execution " + executionId + " was modified concurrently. " +
        "Expected version " + expectedVersion + " but current version differs."
    );
}
```

3. **Command validation**

All external commands (Core → Engine via MQ) carry `expectedExecutionVersion`:

```java
record ApprovalDecisionEvent(
    UUID ticketId,
    Long executionId,
    ApprovalDecision action,
    int expectedExecutionVersion,    // Client's view of execution version
    int decisionVersion
);
```

Engine validates `execution.version == expectedExecutionVersion` before applying.

4. **Version mismatch handling**

| Source | On Version Mismatch | Response |
|--------|---------------------|----------|
| Engine internal transition | Retry with fresh state (max 3 retries) | Reload execution, re-evaluate transition |
| User command (MQ) | 409 Conflict | Return error via `approval.decision.rejected` event with reason VERSION_CONFLICT |
| Approval decision | Reject decision | Client must re-fetch execution state and re-submit |

5. **Distributed lock (optimization, NOT consistency guard)**

```
Redis lock: SET execution:lock:{executionId} {token} NX EX 30
Purpose: Reduce optimistic lock conflicts (serialize Engine-internal transitions)
Not a consistency guarantee: PG optimistic lock is the true guard
Safety: If Redis lock fails, system falls back to PG optimistic lock only
```

6. **Concurrency rules summary**

| Rule | Enforcement |
|------|------------|
| Engine internal transitions are single-threaded per execution | Redis distributed lock (best-effort) + PG optimistic lock (guarantee) |
| Core commands carry expectedVersion | MQ event field |
| Engine validates version before applying external command | Optimistic lock |
| Version mismatch → conflict response | 409 / rejected decision |
| State transition matrix owned solely by Engine | No external module writes execution.state |

## 理由

- **Framework-native**: MyBatis-Plus `@Version` requires zero additional dependency. One annotation on the entity field.
- **Battle-tested**: MyBatis-Plus optimistic locking is used in thousands of production systems.
- **Correctness**: Optimistic locking provides true consistency guarantee. Redis lock is an optimization.
- **Conflict visibility**: Version mismatch is explicit (rows == 0), not silent data corruption.
- **Defense in depth**: Redis lock reduces conflicts at the application level, PG lock guarantees correctness at the data level.

## 影响

- **对现有代码的影响**
  - `SfAgentExecution`: add `@Version` annotation on `version` field (done)
  - `ExecutionConcurrencyService`: new service wrapping version-check logic
  - All Engine state transitions: must handle `OptimisticLockException` with retry
  - All Core commands to Engine: must include `expectedExecutionVersion` in event payload

- **对性能的影响**
  - Optimistic lock: zero overhead on successful updates (no extra query)
  - Conflict retry: reload + re-evaluate (~5-10ms) on conflict — expected < 0.1% of updates
  - Distributed lock: Redis round-trip (~1ms) per state transition — saves PG conflict retries

- **对API的影响**
  - Execution status response includes `version` field (client must track for commands)
  - Command endpoints accept `expectedVersion` parameter
  - 409 Conflict HTTP status for version mismatches on REST commands

## 替代方案

| 方案 | 优点 | 缺点 | 结果 |
|------|------|------|------|
| Pessimistic locking (SELECT FOR UPDATE) | No retry logic needed | Holds row locks for duration of state transition, reduces throughput | 拒绝 |
| Redis lock only (no PG lock) | Simple, fast | No durability guarantee — Redis crash loses lock state, potential duplicate transitions | 拒绝 |
| Event sourcing (no mutable state) | Full audit trail | Massive complexity, rewrites entire persistence layer for v1 | 拒绝 |
| **MyBatis-Plus @Version + Redis lock (本方案)** | Framework-native, defense in depth | Retry logic needed, version field in API | **采纳** |
| No concurrency control | Simplest | Data corruption on concurrent updates — unacceptable | 拒绝 |

## 相关文档

- `.claude/outputs/phase-0/red-test-skeleton.md` (ExecutionVersionConflictTest)
- `C:\Users\amsterdam\.claude\plans\twinkling-mixing-flute.md` (Section 2.5.2: Execution Concurrency Control)
- ADR-016: Event-Driven Architecture with Outbox + Inbox
- ADR-009: Agent State Machine