---
topic: agent-execution-control-plane
stage: approved
version: 1.0.0
status: active
---

# Plan: SchemaPlexAI Pivot to Agent Execution Control Plane

## Context

**Why this change:** The original "full lifecycle AI R&D collaboration platform" direction has lost focus. Product boundary is too wide, architecture is prematurely microserviced, and no single不可替代 value anchor exists. After 6-dimension analysis and roundtable debates, the team has decided to pivot to "Agent Execution Control Plane" — a console that lets teams safely launch, observe, control, and audit agent executions.

**Core value proposition:** "Not making agents smarter, but making organizations dare to use agents."

**Key product line (4 steps):** Launch execution → Observe execution → Control execution → Review execution. Anything that does not strengthen these 4 steps does not enter v1.

---

## Section 1: Architecture

### Deployable Units (4 units)

| Unit | Modules | Scaling Profile | Rationale |
|------|---------|-----------------|-----------|
| **Gateway** | gateway | Edge, independent | JWT, tenant, rate limit, routing |
| **Agent-Engine** | agent-engine | Compute-heavy, stateful | State machine, execution orchestration, SSE producer |
| **Workflow** | workflow | Stateful, long-running | Flowable BPMN engine for complex approvals |
| **Core** | system + web + ops + quality + task + integration + context + agent-config + common + model + dao + admin(merged) | CRUD/web/controllers | Modular monolith with internal package boundaries |

### Infrastructure

| Component | Status | Rationale |
|-----------|--------|-----------|
| PostgreSQL | KEEP | Primary data store, truth source for all business state |
| Redis | KEEP | Hot cache, session store, distributed locks, rate limiting |
| RabbitMQ | KEEP | Event bus between Engine and Core |
| Flowable | KEEP | BPMN engine for complex approval workflows |
| ClickHouse | **v1.1** | Cost analytics deferred — use PG short-path for v1 |
| Elasticsearch | REMOVE | No active code references |
| Prometheus/Grafana | REMOVE | No metrics endpoints configured |
| Milvus | REMOVE | RAG is not core to control plane |
| MinIO | DEFER | File storage not in v1 MVP |

---

## Section 2: Components & Data Flow

### Agent-Engine Components

| Component | Responsibility |
|-----------|---------------|
| `AgentExecutionController` | REST: execute, pause, resume, cancel, SSE events |
| `AgentRuntimeOrchestrator` | Main execution loop: admission check → state machine → tool call → event emission |
| `AgentStateMachine` | 15+ state transitions: INITIALIZING → RUNNING → PAUSED → GATE_BLOCKED → COMPLETED/FAILED/REJECTED/CANCELLED |
| `ExecutionEventBus` | Produces execution events to MQ (not SSE directly) |
| `ExecutionAdmissionService` | 5-dimension admission: rate limit, concurrency, token budget, cost budget, daily tool-call budget |
| `ApprovalService` | Runtime approval handling (Phase 1: unified API, DB-persisted) |
| `ToolApprovalService` | High-risk tool call approval integration |
| `LocalPolicyCache` | Caffeine cache of tenant policies with 5-min TTL |
| `ExecutionSnapshotService` | Snapshot save/restore: Redis L1 + PG checkpoint |

### Workflow Components

| Component | Responsibility |
|-----------|---------------|
| `FlowableEngine` | BPMN process execution |
| `BpmnApprovalDeployer` | Deploys `agent-execution-approval.bpmn20.xml` |
| `HumanTaskAssignmentDelegate` | Assigns human tasks in BPMN flows |
| **Rule:** Workflow does NOT own execution state. It only processes ApprovalTickets with `handler=workflow`. |

### Core Components

| Sub-module | Key Components | Responsibility |
|------------|---------------|---------------|
| system | `AuthService`, `TenantService`, `UserService`, `RoleService`, `TenantPolicyService` | Auth, RBAC, tenant isolation, tenant-level policies |
| web | `ExecutionWebController`, `ApprovalWebController`, `CostWebController`, `SseController` | HTTP API entry, DTO mapping, SSE frontend exposure |
| ops | `CostService`, `BudgetService`, `BudgetGuard` | Cost tracking (PG), budget enforcement, budget alerts |
| quality | `AuditEventService`, `ToolApprovalServiceImpl` | Audit CRUD, tool registration approval |
| task | `AgentExecuteDispatcher`, `CostSyncConsumer`, `NotificationConsumer` | MQ consumers |
| integration | `GitHubToolExecutor`, `GitLabToolExecutor` | Tool execution for GitHub/GitLab (only) |

### Data Flow (4-Step Product Line)

```
User → Gateway → Core/Web Controller → Core validates auth/budget → MQ → Agent-Engine
                                                                           │
Agent-Engine: StateMachine drives execution → produces ExecutionEvents → Outbox → MQ → Core
                                                                           │
High-risk tool call → ApprovalPolicyEngine (local cache) → FAST_APPROVAL / BPMN_APPROVAL
                                                                           │
FAST: Engine pauses, sends ApprovalRequestEvent → MQ → Core creates ApprovalTicket
BPMN: Core creates ticket, starts Workflow instance with businessKey=ticketId
                                                                           │
User approves → Core → Outbox(approval.decisions) → MQ → Engine → StateMachine resume
                                                                           │
Execution ends → CostEvent + AuditEvent → Core projection → ReadModel + SSE push
```

---

## Section 2.4: State Ownership & Storage Matrix

| Data | Store | Truth Source | Owner | Notes |
|------|-------|--------------|-------|-------|
| `Execution` (state, version, lastEventSeq) | PG | YES | Agent-Engine | Version optimistic lock for concurrency |
| `ExecutionEvent` (immutable event stream) | PG | YES | Agent-Engine | eventId = UUIDv5(namespace, executionId + ":" + seq) |
| `ExecutionSnapshot` | PG + Redis | PG | Agent-Engine | PG = checkpoint, Redis = hot recovery |
| `ApprovalTicket` | PG | YES | Core | Unified truth for all approvals |
| `AuditEvent` | PG | YES | Core | Projected from ExecutionEvent |
| `CostRecord` | PG | YES | Core | Projected from ExecutionEvent |
| `Budget` | PG | YES | Core | Tenant-level budget config |
| `TenantPolicy` | PG | YES | Core | Approval policies, tool whitelist, cost caps |
| `WorkflowInstance` | PG | YES | Workflow | Flowable native tables |
| `IdempotencyKey` | Redis | Redis | Core | 24h TTL |
| `SseSession` | Redis | Redis | Core | User session mapping |

### Core Principles

