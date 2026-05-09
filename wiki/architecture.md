<!-- AUTO-GENERATED: sync-wiki.sh at 2026-05-09T07:07:51Z -->

---
title: System Architecture
type: architecture
source: CLAUDE.md, docs/designs/system-architecture.md, pom.xml
creation_date: 2026-04-30
update_date: 2026-05-09
tags: [architecture, microservices, spring-boot, patterns, control-plane, pivot]
confidence: high
---

# System Architecture

> One-sentence summary: SchemaPlexAI has pivoted to an "Agent Execution Control Plane" — 4 deployable units (Gateway, Agent-Engine, Workflow, Core monolith) communicating via Outbox + MQ over PostgreSQL.

## Pivot: Agent Execution Control Plane (2026-05-09)

The platform has pivoted from a general "AI R&D collaboration platform" to a focused "Agent Execution Control Plane." Core value: safely launch, observe, control, and audit agent executions.

### Deployable Units (4 units, down from 12 microservices)

| Unit | Modules | Port | Role |
|------|---------|------|------|
| **Gateway** | gateway | 8080 | JWT, tenant, rate limit, routing |
| **Agent-Engine** | agent-engine | 8084 | State machine, execution orchestration, event production (Outbox) |
| **Workflow** | workflow | 8087 | Flowable BPMN for complex multi-step approvals |
| **Core** | system + web + ops + quality + task + integration + context + agent-config + common + model + dao | 8081-8091 | Modular monolith: CRUD, web controllers, SSE, cost, audit, approval tickets, MQ consumers |

### Event Flow (4-Step Product Line)

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

### State Ownership Matrix

| Data | Store | Truth Source | Owner | Notes |
|------|-------|--------------|-------|-------|
| `Execution` (state, version, lastEventSeq) | PG | YES | Agent-Engine | Version optimistic lock |
| `ExecutionEvent` (immutable event stream) | PG | YES | Agent-Engine | Append-only |
| `ExecutionOutbox` | PG | YES | Agent-Engine | At-least-once MQ publish |
| `ProcessedEvent` (inbox dedup) | PG | YES | Core | Exactly-once per consumer |
| `ApprovalTicket` | PG | YES | Core | Unified truth for all approvals |
| `AuditEvent` | PG | YES | Core | Projected from ExecutionEvent |
| `CostRecord` | PG | YES | Core | Projected from ExecutionEvent |
| `Budget` | PG | YES | Core | Tenant-level budget config |
| `TenantPolicy` | PG | YES | Core | Approval policies, tool whitelist |
| `WorkflowInstance` | PG | YES | Workflow | Flowable native tables |

### Core Principles

1. **PG = truth source.** All business state primary copies in PostgreSQL.
2. **Redis = hot cache + recovery.** Rebuildable from PG.
3. **Engine = event producer only.** Never directly writes Core tables.
4. **Core = event consumer + read model builder.** Maintains query views.
5. **Single state owner per aggregate.** Execution owned by Engine. Approval owned by Core.

### Infrastructure Changes

| Component | Status | Rationale |
|-----------|--------|-----------|
| PostgreSQL | KEEP | Primary data store |
| Redis | KEEP | Hot cache, locks, sessions |
| RabbitMQ | KEEP | Event bus Engine ↔ Core |
| Flowable | KEEP | BPMN for complex approvals |
| ClickHouse | v1.1 | Cost analytics deferred |
| Elasticsearch | REMOVE | No active code references |
| Prometheus/Grafana | REMOVE | No metrics endpoints configured |
| Milvus | REMOVE | RAG not core to control plane |
| MinIO | DEFER | File storage not in v1 MVP |

## Original Architecture (pre-pivot)

### Service Topology

```
                      +-------------+
                      |   Client    |
                      +------+------+
                             |
                      +------v------+
                      |  Gateway    |  Port 8080
                      |  (JWT/Rate  |  Spring Cloud Gateway
                      |   Limit/CORS|
                      +------+------+
                             |
        +--------+-----------+-----------+--------+
        |        |           |           |        |
   +----v----+ +-v-----+ +--v---+ +----v----+ +--v---+
   | system  | |  web  | |agent-| | context | | spec |
   | 8081    | | 8082  | |engine| |  8085   | | 8086 |
   +---------+ +-------+ +------+ +---------+ +------+
   | tenant  | |controllers|     | |  RAG    | | docs |
   | auth    | | SSE/WS  |      | | vectors | |review|
   | RBAC    | | Knife4j |      | | MinIO   | |      |
   +---------+ +-------+ +------+ +---------+ +------+
        |        |           |           |        |
   +----v----+ +-v-----+ +--v---+ +----v----+ +--v---+
   | workflow| |agent- | |quality| |integration| | ops  |
   |  8087   | |config | | 8090  | |  8088   | | 8089 |
   +---------+ +-------+ +------+ +---------+ +------+
   | Flowable| |        | | gates | | GitHub  | |cost  |
   | BPMN    | |        | | audit | | Jenkins | |notify|
   +---------+ +-------+ +------+ +---------+ +------+
        |
   +----v----+
   |  task   |  8091
   +---------+
   | MQ jobs |
   +---------+
```

## Module Dependency Chain

```
schemaplexai-common  (no internal deps)
    ↓
schemaplexai-model   → common
    ↓
schemaplexai-dao     → common, model
    ↓
schemaplexai-task    → common, model, dao

All other services → common, model, dao
```

`schemaplexai-admin` is a placeholder aggregator module with no code yet.

## Communication Patterns

1. **Gateway → Services**: HTTP REST via Spring Cloud Gateway (load-balanced `lb://` in Java Config)
2. **Web → Domain Services**: Internal REST calls (schemaplexai-web hosts most controllers)
3. **Services → DB**: MyBatis-Plus via `schemaplexai-dao`
4. **Async**: RabbitMQ for MQ consumers (task module)
5. **Real-time**: SSE (`/sse/subscribe/{clientId}`) and WebSocket (`/ws/**`)
6. **Cache**: Redis (session, rate limit, chat memory L1)
7. **Vector**: Milvus for RAG embeddings
8. **Object**: MinIO for file storage
9. **Analytics**: ClickHouse for cost analytics
10. **Search**: Elasticsearch for logs/audit

## Key Patterns

- **BaseEntity**: All entities extend `BaseEntity` with `id` (ASSIGN_ID), `tenantId`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deleted`
- **BaseMapperX**: All mappers extend MyBatis-Plus `BaseMapper<T>`
- **BaseController**: All controllers extend `BaseController` with `success()`/`error()` helpers returning `Result<T>`
- **TenantLineInterceptor**: Auto-injects `tenant_id` into SQL; global tables excluded
- **BaseException**: Business errors carry integer code + message

## Infrastructure Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| OLTP DB | PostgreSQL 16 | Primary transactional data |
| Cache | Redis 7 | Sessions, rate limit, chat memory L1 |
| MQ | RabbitMQ 3.12 | Async job processing |
| Vector DB | Milvus 2.3.5 | RAG embeddings |
| Object Store | MinIO | File/document storage |
| Analytics | ClickHouse 24 | Cost analytics warehouse |
| Search | Elasticsearch 8 | Logs and audit search |
| Metrics | Prometheus + Grafana | Observability |
| Tracing | Jaeger | Distributed tracing |
| BPMN | Flowable 7 | Workflow engine |

## Backlinks

- Data model in [[data-model]]
- Routes in [[routes]]
- Dependencies in [[dependencies]]
- Decisions in [[decisions]]
