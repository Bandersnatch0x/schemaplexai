---
topic: approval-domain-module-ownership
stage: decision
version: v1.0
status: 已批准
supersedes: ""
---

# ADR-015: Module Ownership for Approval Domain in quality

> **日期**: 2026-05-09
> **决策人**: 架构评审委员会
> **状态**: 已批准

---

## 背景

The pivot to "Agent Execution Control Plane" introduces a full approval domain:
- `ApprovalTicket` entity — unified truth for all approval requests
- `ApprovalRequestConsumer` — consumes approval requests from Engine via MQ
- `ApprovalDecisionOutbox` — publishes approval decisions back to Engine
- `ApprovalEscalationService` — handles SLA-based and manual escalations
- `ExecutionReadModel` — query-side persistence for execution state
- `ReadModelProjector` — projects events into read models
- `GapRecoveryJob` — fills event gaps in read models

A decision is needed on which module owns these components. The current module landscape:
- `schemaplexai-quality` (8090) — currently holds `SfToolApprovalAmendment`, `AuditEvent`, `ToolApprovalServiceImpl`
- `schemaplexai-system` (8081) — tenant, user, role, permission management
- `schemaplexai-web` (8082) — controllers, SSE, WebSocket endpoints
- `schemaplexai-ops` (8089) — cost, budget, artifacts

## 决策

**The approval domain is owned by `schemaplexai-quality`** as the primary module, with read model exposure delegated to `schemaplexai-web` for HTTP endpoints.

### 实现细节

1. **quality module scope**
   - `ApprovalTicket` entity + mapper
   - `ApprovalRequestConsumer` — MQ consumer for `approval.requests`
   - `DeferredApprovalConsumer` — MQ consumer for `approval.requests.deferred`
   - `ApprovalDecisionOutbox` — publishes `ApprovalDecisionEvent` to MQ
   - `ApprovalEscalationService` — SLA tracking and escalation logic
   - `ApprovalDecisionValidator` — validates decision version and execution version
   - `ExecutionReadModel` — query-side read model (NOT an HTTP controller)
   - `ReadModelProjector` — event-to-read-model projection
   - `GapRecoveryJob` — scheduled job for event gap detection and fill

2. **web module scope (SSE/HTTP exposure)**
   - `SseController` — SSE endpoint for frontend connections
   - `SseSessionManager` — Redis-backed session tracking
   - `SseReplayService` — replays missed events on reconnect
   - `ExecutionWebController`, `ApprovalWebController` — REST endpoints (consume from quality read model)

3. **Dependency rules**
   - web reads from quality (ReadModel) — allowed
   - quality publishes to MQ, consumed by quality and ops — allowed
   - quality NEVER calls agent-engine directly (no HTTP callback) — forbidden
   - workflow updates ApprovalTicket in quality — allowed (BPMN completion)

4. **New dependency**
   - `schemaplexai-quality/pom.xml` requires `spring-boot-starter-amqp` for MQ consumers

## 理由

- **Natural sibling**: `SfToolApprovalAmendment`, `AuditEvent`, and `ToolApprovalServiceImpl` already exist in quality. Approval and audit are naturally co-located.
- **Single responsibility**: quality owns "gates and audit" — approval is a gate mechanism, read model is the query side of that gate
- **Separation of concerns**: web owns HTTP exposure (SSE, controllers), quality owns domain logic and persistence. No domain logic in web.
- **Transactional consistency**: ApprovalTicket creation and AuditEvent writing happen in the same module, enabling same-transaction writes.

## 影响

- **对现有代码的影响**
  - `schemaplexai-quality/pom.xml`: add `spring-boot-starter-amqp` (done)
  - New entities: `ApprovalTicket`
  - New consumers: `ApprovalRequestConsumer`, `DeferredApprovalConsumer`
  - New services: `ApprovalEscalationService`, `ExecutionReadModel`, `ReadModelProjector`
  - New job: `GapRecoveryJob`

- **对模块边界的影响**
  - Read model is NOT a separate module — it lives in quality as a package
  - web depends on quality for read model access
  - No circular dependency introduced

- **对测试的影响**
  - Approval consumer tests require embedded MQ or mock
  - Read model tests require PG testcontainer
  - SSE replay tests in web verify correctness against quality read model

## 替代方案

| 方案 | 优点 | 缺点 | 结果 |
|------|------|------|------|
| Approval in system module | Close to RBAC | System already handles auth/user/tenant — too many concerns | 拒绝 |
| Approval in agent-engine | No MQ boundary | Engine would own approval state AND execution state — violates single-state-owner principle | 拒绝 |
| Dedicated approval module | Cleanest separation | 12th module for a single domain — premature extraction | 拒绝 |
| **Approval in quality (本方案)** | Natural sibling to audit, existing infra | AMQP dependency addition needed | **采纳** |
| Read model as separate module | Pure CQRS | Overkill for v1, adds deployment complexity | 拒绝 |

## 相关文档

- `.claude/outputs/phase-0/module-ownership.md`
- `wiki/architecture.md`
- ADR-008: Domain Decomposition