1. **PG = truth source.** All business state, events, audit, cost primary copies in PostgreSQL.
2. **Redis = hot cache + recovery.** Session state, rate limit counters, distributed locks. Rebuildable from PG.
3. **Engine = event producer only.** Produces domain events via Outbox + MQ. Never directly writes Core tables.
4. **Core = event consumer + read model builder.** Subscribes to events, maintains query views.
5. **Single state owner per aggregate.** Execution state owned by Engine. Approval state owned by Core.

---

## Section 2.5: Commands, Events & Concurrency Contracts

### 2.5.1 Event Storage & Idempotency

**ExecutionEvent (PG, append-only)**
```sql
CREATE TABLE sf_execution_event (
    event_id      UUID PRIMARY KEY,           -- UUIDv5(namespace, executionId:seq)
    execution_id  BIGINT NOT NULL,
    seq           INT NOT NULL,
    event_type    VARCHAR(32) NOT NULL,
    payload       JSONB,
    occurred_at   TIMESTAMPTZ NOT NULL,
    tenant_id     BIGINT NOT NULL,
    agent_id      BIGINT,
    sensitivity   VARCHAR(16),                -- AUDIT, DEBUG, EPHEMERAL
    UNIQUE(execution_id, seq)
);
```

**Outbox (PG, Engine writes)**
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

**Inbox (PG, Core consumer dedup)**
```sql
CREATE TABLE sf_processed_event (
    event_id      UUID NOT NULL,
    consumer_name VARCHAR(64) NOT NULL,
    processed_at  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (event_id, consumer_name)   -- composite: each consumer independent
);
```

### 2.5.2 Execution Concurrency Control

**Execution table with optimistic locking**
```sql
CREATE TABLE sf_agent_execution (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    agent_id        BIGINT NOT NULL,
    state           VARCHAR(32) NOT NULL,
    version         INT NOT NULL DEFAULT 0,
    last_event_seq  INT NOT NULL DEFAULT 0,
    context_json    JSONB,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);
```

**Concurrency rules:**
- Core commands (pause, resume, cancel, approve) carry `expectedVersion`
- Engine validates `version == expectedVersion`; mismatch → 409 Conflict
- Engine internal state transitions are single-threaded per execution (distributed lock)
- State transition matrix is owned solely by Engine

**ExecutionState enum**
```java
enum ExecutionState {
    INITIALIZING, RUNNING, PAUSED, GATE_BLOCKED,
    COMPLETED, FAILED, CANCELLED, REJECTED
}
```

### 2.5.3 Approval Decision Loop

**ApprovalDecisionEvent (Core → Engine via MQ)**
```java
record ApprovalDecisionEvent(
    UUID ticketId,
    Long executionId,
    ApprovalDecision action,           // APPROVE, REJECT, ESCALATE
    String approverId,
    String reason,
    Instant decidedAt,
    int decisionVersion,               // ticket-level ordering
    int expectedExecutionVersion       // execution optimistic lock version at decision time
);
```

Engine validates `execution.version == expectedExecutionVersion` before applying.

### 2.5.4 Policy Engine Runtime

- Core owns policy configuration (`TenantPolicyService`)
- Engine holds `LocalPolicyCache` (Caffeine, 5-min TTL)
- Policy changes pushed via MQ: `tenant.policy.updated`
- Cache miss → synchronous fallback to Core (2s timeout)
- **Fail-closed:** Core unreachable → execution enters `GATE_BLOCKED`, deferred approval request queued

### 2.5.5 SSE Ownership

- **Frontend connects ONLY to Core** (`GET /api/executions/{id}/events?lastSeq={n}`)
- Engine → MQ → Core Consumer → ReadModel + SSE push
- Reconnect: Core queries `ExecutionEvent` (seq > lastSeq), excludes EPHEMERAL events
- Session state managed by Core in Redis

### 2.5.6 ID Relationships

| ID | Format | Owner | Business Key |
|----|--------|-------|--------------|
| `executionId` | BIGSERIAL | Engine | Execution aggregate root |
| `ticketId` | UUID | Core | Approval ticket; Workflow businessKey = ticketId |
| `approvalRequestId` | UUIDv5 | Engine | Per-pause-point unique ID |
| `eventId` | UUIDv5 | Engine | `{executionId}:{seq}` |
| `workflowInstanceId` | VARCHAR(64) | Workflow | Flowable internal |

**Single-ticket rule:** One ApprovalTicket per approval need. FAST → BPMN upgrade changes `stage` and `handler`, does NOT create new ticket.

---

## Section 2.5.8: Multi-Instance Serialization & Event Reordering

### Distributed Lock (per execution)

```
SET execution:lock:{executionId} {lockToken} NX EX 30
Heartbeat renew: EXPIRE execution:lock:{executionId} 30 (every 10s)
Release: Lua compare-and-delete (token match)
```

**Lock failure:** NACK + exponential backoff requeue. No fixed retry ceiling (24h TTL upper bound).

**Safety note:** Redis lock is optimization. True consistency from PG optimistic lock.

### Event Reordering

Consumer maintains per-execution `TreeMap<Integer, ExecutionEvent>` buffer + `confirmedSeq` watermark. Events applied in seq order. Gap > 30s triggers `GapRecoveryJob`.

### GapRecoveryJob

Queries `ExecutionEvent` directly for missing seq ranges, republishes to MQ via `mqPublisher.republish()` (NOT via Outbox to avoid UNIQUE conflict).

---

## Section 2.5.9: Approval Request Creation & Degradation

### Approval Request Event (Engine → Core)

```java
record ApprovalRequestEvent(
    UUID approvalRequestId,           -- UUIDv5(namespace, executionId + ":approval:" + triggeringSeq)
    Long executionId,
    Long tenantId,                    -- Core needs this to create ticket
    Long agentId,                     -- Audit and SSE filtering
    int triggeringSeq,                -- Links to exact ExecutionEvent that triggered approval
    String requestType,               -- FAST | BPMN
    RiskLevel riskLevel,
    String actionDescription,
    int executionVersionAtPause,
    Instant createdAt
);
```

**Idempotency:** Core Consumer uses `approvalRequestId` as unique key. Same execution can have multiple approvals (each tool call = new triggeringSeq = new request).

### Approval Request Flow

```
Engine detects high-risk tool call
  → ApprovalPolicyEngine (local cache) decides FAST / BPMN
  → StateMachine: RUNNING → PAUSED
  → Write ExecutionEvent (APPROVAL_REQUESTED, seq=N)
  → Write Outbox (topic: approval.requests)
  → MQ → Core ApprovalRequestConsumer
      → Check processed_event for approvalRequestId
      → Create ApprovalTicket (stage=PENDING_FAST or PENDING_BPMN)
      → If BPMN: start Workflow with businessKey=ticketId
      → Write AuditEvent
      → Push SSE
```

