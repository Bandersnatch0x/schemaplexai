---
topic: event-driven-architecture-outbox-inbox
stage: decision
version: v1.0
status: 已批准
supersedes: ""
---

# ADR-016: Event-Driven Architecture with Outbox + Inbox Patterns

> **日期**: 2026-05-09
> **决策人**: 架构评审委员会
> **状态**: 已批准

---

## 背景

The pivot to "Agent Execution Control Plane" introduces a split between Engine (state machine, event production) and Core (approval, audit, cost, read models). These two deployable units communicate asynchronously via RabbitMQ. The core interaction pattern:

```
Engine produces ExecutionEvents → MQ → Core consumers project to ReadModel + Audit + Cost
Engine requests approval → MQ → Core creates ApprovalTicket
Core approves/rejects → MQ → Engine resumes execution
```

This async boundary introduces classic distributed systems challenges:
- Atomicity: How to ensure an event is published if and only if the state change is committed?
- Reliability: What if MQ is down when we need to publish?
- Idempotency: What if the same event is delivered twice?
- Ordering: What if events arrive out of order?

## 决策

**Adopt the Transactional Outbox pattern (Engine side) + Inbox/Dedup table (Core side)** for all cross-service event communication.

### 实现细节

1. **Outbox (Engine side)**

```sql
CREATE TABLE sf_execution_outbox (
    id            BIGSERIAL PRIMARY KEY,
    event_id      UUID NOT NULL UNIQUE,
    execution_id  BIGINT NOT NULL,
    seq           INT NOT NULL,
    topic         VARCHAR(64) NOT NULL,
    payload       JSONB NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    published_at  TIMESTAMPTZ,
    retry_count   INT DEFAULT 0,
    CHECK (retry_count <= 5)
);
```

- Event and Outbox entry written in the SAME database transaction
- OutboxPublisher polls `published_at IS NULL AND retry_count < 5` entries
- After successful MQ publish, marks `published_at`
- After 5 failures, marks as DEAD (requires manual intervention)
- UNIQUE(event_id) prevents duplicate Outbox entries

2. **Inbox (Core side)**

```sql
CREATE TABLE sf_processed_event (
    event_id      UUID NOT NULL,
    consumer_name VARCHAR(64) NOT NULL,
    processed_at  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (event_id, consumer_name)
);
```

- Each consumer inserts a row BEFORE processing
- Composite PK `(event_id, consumer_name)` allows independent tracking per consumer
- Duplicate delivery → `DuplicateKeyException` → ACK and skip
- Insert and business logic in same transaction for atomicity

3. **Event ordering (Core side)**

- Consumer maintains per-execution `TreeMap<Integer, ExecutionEvent>` buffer
- Events applied in `seq` order, confirmed via `confirmedSeq` watermark
- Gap > 30s → `GapRecoveryJob` queries `ExecutionEvent` table directly for missing seqs

4. **Retry and DLQ**

| Consumer | Max Retries | Backoff | DLQ |
|----------|------------|---------|-----|
| ApprovalRequestConsumer | 3 | 5s fixed | approval.requests.dlq |
| DeferredApprovalConsumer | 5 | Exponential | approval.deferred.dlq |
| CostSyncConsumer | 3 | 2s fixed | cost.sync.dlq |
| AuditEventConsumer | 3 | 2s fixed | audit.events.dlq |

## 理由

- **Atomicity guarantee**: Event published iff transaction commits. No "phantom events" or "lost events."
- **Reliability**: MQ outage → events accumulate safely in PG Outbox. Published on reconnect. No event loss.
- **Exactly-once processing**: Inbox dedup prevents double-processing even with MQ redelivery.
- **Ordering guarantee**: Per-execution seq buffer ensures events applied in causal order.
- **Simplicity**: Pure PG-based patterns, no external transaction coordinator (no saga, no 2PC).

## 影响

- **对现有代码的影响**
  - New tables: `sf_execution_outbox`, `sf_processed_event`
  - Engine: `OutboxService`, `OutboxPublisher` scheduled job
  - Core: Inbox check pattern in all MQ consumers
  - Event ordering: `ExecutionEventBuffer`, `GapRecoveryJob`

- **对性能的影响**
  - Outbox write: one additional INSERT per event in same transaction (~1ms overhead)
  - Outbox publish: polling job every 1s, negligible load
  - Inbox dedup: INSERT on each event processing, indexed by PK (~1ms)
  - Total overhead per event: ~2-3ms (acceptable for async flow)

- **对运维的影响**
  - Monitor: `sf_execution_outbox` entries with `retry_count >= 4` (alert before DEAD)
  - Monitor: `sf_execution_outbox` entries with `published_at IS NULL AND created_at > 5 min ago`
  - Cleanup: DEAD entries retained 30 days for investigation
  - Recovery: GapRecoveryJob manual trigger available via admin endpoint

## 替代方案

| 方案 | 优点 | 缺点 | 结果 |
|------|------|------|------|
| Direct MQ publish (no Outbox) | Simplest, lowest latency | No atomicity — crash between DB write and MQ publish loses events | 拒绝 |
| Change Data Capture (Debezium) | Zero application code for events | Requires Kafka, operational complexity, eventual consistency only | 拒绝 |
| Saga orchestration | Compensating transactions | Complex, overkill for approval flow (not a distributed transaction) | 拒绝 |
| **Outbox + Inbox (本方案)** | Atomic, reliable, simple | 2-3ms overhead per event, polling latency | **采纳** |
| Event sourcing (full event store) | Complete audit trail, temporal queries | Massive complexity increase, premature for v1 | 拒绝 |

## 相关文档

- `.claude/outputs/phase-0/red-test-skeleton.md`
- `C:\Users\amsterdam\.claude\plans\twinkling-mixing-flute.md` (Section 2.5: Commands, Events & Concurrency)
- ADR-014: agent-engine as Flyway Schema Owner
- ADR-009: Agent State Machine