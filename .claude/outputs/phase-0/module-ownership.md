# Phase 0.2: Core Module Ownership & Dependency Resolution

## Module Responsibility Matrix

| Domain | Owner Module | Key Classes | Rationale |
|--------|-------------|-------------|-----------|
| **Approval Domain** | `schemaplexai-quality` | `ApprovalTicket`, `ApprovalRequestConsumer`, `ApprovalDecisionOutbox`, `ApprovalEscalationService` | Existing `SfToolApprovalAmendment`, `AuditEvent`, `ToolApprovalServiceImpl` already in quality. Approval and audit are natural siblings. |
| **Read Model** | `schemaplexai-quality` | `ExecutionReadModel`, `ReadModelProjector`, `GapRecoveryJob` | Query-side persistence model, not interface-layer. Separated from web controllers. |
| **SSE Exposure** | `schemaplexai-web` | `SseController`, `SseSessionManager`, `SseReplayService` | HTTP endpoint ownership. Consumes data from quality read model. |
| **MQ Consumer (Approval)** | `schemaplexai-quality` | `ApprovalRequestConsumer`, `DeferredApprovalConsumer` | Colocated with approval domain for transactional consistency. |
| **MQ Consumer (Cost/Audit)** | `schemaplexai-ops` / `schemaplexai-quality` | `CostSyncConsumer`, `AuditEventConsumer` | Cost in ops (existing), Audit in quality (natural). |
| **Cost/Budget** | `schemaplexai-ops` | `CostService`, `BudgetService`, `BudgetGuard` | Existing `SfBudget`, `CostRecord`, `ClickHouseCostSyncService` already in ops. |
| **Execution Engine** | `schemaplexai-agent-engine` | `AgentStateMachine`, `ExecutionEventBus`, `OutboxService`, `ApprovalRequestProducer` | State machine and event production are engine's core. |
| **Workflow BPMN** | `schemaplexai-workflow` | `BpmnApprovalService`, `HumanTaskAssignmentDelegate` | Flowable engine integration stays in workflow module. |

## Dependency Changes

### `schemaplexai-quality/pom.xml`

**Added:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

**Status:** ✅ Applied.

### Cross-Module Dependencies (allowed)

```
web ──reads──> quality (ReadModel)
quality ──publishes──> MQ ──consumed by──> quality, ops
agent-engine ──publishes──> MQ ──consumed by──> quality, ops
workflow ──updates──> quality (ApprovalTicket)
```

**Forbidden:**
- quality → agent-engine (HTTP callback)
- workflow → agent-engine (direct state mutation)
- Any module bypassing MQ to write another module's tables

## Verification

- [ ] `mvn dependency:tree -pl schemaplexai-quality` shows `spring-boot-starter-amqp`
- [ ] `mvn compile -pl schemaplexai-quality` succeeds
- [ ] No circular dependencies in `mvn dependency:tree`
- [ ] All approval-related classes compile in quality module