### Degradation: Core Unreachable

```
Engine detects need for approval
  → PolicyCache miss / Core unreachable
  → StateMachine: RUNNING → GATE_BLOCKED
  → Write ExecutionEvent (GATE_BLOCKED, reason: POLICY_CACHE_MISS)
  → Write Outbox (topic: approval.requests.deferred)
  → MQ → Core DeferredApprovalConsumer (when Core recovers)
      → Create ApprovalTicket (deferred=true)
      → Write Outbox (topic: approval.deferred.created)
      → MQ → Engine ApprovalDeferredCreatedConsumer
          → Validate execution.state == GATE_BLOCKED
          → StateMachine: GATE_BLOCKED → PAUSED
          → Write ExecutionEvent (DEFERRED_APPROVAL_ACTIVATED)
```

**Critical rule:** Core NEVER directly modifies Execution.state. All state changes originate from Engine.

---

## Section 2.5.10: Event Sensitivity & Retention

| Level | Content | Store | Retention | PII | SSE |
|-------|---------|-------|-----------|-----|-----|
| `AUDIT` | state change, tool result, approval decision, cost | PG | Permanent | Secrets masked | Always push |
| `DEBUG` | thought, plan, reasoning | PG | 7 days | Default masked; tenant opt-in for raw | Optional push |
| `EPHEMERAL` | token stream | None | Real-time only | N/A | Push only, no replay |

---

## Next: Section 3 — Error Handling, Security & Testing

This plan file will be extended with Section 3 after this baseline is approved.

---

## Section 3: Error Handling, Security & Testing

### 3.1 Error Handling

#### 3.1.1 Retry Policies

**Outbox Publishing (Engine side)**

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| Max retries | 5 | Aligned with `sf_execution_outbox.CHECK(retry_count <= 5)` |
| Backoff strategy | Exponential: 1s, 2s, 4s, 8s, 16s | Prevents MQ overload during transient failures |
| Terminal state | After 5th failure, event marked as `DEAD` | Operator alert triggered, manual resolution required |
| Retry scope | Per-outbox-entry | Independent retries avoid one poison message blocking others |

**Dead Letter Queue (Poison Messages)**

```
Engine Outbox publish fails after 5 retries
  → Event status = DEAD
  → Alert to ops channel (PagerDuty / webhook)
  → DLQ consumer: execution.dead-letter queue
  → Core DeadLetterHandler:
      - Log full event payload
      - Notify tenant admin (in-app notification)
      - Create AuditEvent with DEAD_LETTER severity
      - Manual retry via admin console
```

**MQ Consumer Retry (Core side)**

| Consumer | Retry Policy | DLQ Routing |
|----------|-------------|-------------|
| ApprovalRequestConsumer | 3 retries, 5s fixed backoff | `approval.requests.dlq` |
| DeferredApprovalConsumer | 5 retries, exp backoff | `approval.deferred.dlq` |
| CostSyncConsumer | 3 retries, 2s fixed backoff | `cost.sync.dlq` |
| AuditEventConsumer | 3 retries, 2s fixed backoff | `audit.events.dlq` |

**Consumer idempotency guard:** All consumers check `sf_processed_event` before processing. Re-delivery of already-processed events is silently acknowledged.

#### 3.1.2 Graceful Degradation

**Policy Cache Degradation**

```
PolicyCache.get(tenantId, policyType):
  1. Cache hit → return immediately
  2. Cache miss → HTTP GET Core/TenantPolicyService (timeout 2s)
     a. Success → cache and return
     b. Timeout / 5xx:
        → AgentRuntimeOrchestrator applies FAIL-CLOSED default
        → All approvals → GATE_BLOCKED (not FAULTY)
        → Event: POLICY_CACHE_MISS, deferred request queued
        → Operator: alert raised, executions held safely
  3. Recovery: Core back online → DeferredApprovalConsumer activates
     → Engine transitions GATE_BLOCKED → PAUSED → resumes approval flow
```

**Core Unreachable Scenarios**

| Scenario | Engine Behavior | User Impact |
|----------|----------------|-------------|
| Core HTTP 5xx | GATE_BLOCKED, deferred queue | Execution paused, no data loss |
| Core HTTP timeout (2s) | GATE_BLOCKED, deferred queue | Execution paused, auto-resume on recovery |
| MQ connection lost | Outbox accumulates in PG | Events queued, published on reconnect |
| Complete Core outage | Engine runs independently, Outbox buffers all events | Executions continue, events delivered later |

**SSE Degradation**

```
SSE connection lost:
  Client reconnects to Core → GET /api/executions/{id}/events?lastSeq={n}
  Core queries ExecutionEvent (seq > lastSeq, sensitivity != EPHEMERAL)
  Core replays missed events, then opens SSE stream
  Gap: events during disconnect window are never lost (PG is truth source)
```

#### 3.1.3 Circuit Breakers

| Resource | Breaker Type | Threshold | Recovery | Fallback |
|----------|-------------|-----------|----------|----------|
| **RabbitMQ connection** | FAIL-CLOSED | 5 failures in 60s | Half-open after 30s, probe with single publish | Outbox accumulates in PG; no message loss |
| **Redis connection** | FAIL-OPEN (locks), FAIL-CLOSED (cache) | 3 failures in 30s | Half-open after 15s | Locks: proceed without lock (PG optimistic lock is safety net); Cache: GATE_BLOCKED per policy degradation |
| **PostgreSQL connection** | HARD FAIL (service restart) | Any connection pool exhaustion | Kubernetes health check → restart | Container liveness probe fails, pod restarted |
| **Core HTTP (policy fetch)** | FAIL-CLOSED | 3 timeouts in 60s | Half-open after 30s | GATE_BLOCKED with deferred approval |

**Circuit breaker rationale:**
- **MQ FAIL-CLOSED**: Events are critical path. Outbox persists them safely. Reconnecting with a stale connection risks duplicate publishes.
- **Redis FAIL-OPEN for locks**: PG optimistic locking is the actual consistency guard. Redis lock is an optimization. Losing it means lower throughput, not data corruption.
- **Redis FAIL-CLOSED for cache**: Without policy cache, every tool call would need a synchronous Core call — defeating the purpose. Safer to gate-block.
- **PG HARD FAIL**: No recovery without database. Let Kubernetes handle restart.

#### 3.1.4 Idempotency Guarantees

**Outbox: At-Least-Once**

```
Guarantee: Each ExecutionEvent is published to MQ at least once.
Mechanism:
  1. Event + Outbox entry written in SAME transaction (atomic)
  2. OutboxPublisher polls unpublished entries (published_at IS NULL)
  3. Publishes to MQ, then marks published_at
  4. On crash/restart: unpublished entries are re-polled
  5. UNIQUE(event_id) prevents duplicate outbox entries from the Engine side
```

**Inbox: Exactly-Once Per Consumer**

```
Guarantee: Each consumer processes each event exactly once.
Mechanism:
  1. Consumer receives event from MQ
  2. INSERT INTO sf_processed_event (event_id, consumer_name, processed_at)
     - PK is (event_id, consumer_name) — composite key
     - DuplicateKeyException → event already processed, ACK and skip
  3. Process event business logic in same transaction as INSERT
  4. ACK to MQ (auto-ack after transactional commit)
  5. MQ redelivery (crash before ACK) → step 2 catches duplicate

Why composite key: Different consumers (ApprovalRequestConsumer, CostSyncConsumer)
process the same event for different purposes. Each tracks independently.
```

**Approval Idempotency**

```
Engine produces ApprovalRequestEvent with approvalRequestId = UUIDv5(executionId + ":approval:" + triggeringSeq)

Core ApprovalRequestConsumer:
  → Query sf_processed_event WHERE event_id = approvalRequestId AND consumer_name = 'ApprovalRequestConsumer'
  → If exists: ACK, no-op
  → If not: create ApprovalTicket, insert processed_event, commit

Multiple approvals per execution: different triggeringSeq → different approvalRequestId → different tickets.
Same triggeringSeq re-delivered: same approvalRequestId → idempotent skip.
```

---

### 3.2 Security

#### 3.2.1 Approval Authorization

**RBAC Model for Approvals**

| Role | Permissions | Scope |
|------|------------|-------|
| `TENANT_ADMIN` | Approve/reject any execution, configure approval policies, view all audit events | Tenant-wide |
| `APPROVER` | Approve/reject executions assigned to them or their group, escalate to TENANT_ADMIN | Assigned executions |
| `DEVELOPER` | Submit executions for approval, view own execution audit trail | Own executions only |
| `VIEWER` | Read-only access to execution status and audit events | Tenant-wide (read) |

**Approval Action Authorization**

| Action | Required Role | Validation |
|--------|--------------|------------|
| `APPROVE` | APPROVER or TENANT_ADMIN | Must be assigned approver OR tenant admin |
| `REJECT` | APPROVER or TENANT_ADMIN | Must provide reason if rejecting |
| `ESCALATE` | APPROVER | Escalates to TENANT_ADMIN pool |
| `CANCEL` | DEVELOPER (own) or TENANT_ADMIN (any) | Only cancellable in PAUSED or GATE_BLOCKED state |

**Escalation Rules**

```
Escalation triggers:
  1. APPROVER does not respond within SLA window (configurable per tenant, default 4h)
  2. APPROVER explicitly escalates ("needs higher authority")
  3. Execution risk level > APPROVER authority threshold

Escalation target:
  Level 1: APPROVER → TENANT_ADMIN pool (round-robin)
  Level 2: TENANT_ADMIN → SYSTEM (automatic rejection after 24h no response)

Escalation notification:
  In-app notification to target role group
  Execution event: ESCALATION_TRIGGERED (AUDIT sensitivity)
```

#### 3.2.2 Audit Immutability

**ExecutionEvent (append-only by design)**

```
Schema guarantees:
  - No UPDATE or DELETE grants on sf_execution_event for application user
  - Only INSERT allowed (application-level enforcement via mapper)
  - event_id is UUIDv5 hash — cannot be retroactively inserted in wrong order
  - UNIQUE(execution_id, seq) prevents gap-filling with fake events

Application enforcement:
  - ExecutionEventMapper only exposes insert(ExecutionEvent) — no update/delete methods
  - ExecutionEventService.writeEvent() is the single write path, called only by StateMachine
  - Any attempt to modify events detected by periodic hash-chain validation (Phase 3)
```

**AuditEvent (tamper-evident)**

```
Schema guarantees:
  - sf_audit_event stores content_hash = SHA-256(event_type + payload + occurred_at)
  - ReadModel projection computes hash on write, verifies on read
  - Hash verification failure → alert + event marked CORRUPTED
  - Periodic background job (AuditIntegrityJob) validates last 24h of audit events

Application enforcement:
  - AuditEventConsumer is the ONLY writer of sf_audit_event
  - Core modules write AuditEvent via AuditEventService, which delegates to consumer path
  - No direct INSERT bypassing hash computation
```

#### 3.2.3 Secret Masking

**What is masked and when**

| Data Category | Masking Rule | Applied At | Example |
|---------------|-------------|------------|---------|
| Tool call parameters (secrets) | `***MASKED***` for keys matching `apiKey`, `token`, `secret`, `password`, `credential` | Engine, before writing ExecutionEvent | `{"apiKey": "***MASKED***", "repo": "org/repo"}` |
| Execution context (LLM inputs) | Redact PII patterns (email, phone, IP) via regex | Engine, before snapshot save | `"user: u***@domain.com"` |
| Snapshot data | Same rules as execution context + tool parameters | Engine, SnapshotService | Full snapshot with secrets masked |
| Audit event payload | Inherits masking from source ExecutionEvent | Core, AuditEventConsumer | Already masked at source |
| SSE push | EPHEMERAL events carry raw token stream (not stored), AUDIT events are pre-masked | Core, SseController | Token stream: real-time only, no persistence |

**Masking Implementation**

```java
// SecretMasker utility — Engine internal
class SecretMasker {
    private static final Set<String> SECRET_KEY_PATTERNS = Set.of(
        "apiKey", "token", "secret", "password", "credential", "privateKey"
    );
    private static final Pattern PII_PATTERN = Pattern.compile(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b|" +  // email
        "\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b"                           // phone (US)
    );

    JsonNode mask(JsonNode input);      // Deep-traverse and mask secret keys
    String maskPii(String text);        // Regex-replace PII patterns
}
```

#### 3.2.4 Multi-Tenant Isolation

**TenantContext Propagation Through MQ**

```
Problem: MQ events cross thread boundaries. TenantContext (ThreadLocal) is lost.
Solution: Tenant ID embedded in every event payload.

All MQ events include:
  - tenantId (Long) — source of truth for tenant isolation
  - eventId (UUID) — idempotency key

Core consumers:
  1. Extract tenantId from event payload
  2. Set TenantContextHolder.set(tenantId) BEFORE any DB operation
  3. TenantLineInterceptor auto-injects tenant_id into SQL WHERE clauses
  4. Clear TenantContextHolder in finally block

Engine consumers (if any):
  Same pattern. Engine tables (sf_agent_execution, sf_execution_event) also
  carry tenant_id for cross-tenant query prevention.
```

**Read Model Tenant Filtering**

```
All read model queries (ExecutionReadModel, ApprovalTicket queries) MUST:
  1. Accept tenantId as parameter (never rely solely on TenantContext)
  2. Include tenant_id in WHERE clause explicitly
  3. API layer validates: request tenantId == authenticated user tenantId
  
Exception: TENANT_ADMIN cross-tenant view (explicit RBAC check, audit logged).
```

**Tenant Data Boundaries**

| Data | Isolation Level | Cross-Tenant Access |
|------|----------------|---------------------|
| Execution state | Per-tenant (tenant_id column) | Never |
| Execution events | Per-tenant (tenant_id column) | Never |
| Approval tickets | Per-tenant (tenant_id column) | Never |
| Audit events | Per-tenant (tenant_id column) | TENANT_ADMIN audit view only |
| Cost records | Per-tenant (tenant_id column) | Never (billing boundary) |
| Budget config | Per-tenant (tenant_id column) | Never |
| Tenant policies | Per-tenant (tenant_id column) | Never |
| SSE sessions | Per-tenant (Redis key prefix) | Never |

---

### 3.3 Testing Strategy

#### 3.3.1 Contract Tests

**Scope:** Message queue event schemas between Engine and Core.

**Technology:** Spring Cloud Contract (preferred over Pact — already in Spring ecosystem, no additional infrastructure).

**Contracts to Define**

| Contract Name | Producer | Consumer | Event Schema |
|---------------|----------|----------|-------------|
| `ApprovalRequestEvent` | agent-engine | quality | `approvalRequestId`, `executionId`, `tenantId`, `triggeringSeq`, `requestType`, `riskLevel`, `expectedExecutionVersion` |
| `ApprovalDecisionEvent` | quality (via outbox) | agent-engine | `ticketId`, `executionId`, `action`, `decisionVersion`, `expectedExecutionVersion` |
| `ExecutionStateChangedEvent` | agent-engine | quality, ops | `executionId`, `fromState`, `toState`, `eventSeq`, `tenantId` |
| `CostRecordedEvent` | agent-engine | ops | `executionId`, `tenantId`, `amount`, `currency`, `modelName` |
| `AuditEventRecorded` | agent-engine | quality | `executionId`, `eventType`, `payload`, `sensitivity` |

**Contract Test Structure**

```
schemaplexai-agent-engine/src/test/resources/contracts/
  approvalRequestEvent.groovy        # Produces ApprovalRequestEvent
  executionStateChangedEvent.groovy  # Produces ExecutionStateChangedEvent
  costRecordedEvent.groovy           # Produces CostRecordedEvent
  auditEventRecorded.groovy          # Produces AuditEventRecorded

schemaplexai-quality/src/test/resources/contracts/
  approvalDecisionEvent.groovy       # Produces ApprovalDecisionEvent

Each contract defines:
  - Input: trigger scenario description
  - Output: expected JSON payload with matching rules (type match, not exact value)
  - Generated stub: used by consumer tests to verify deserialization
```

**Verification Pipeline**

```
1. Producer test: generates contract stubs → publishes to local Maven repo
2. Consumer test: pulls stubs, verifies deserialization against expected schema
3. CI: contract tests run in PR pipeline for both producer and consumer
4. Breaking change detection: schema evolution rules (additive only, no field removal)
```

#### 3.3.2 Chaos Testing

**Scenario 1: Redis Failover → Lock Loss**

```
Test setup:
  1. Execution running with distributed lock (execution:lock:{id})
  2. Kill Redis primary, force failover to replica (3-5s downtime)
  3. Observe Engine behavior

Expected behavior:
  - Redis lock acquisition fails during outage window
  - StateMachine falls back to PG optimistic lock (version check)
  - Two concurrent Engine instances attempt same execution: one wins, one gets 409 Conflict
  - NO duplicate state transitions
  - After Redis recovery: locks re-acquired, normal operation resumes
  - Execution outcome: consistent (either COMPLETED or FAILED, never DUPLICATED)

Validation:
  - Query sf_execution_event for duplicate seq numbers → must be 0
  - Query sf_agent_execution for version gaps → must be sequential
```

**Scenario 2: RabbitMQ Partition → Outbox Catchup**

```
Test setup:
  1. Multiple executions running, producing events to Outbox
  2. Simulate network partition: RabbitMQ becomes unreachable from Engine
  3. Maintain partition for 5 minutes
  4. Restore connectivity

Expected behavior:
  - OutboxPublisher detects publish failures, increments retry_count
  - Outbox entries accumulate (retry_count < 5)
  - After 5th failure: event marked DEAD, alert triggered
  - During partition window: Engine continues processing, events buffered in PG
  - After partition heals:
    a. Undead events (retry_count < 5): automatically published
    b. Dead events: operator manually retriggers via admin console
  - All Core consumers receive events in correct seq order (EventReordering handles gaps)

Validation:
  - Outbox: count(published_at IS NULL) == 0 after catchup
  - EventReordering: no gaps in confirmedSeq for any execution
  - No event loss: count(ExecutionEvent) == count(Outbox with published_at NOT NULL) + count(DEAD)
```

**Scenario 3: PostgreSQL Connection Exhaustion**

```
Test setup:
  1. Saturate PG connection pool with slow queries
  2. Observe Engine behavior

Expected behavior:
  - HikariCP connection timeout triggers
  - Engine health endpoint returns DOWN
  - Kubernetes liveness probe fails → pod restart
  - New pod connects to PG with fresh pool
  - In-flight executions: Outbox has committed events (published after restart), uncommitted transactions rolled back
  - Execution recovery: StateMachine reloads from last checkpoint/snapshot

Validation:
  - No partial state written (transaction atomicity)
  - Restarted execution resumes from last committed state
```

#### 3.3.3 Load Testing

**Scenario 1: Concurrent Execution Admission**

```
Goal: Verify rate limiter and budget guard under concurrent load.

Setup:
  - 100 concurrent users submitting execution requests
  - Each user within rate limit individually, but combined exceeds global concurrency cap
  - Budget guard: some tenants near budget limit

Metrics:
  - Rate limiter: requests rejected with 429, rejection rate measured
  - Concurrency guard: queued executions, admission order preserved
  - Budget guard: over-budget executions rejected with 402
  - p95 admission latency < 500ms

Success criteria:
  - Zero executions admitted above configured limits
  - Rate limit counter accurate (no undercount or overcount)
  - Budget enforcement accurate to within 1% of configured limit
```

**Scenario 2: SSE Connection Fan-Out**

```
Goal: Verify SSE broadcast performance under connection load.

Setup:
  - 500 concurrent SSE connections watching 50 different executions
  - Each execution produces 10 events/second
  - Total: 500 events/second broadcast

Metrics:
  - Event delivery latency: p95 < 200ms from Engine Outbox publish to SSE push
  - Connection churn: 50 connections drop and reconnect per minute
  - Replay performance: reconnect queries sf_execution_event within 100ms
  - Memory: SSE session store in Redis under 500MB

Success criteria:
  - Zero missed events during stable connection
  - Replay gap window < 3 seconds (events emitted during disconnect delivered on reconnect)
  - No OOM on Core pod
```

**Scenario 3: MQ Message Rate**

```
Goal: Verify MQ throughput under peak load.

Setup:
  - 1000 executions running concurrently
  - Each producing events at 10 events/second
  - Total: 10,000 events/second through RabbitMQ

Metrics:
  - MQ publish rate: sustained 10k msg/s
  - Consumer processing rate: keeps pace (no growing queue depth)
  - Outbox publish latency: p95 < 100ms from write to MQ publish

Success criteria:
  - RabbitMQ queue depth stable (not monotonically increasing)
  - Consumer lag < 5 seconds
  - No Outbox entries stuck in unpublished state for > 30s
```

#### 3.3.4 Soak Testing

**24-Hour Execution Soak Test**

```
Goal: Verify system stability under sustained load with periodic state transitions.

Setup:
  - 50 concurrent long-running executions
  - Each execution cycles through states: RUNNING → PAUSED (approval) → RUNNING → COMPLETED
  - Each cycle: 30 min RUNNING, 5 min PAUSED, resume → repeat
  - Total: ~48 state transitions per execution over 24h
  - 10% of approvals: BPMN workflow path (multi-step)

External disruptions (injected periodically):
  - Hour 4: Redis failover (lock loss scenario)
  - Hour 8: RabbitMQ restart (connection loss)
  - Hour 12: Core pod restart (SSE reconnect storm)
  - Hour 16: PG read replica lag (5s delay)
  - Hour 20: Network latency spike (200ms added to all MQ)

Monitoring:
  - Execution completion rate: > 95% successful completion
  - Event loss: 0 (every ExecutionEvent has corresponding Outbox entry)
  - State consistency: no execution stuck in intermediate state > 10 min
  - Memory: no heap growth trend (GC overhead < 5%)
  - DB: no connection leak, no transaction timeout accumulation

Success criteria:
  - All 50 executions reach terminal state (COMPLETED, FAILED, or REJECTED)
  - Zero executions in inconsistent state (e.g., PAUSED with no ApprovalTicket)
  - Outbox: all entries published or properly marked DEAD
  - SSE: all AUDIT events delivered, replay gap < 10 seconds
  - Approval tickets: all resolved (APPROVED or REJECTED)
  - Cost records: sum matches total budget consumed
```

**Post-Soak Analysis**

```
After 24h soak:
  1. Compare ExecutionEvent count vs Outbox published count → must match
  2. Verify sf_processed_event has no duplicates per consumer
  3. Check all approval tickets resolved (no orphaned PENDING)
  4. Validate cost records sum against event stream
  5. Check audit integrity hash for last 24h window
  6. Memory profile: confirm no leak in Caffeine cache, Redis session store, MQ connection pool
```

---

## Section 4: Implementation Phases & Milestones

### 4.1 Overview

| Phase | Theme | Duration | Milestones | Target Date |
|-------|-------|----------|------------|-------------|
| **Phase 1** | Foundation: Observability & Safety | ~2 weeks | M8–M10 | W1–W2 |
| **Phase 2** | Control Plane Hardening | ~2–3 weeks | M11–M13 | W3–W5 |
| **Phase 3** | v1 GA Sprint | ~2 weeks | M14–M16 | W6–W7 |

**Scope boundary (v1.1 and beyond):**
- Multi-Agent orchestration (concurrent fan-out/fan-in, Handoff, Group Chat) → v1.1
- ClickHouse cost analytics → v1.1
- Full BPMN drag-and-drop editor → v1.1
- Email/SMS notification channels → v1.1

---

### 4.2 Phase 1: Foundation — Observability & Safety

**Goal:** Replace blind spots with telemetry, close the two MAF-identified safety gaps (#14 tool-call budget, #15 audit provenance), and establish runtime trust.

#### M8: OpenTelemetry Integration (~3 days)

**Deliverables:**
- `OtelConfig` in `schemaplexai-common`: OTLP exporter, resource attributes (service.name, service.version, deployment.environment)
- Custom `ExecutionSpanProcessor`: links MQ message spans to execution spans via `executionId` baggage
- `PolicyCacheMetrics`: Caffeine hit/miss ratio, eviction count exposed as Micrometer gauges
- `ExecutionEventSpanExporter`: converts `ExecutionEvent` occurrences to span events (not replacing events, augmenting traces)
- Replace ad-hoc `PgTraceStore` table reads with traceql queries in debug endpoints

**Code sketch:**
```java
@Configuration
public class OtelConfig {
    @Bean
    public SdkTracerProvider tracerProvider(MeterRegistry meterRegistry) {
        return SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(
                OtlpGrpcSpanExporter.builder()
                    .setEndpoint(otelCollectorEndpoint)
                    .setTimeout(5, TimeUnit.SECONDS)
                    .build()).build())
            .addSpanProcessor(new ExecutionSpanProcessor())
            .setResource(Resource.create(Attributes.of(
                SERVICE_NAME, "schemaplexai-agent-engine",
                SERVICE_VERSION, buildVersion)))
            .build();
    }
}
```

**Acceptance criteria:**
- [ ] Jaeger UI shows execution traces end-to-end (Gateway → Core → MQ → Engine → MQ → Core)
- [ ] Trace contains `executionId`, `tenantId`, `agentId` tags on every span
- [ ] `PolicyCacheMetrics` visible in `/actuator/prometheus`
- [ ] `PgTraceStore` table deprecated (read-only), no new writes
- [ ] Latency overhead < 5% on p99 execution duration

**Dependencies:** None (self-contained).

---

#### M9: Tool-Call Budget Enforcement + Checkpoint Hash (~4 days)

**Deliverables:**
- `ToolCallBudgetService` (Engine): per-execution and per-tenant daily tool-call counters in Redis with PG fallback
- `AgentRuntimeOrchestrator` integration: before each tool invocation, check budget; if exceeded → transition to `REJECTED` with event `TOOL_BUDGET_EXCEEDED`
- `ExecutionSnapshotHashChain` (Engine): on every snapshot save, compute `previousHash + snapshotJson` SHA-256 chain; store in `sf_execution_snapshot.hash_chain`
- `SnapshotIntegrityJob` (Engine, daily): verify hash chain continuity for all executions completed in last 24h

**Schema change:**
```sql
ALTER TABLE sf_agent_execution_snapshot ADD COLUMN hash_chain VARCHAR(64);
ALTER TABLE sf_agent_execution ADD COLUMN tool_calls_today INT NOT NULL DEFAULT 0;
```

**Acceptance criteria:**
- [ ] Execution with 50 tool calls rejected on 51st call when tenant daily limit = 50
- [ ] Counter resets at tenant-local midnight (configurable timezone)
- [ ] Hash chain validates for 1000 consecutive snapshots
- [ ] `SnapshotIntegrityJob` detects a single bit flip in snapshot JSON and raises `SNAPSHOT_CORRUPTED` audit event
- [ ] Budget check p95 latency < 10ms

**Dependencies:** M8 (telemetry needed for budget exhaustion alerts).

---

#### M10: Progressive Skill Disclosure (~3 days)

**Deliverables:**
- `SkillRegistry.v2`: skills annotated with `@SkillTier(TIER_1 | TIER_2 | TIER_3)`
- `TenantSkillUnlockService` (Core): tracks which tiers are unlocked per tenant; default = TIER_1 only
- `AgentConfigValidator` (agent-config): rejects agent definitions referencing tiers the tenant has not unlocked
- Unlock criteria: TIER_2 after 100 successful executions; TIER_3 after 500 + 10-day account age
- Admin override via `TenantAdmin` API

**Acceptance criteria:**
- [ ] New tenant sees only TIER_1 skills in agent builder UI
- [ ] Agent with TIER_2 skill rejected with `SKILL_TIER_LOCKED` error code
- [ ] Auto-unlock triggers within 1 hour of criterion met (via scheduled job)
- [ ] Unlock event written to `sf_audit_event` with `SKILL_TIER_UNLOCKED` type
- [ ] No regression: existing agents without tier annotations default to TIER_1

**Dependencies:** None.

---

### 4.3 Phase 2: Control Plane Hardening

**Goal:** Extract cross-cutting concerns from the FSM, make approval a first-class citizen, and deliver the 4-step product line frontend.

#### M11: Middleware Pipeline (~1 week)

**Deliverables:**
- `ExecutionMiddleware` interface: `void handle(ExecutionContext ctx, ExecutionChain chain)`
- Pipeline stages extracted from `AgentRuntimeOrchestrator`:
  1. `AdmissionMiddleware` (rate limit, concurrency, token budget)
  2. `PolicyCacheMiddleware` (fetch tenant policy, inject into context)
  3. `ApprovalCheckMiddleware` (high-risk tool gate)
  4. `ToolExecutionMiddleware` (invoke tool, capture result)
  5. `EventEmitMiddleware` (write ExecutionEvent + Outbox)
  6. `SnapshotMiddleware` (save checkpoint every N steps)
- `ExecutionPipelineFactory`: assembles stages per tenant policy (some tenants skip approval)
- `PipelineMetrics`: per-stage latency histogram

**Code sketch:**
```java
public interface ExecutionMiddleware {
    void handle(ExecutionContext ctx, ExecutionChain chain);
}

@Component
public class AgentRuntimeOrchestrator {
    private final ExecutionPipeline pipeline;

    public void runStep(Execution execution) {
        ExecutionContext ctx = ExecutionContext.from(execution);
        pipeline.execute(ctx);
        // ctx now contains events emitted, tool results, next state
    }
}
```

**Acceptance criteria:**
- [ ] All existing M7 tests pass without modification (pipeline is refactor, not behavior change)
- [ ] Per-stage latency visible in traces (M8 OTel integration)
- [ ] Tenant with `approval.disabled=true` policy skips `ApprovalCheckMiddleware` (measurable throughput gain)
- [ ] Pipeline stages can be unit-tested independently
- [ ] No FSM method exceeds 50 lines after extraction

**Dependencies:** M7 (all infrastructure must be in place before refactoring).

---

#### M12: ApprovalMode — FAST + BPMN (~1 week)

**Deliverables:**
- `ApprovalPolicyEngine.v2` (Engine): risk scoring matrix (tool type × data sensitivity × execution context)
- `ApprovalMode` enum: `FAST` (single approver, < 5 min SLA), `BPMN` (multi-step, configurable SLA)
- `FastApprovalHandler` (Core): assigns to round-robin `APPROVER` pool; auto-escalates to `TENANT_ADMIN` after SLA
- `BpmnApprovalHandler` (Core): starts Flowable process with `businessKey=ticketId`; delegates to `HumanTaskAssignmentDelegate`
- `ApprovalTicket.stage` transitions: `PENDING_FAST` → `PENDING_BPMN` (upgrade path), `APPROVED`, `REJECTED`, `ESCALATED`
- Frontend: Approval inbox page (`/approvals`) with filter by risk level, execution, tenant

**Acceptance criteria:**
- [ ] `FAST` approval: median resolution time < 3 minutes (synthetic test)
- [ ] `BPMN` approval: 3-step approval (engineer → lead → manager) completes end-to-end
- [ ] Escalation after SLA: ticket auto-reassigned, in-app notification sent, audit event written
- [ ] Reject decision: execution transitions to `REJECTED`, user sees reason in UI
- [ ] 100 concurrent approval tickets: no duplicate assignments, no lost decisions

**Dependencies:** M11 (pipeline must support approval gating); M7 workflow infrastructure.

---

#### M13: SSE Frontend + Execution Observability (~1 week)

**Deliverables:**
- `SseController` (Core): `GET /api/executions/{id}/events?lastSeq={n}` with replay
- `ExecutionReadModel` (Core): denormalized view of execution state + recent events
- Frontend `ExecutionMonitor` component: real-time event stream, state diagram, pause/resume/cancel buttons
- `ExecutionTimeline` component: visual timeline of events (tool calls, approvals, state changes)
- Dark mode support for execution monitor (follows system preference)

**Acceptance criteria:**
- [ ] Frontend connects to SSE, receives events within 200ms of Engine Outbox publish
- [ ] Reconnect after 30s disconnect: replays missed events, no duplicates
- [ ] Pause button: execution state changes to `PAUSED` within 1s
- [ ] Timeline shows 50 events without scroll lag
- [ ] Mobile responsive: timeline collapses to compact view < 768px

**Dependencies:** M7 SSE infrastructure; M12 for pause/resume/approve buttons.

---

### 4.4 Phase 3: v1 GA Sprint

**Goal:** Close the 22 v1 readiness blockers, pass soak test, and ship.

#### M14: Coverage + Security + CI (~1 week)

**Deliverables:**
- Raise 8 sub-80% modules to 80% minimum:
  - gateway (74% → 80%)
  - system (61% → 80%)
  - web (33% → 80%)
  - agent-config (67% → 80%)
  - integration (61% → 80%)
  - context (55% → 80%)
  - ops (69% → 80%)
  - admin (66% → 80%)
- Fix 6 P0 security blockers from v1-readiness Day-0 report
- Add `smoke.spec.ts` (E2E) referenced by CI but currently missing
- JaCoCo coverage provider for all 16 backend modules
- Frontend vitest coverage provider

**Acceptance criteria:**
- [ ] `mvn clean verify` passes with JaCoCo check on all modules
- [ ] `npm run test:coverage` in `schemaplexai-ui` passes with 80% threshold
- [ ] Security scan (OWASP dependency-check) zero HIGH/CRITICAL findings
- [ ] E2E smoke test passes against local docker-compose stack
- [ ] CI pipeline < 15 minutes end-to-end

**Dependencies:** All prior milestones (this is the cleanup phase).

---

#### M15: BPMN Frontend — Read-Only Render (~3 days)

**Deliverables:**
- Frontend `BpmnViewer` component: parses `agent-execution-approval.bpmn20.xml` via `bpmn-js`
- `WorkflowPage`: list of deployed workflows, click to view diagram
- Highlight active node in running workflow instances (color-coded by state)
- Export workflow as PNG/SVG

**Scope boundary:** No drag-and-drop editing. No new node creation. Read-only + zoom/pan.

**Acceptance criteria:**
- [ ] `agent-execution-approval.bpmn20.xml` renders correctly in browser
- [ ] Active node highlighted in real-time as workflow progresses
- [ ] Zoom in/out smooth at 60fps for diagrams with < 50 nodes
- [ ] PNG export resolution >= 300 DPI for documentation use
- [ ] No editor-related dependencies (keep bundle size minimal)

**Dependencies:** M12 (BPMN approval must be functional to have instances to display).

---

#### M16: Soak Test + Release (~4 days)

**Deliverables:**
- Execute 24-hour soak test (Section 3.3.4 specification)
- Fix any regressions discovered
- `CHANGELOG-v1.0.0.md`
- `DEPLOYMENT-v1.md`: runbook for k8s deployment with Helm chart
- Version tag `v1.0.0`

**Acceptance criteria:**
- [ ] Soak test: 50 executions, > 95% completion, zero event loss, zero inconsistent states
- [ ] Post-soak analysis checklist (Section 3.3.4) all green
- [ ] Helm chart deploys to fresh k8s cluster in < 10 minutes
- [ ] Rollback procedure documented and tested (deploy v1, rollback to pre-v1, verify)
- [ ] All 49 v1-readiness sub-dimensions score >= 8/10

**Dependencies:** M14, M15.

---

### 4.5 Milestone Dependency Graph

```
M8 (OTel) ──┬── M9 (Budget+Hash) ──┬── M11 (Pipeline) ──┬── M12 (Approval) ──┬── M13 (SSE UI)
            │                       │                     │                     │
            └───────────────────────┴─────────────────────┴─────────────────────┘
                                                                              │
M10 (Skills) ─────────────────────────────────────────────────────────────────┘
                                                                              │
                                                                     M14 (Coverage/Security)
                                                                              │
                                                                     M15 (BPMN UI)
                                                                              │
                                                                     M16 (Soak + Release)
```

**Critical path:** M8 → M9 → M11 → M12 → M13 → M14 → M16 (6 weeks).
M10 and M15 are parallelizable.

---

### 4.6 Risk Register

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| OpenTelemetry collector unavailable in target environment | Medium | High | Provide `OTEL_ENABLED=false` flag; fallback to Micrometer-only metrics |
| Flowable BPMN complexity exceeds 1-week estimate | Medium | High | Scope M12 to `FAST` approval first; BPMN as stretch goal |
| Frontend SSE replay performance degrades with > 10k events | Medium | Medium | Implement cursor-based pagination for replay; cap replay to last 1000 events |
| Security scan finds inherited vulnerability in dependency | High | Medium | Run `dependency-check` in CI on every PR; auto-create ticket for HIGH+ |
| Soak test reveals memory leak in Caffeine cache | Low | High | Monitor heap in soak test; if leak confirmed, switch to Redis-backed cache |

---

### 4.7 Definition of Done (per milestone)

Every milestone must satisfy:
1. All acceptance criteria checked
2. Code review by `code-reviewer` agent
3. Security review by `security-reviewer` agent if touching auth, events, or approvals
4. Tests pass: `mvn clean verify` + `npm run test` + E2E smoke
5. Wiki updated: `wiki/active-areas.md` and `wiki/log.md`同步
6. ADR written if introducing new pattern or changing existing contract

---

## Appendix A: Glossary

| Term | Definition |
|------|-----------|
| **Engine** | `schemaplexai-agent-engine` module; owns execution state machine |
| **Core** | Collective term for `system`, `web`, `ops`, `quality`, `task`, `integration` modules |
| **Outbox** | `sf_execution_outbox` table; Engine writes events here before MQ publish |
| **Inbox** | `sf_processed_event` table; Core consumers deduplicate via composite PK |
| **FAST approval** | Single-step human approval with short SLA |
| **BPMN approval** | Multi-step approval workflow via Flowable engine |
| **GATE_BLOCKED** | Execution state when policy cache miss prevents approval decision |

## Appendix B: Decision Log

| Date | Decision | ADR | Status |
|------|----------|-----|--------|
| 2026-05-08 | Pivot to Agent Execution Control Plane | — | Approved |
| 2026-05-08 | Launch narrative = Enterprise AI Orchestration (A+C) | — | Locked |
| 2026-05-08 | Notification v1 in-app only | ADR-013 | Approved |
| 2026-05-08 | Cost v1 PG short-path | ADR-012 | Approved |
| 2026-05-08 | JWT rotation SLA = 90d | ADR-011 | Approved |
| 2026-05-09 | Event-driven architecture with Outbox+Inbox | ADR-016 | Approved |
| 2026-05-09 | MyBatis-Plus optimistic locking for execution concurrency | ADR-017 | Approved |
| 2026-05-09 | Agent-engine Flyway schema ownership | ADR-014 | Approved |
| 2026-05-09 | Approval domain owned by Core, not Engine | ADR-015 | Approved |

