# Phase 2-3 Implementation Plan

> Part of the Agent Execution Control Plane pivot.
> Parent plan: `C:\Users\amsterdam\.claude\plans\twinkling-mixing-flute.md`

---

## Current State (End of Phase 1)

Phase 1 delivered:
- 7 service stubs implemented (ExecutionEventService, ExecutionConcurrencyService, ExecutionEventBuffer, SchemaValidator, ApprovalRequestConsumer, ApprovalDecisionValidator, SseReplayService)
- 21 tests all GREEN (13 previous RED failures + 5 errors now passing)
- Flyway migration `V2026_05_09__execution_control_plane_tables.sql` applied
- 4 new entities: ExecutionEvent, ExecutionOutbox, ProcessedEvent, ApprovalTicket
- 2 shared events in model module: ApprovalRequestEvent, ApprovalDecisionEvent
- `SfAgentExecution` extended with `version` and `lastEventSeq` fields

---

## Phase 2: State Machine & Approval Engine (Weeks 2-3)

### Objective

Implement the execution state machine and the approval policy/decision loop. At Phase 2 completion, the system can execute agents, pause for approval, and resume after human decision.

### Milestones

#### M2.1: State Machine Core (Week 2, Days 1-4)

**Scope:** Full `AgentStateMachine` implementation with all 8 states and their transitions.

| State | Valid Transitions | Trigger |
|-------|-------------------|---------|
| INITIALIZING | RUNNING, FAILED | Admission check result |
| RUNNING | PAUSED, GATE_BLOCKED, COMPLETED, FAILED, CANCELLED | Tool call approval needed, completion, error, user cancel |
| PAUSED | RUNNING, CANCELLED | Approval granted, user cancel |
| GATE_BLOCKED | PAUSED, CANCELLED | Core recovery (deferred approval activated), user cancel |
| COMPLETED | (terminal) | — |
| FAILED | (terminal) | — |
| CANCELLED | (terminal) | — |
| REJECTED | (terminal) | — |

**Tests:**
- Unit tests for every valid transition (TDD: RED → GREEN)
- Unit tests for every invalid transition (must throw IllegalStateTransitionException)
- Integration test: full INITIALIZING → RUNNING → PAUSED → RUNNING → COMPLETED cycle

**Files:**
- `schemaplexai-agent-engine/.../state/ExecutionState.java` (enum)
- `schemaplexai-agent-engine/.../state/AgentStateMachine.java`
- `schemaplexai-agent-engine/.../state/IllegalStateTransitionException.java`

#### M2.2: ExecutionEvent Production (Week 2, Days 3-5)

**Scope:** Event + Outbox atomic write, Outbox publisher, event seq management.

**Tests:**
- Transactional atomicity: event + outbox written in same TX (verify rollback scenarios)
- Outbox publisher: polls unpublished entries, publishes to MQ, marks published_at
- Retry logic: exponential backoff 1s/2s/4s/8s/16s, max 5 retries
- DEAD state: after 5 failures, event marked DEAD, alert triggered

**Files:**
- `schemaplexai-agent-engine/.../service/ExecutionEventService.java` (implement from stub)
- `schemaplexai-agent-engine/.../service/OutboxService.java`
- `schemaplexai-agent-engine/.../service/OutboxPublisher.java` (scheduled job)
- `schemaplexai-agent-engine/.../config/RabbitMqConfig.java`

#### M2.3: Approval Policy Engine (Week 2, Days 5-7)

**Scope:** Policy evaluation, local cache, fail-closed degradation.

**Tests:**
- Policy cache hit: returns policy from Caffeine in < 1ms
- Policy cache miss with Core reachable: fetches, caches, returns within 2s
- Policy cache miss with Core unreachable: returns FAIL_CLOSED default, execution enters GATE_BLOCKED
- Policy cache TTL: entry expires after 5 min, re-fetched on next access
- Policy update event: tenant.policy.updated → cache invalidation

**Files:**
- `schemaplexai-agent-engine/.../policy/ApprovalPolicyEngine.java`
- `schemaplexai-agent-engine/.../policy/LocalPolicyCache.java`
- `schemaplexai-agent-engine/.../policy/PolicyCacheMissException.java`
- `schemaplexai-agent-engine/.../policy/RiskLevel.java` (enum: LOW, MEDIUM, HIGH, CRITICAL)

#### M2.4: Approval Decision Loop (Week 3, Days 1-4)

**Scope:** Engine requests approval, Core creates ticket, Core decides, Engine resumes.

**Tests:**
- Engine detects high-risk tool call → produces ApprovalRequestEvent to Outbox → MQ
- Core ApprovalRequestConsumer creates ApprovalTicket with correct stage (PENDING_FAST or PENDING_BPMN)
- Idempotency: duplicate approvalRequestId → ACK and skip
- Core produces ApprovalDecisionEvent → MQ → Engine validates version → resumes
- Version mismatch: Engine rejects decision with VERSION_CONFLICT
- Multiple approvals per execution: different triggeringSeq → different tickets, no interference

**Files:**
- `schemaplexai-agent-engine/.../service/AgentRuntimeOrchestrator.java`
- `schemaplexai-agent-engine/.../approval/ApprovalRequestProducer.java`
- `schemaplexai-quality/.../service/ApprovalRequestConsumer.java` (implement from stub)
- `schemaplexai-quality/.../service/ApprovalDecisionOutbox.java`
- `schemaplexai-quality/.../service/ApprovalTicketService.java`
- `schemaplexai-quality/.../service/ApprovalDecisionValidator.java` (implement from stub)

#### M2.5: GATE_BLOCKED Recovery Flow (Week 3, Days 3-5)

**Scope:** Deferred approval handling when Core is unreachable.

**Tests:**
- Core unreachable → GATE_BLOCKED → DeferredApprovalEvent queued
- Core recovery → DeferredApprovalConsumer activates → ApprovalTicket(deferred=true) created
- Engine receives ApprovalTicketCreated(deferred) → validates state == GATE_BLOCKED → transitions to PAUSED
- State mismatch (execution no longer in GATE_BLOCKED) → event silently dropped, audit logged

**Files:**
- `schemaplexai-agent-engine/.../approval/DeferredApprovalHandler.java`
- `schemaplexai-quality/.../service/DeferredApprovalConsumer.java`

### Phase 2 Acceptance Criteria

- [ ] All 8 execution states reachable via valid transitions
- [ ] Invalid transitions throw IllegalStateTransitionException with clear message
- [ ] 100% of ExecutionEvents have corresponding Outbox entry (no lost events)
- [ ] Outbox retry: events published within 31s (1+2+4+8+16) or marked DEAD
- [ ] Policy cache returns in < 1ms on hit, < 2s on miss with Core available
- [ ] GATE_BLOCKED → PAUSED recovery flow completes within 10s of Core recovery
- [ ] Approval idempotency: 0 duplicate tickets created under MQ redelivery
- [ ] Version conflict: 100% detection rate for stale commands
- [ ] Test coverage >= 85% on new code (state machine, event service, approval loop)
- [ ] Zero cross-module direct calls (all communication via MQ)

### Phase 2 Dependencies

| Dependency | Status | Risk |
|------------|--------|------|
| RabbitMQ running in dev | Available | Low |
| PostgreSQL with Flyway migrations applied | Available | Low |
| Redis running in dev | Available | Low |
| MQ configuration (exchanges, queues, bindings) | Needs creation | Medium |
| Core module for policy endpoint | Needs implementation | Medium |

### Phase 2 Estimated Effort

- State machine: 3 dev-days
- Event production + Outbox: 3 dev-days
- Policy engine + cache: 2 dev-days
- Approval decision loop: 4 dev-days
- GATE_BLOCKED recovery: 2 dev-days
- **Total: 14 dev-days (2 developers x 7 days, or 1 developer x 14 days)**

---

## Phase 3: BPMN Workflow Integration (Weeks 4-5)

### Objective

Integrate Flowable BPMN for complex multi-step approval workflows. At Phase 3 completion, the system supports single-step FAST approval (Phase 2) + multi-step BPMN approval with escalation chains.

### Milestones

#### M3.1: BPMN Process Deployment (Week 4, Days 1-2)

**Scope:** Deploy and test the `agent-execution-approval.bpmn20.xml` process definition.

**Process definition:**
```
Start → Assign Approver → [Decision Gateway]
  ├─ APPROVE → Notify Engine → End
  ├─ REJECT  → Notify Engine → End
  └─ ESCALATE → Assign Senior Approver → [Decision Gateway]
       ├─ APPROVE → Notify Engine → End
       └─ REJECT  → Notify Engine → End
```

**Tests:**
- BPMN deployed on Flowable engine startup
- Process instance started with businessKey=ticketId
- Human task assigned to correct approver role
- Task completion triggers process flow continuation
- Process end event publishes ApprovalDecisionEvent via Outbox

**Files:**
- `schemaplexai-workflow/src/main/resources/processes/agent-execution-approval.bpmn20.xml`
- `schemaplexai-workflow/.../service/BpmnApprovalDeployer.java`
- `schemaplexai-workflow/.../delegate/NotifyEngineDelegate.java`
- `schemaplexai-workflow/.../delegate/HumanTaskAssignmentDelegate.java`

#### M3.2: FAST → BPMN Upgrade (Week 4, Days 3-4)

**Scope:** Single-ticket rule: when a FAST approval escalates, the same ticket upgrades to BPMN stage.

**Tests:**
- FAST approval ticket created with stage=PENDING_FAST
- APPROVER escalates → same ticket updates stage to PENDING_BPMN, workflow instance started
- Workflow completion → same ticket resolved (stage=APPROVED or REJECTED)
- No duplicate ticket creation during upgrade

**Files:**
- `schemaplexai-quality/.../service/ApprovalEscalationService.java`
- `schemaplexai-workflow/.../service/ApprovalWorkflowBridge.java`

#### M3.3: Multi-Step Approval Chains (Week 4, Days 5-7)

**Scope:** BPMN flows with multiple sequential approval steps.

**Example chain:** Developer → Tech Lead → Architect → Security Review

**Tests:**
- Multi-step BPMN with 4 sequential human tasks
- Each task completion advances to next step
- Mid-chain rejection terminates process immediately
- Complete approval chain → final APPROVE decision to Engine

**Files:**
- `agent-execution-approval.bpmn20.xml` (extended with multi-step support)
- `schemaplexai-workflow/.../delegate/ChainAdvancementDelegate.java`

#### M3.4: Escalation Policies (Week 5, Days 1-3)

**Scope:** SLA-based and manual escalation with configurable policies.

**Escalation triggers:**
1. APPROVER does not respond within SLA window (configurable per tenant, default 4h)
2. APPROVER explicitly escalates
3. Execution risk level exceeds approver authority threshold

**Tests:**
- Timer boundary event fires after SLA window → escalates to next level
- Manual escalation: APPROVER clicks "Escalate" → task reassigned to TENANT_ADMIN pool
- Level 2 escalation (TENANT_ADMIN no response 24h) → automatic REJECTION
- Escalation event logged in AuditEvent with ESCALATION_TRIGGERED type
- In-app notification sent to escalated approver group

**Files:**
- `schemaplexai-workflow/src/main/resources/processes/agent-execution-approval.bpmn20.xml` (timer events)
- `schemaplexai-quality/.../service/ApprovalEscalationService.java` (extended)
- `schemaplexai-quality/.../service/EscalationPolicyService.java`
- `schemaplexai-quality/.../job/EscalationTimeoutJob.java` (scheduled check)

#### M3.5: End-to-End Workflow Integration Tests (Week 5, Days 4-5)

**Scope:** Full integration tests spanning Engine → MQ → Core → Workflow → Core → MQ → Engine.

**Test scenarios:**
1. FAST approval: execution pauses, single approver approves, execution resumes
2. FAST with escalation: FAST → escalate → BPMN → senior approves → resumes
3. BPMN multi-step: 3-step chain, all approve → resumes
4. BPMN mid-chain reject: step 2 rejects → execution rejected, no further steps
5. SLA escalation: approver times out → auto-escalate → senior approves → resumes
6. Version conflict during approval: engine state changed → decision rejected → client retries

### Phase 3 Acceptance Criteria

- [ ] BPMN process deployed and validated on Flowable engine startup
- [ ] FAST → BPMN upgrade: single ticket, no duplicates
- [ ] Multi-step chain: up to 5 sequential approvers supported
- [ ] SLA escalation: timer fires within 1 minute of SLA window expiry
- [ ] Manual escalation: approver can escalate any pending task
- [ ] Level 2 timeout: automatic rejection after 24h no response
- [ ] All escalation events appear in audit trail
- [ ] In-app notifications sent for escalation and assignment events
- [ ] Test coverage >= 80% on workflow module
- [ ] All 6 E2E scenarios pass in CI pipeline

### Phase 3 Dependencies

| Dependency | Status | Risk |
|------------|--------|------|
| Phase 2 completion | In progress | Medium |
| Flowable engine configured | Available | Low |
| BPMN process definition | Needs creation | Low |
| Tenant-level SLA policy config | Needs implementation | Medium |
| Notification service (in-app) | Available | Low |

### Phase 3 Estimated Effort

- BPMN deployment + basic flow: 2 dev-days
- FAST → BPMN upgrade: 2 dev-days
- Multi-step chains: 3 dev-days
- Escalation policies: 3 dev-days
- E2E integration tests: 2 dev-days
- **Total: 12 dev-days (2 developers x 6 days, or 1 developer x 12 days)**

---

## Phase 2+3 Combined Timeline

```
Week 1: Phase 1 (complete: stubs + GREEN tests)
Week 2: Phase 2 M2.1-M2.3 (state machine, event production, policy engine)
Week 3: Phase 2 M2.4-M2.5 (approval loop, GATE_BLOCKED recovery)
Week 4: Phase 3 M3.1-M3.3 (BPMN deploy, FAST→BPMN, multi-step chains)
Week 5: Phase 3 M3.4-M3.5 (escalation policies, E2E tests)
Week 6: Buffer + hardening (soak tests, chaos tests, security review)
```

## Total Estimated Effort

| Phase | Dev-Days | Risk Level |
|-------|----------|------------|
| Phase 1 (complete) | — | — |
| Phase 2 | 14 | Medium |
| Phase 3 | 12 | Medium |
| Buffer/Hardening | 5 | Low |
| **Total remaining** | **31 dev-days** | |

With 2 developers: ~3.5 weeks to Phase 3 completion + 1 week buffer = ~4.5 weeks.

## Cross-Cutting Concerns

### Throughout Both Phases

- **TDD mandatory**: Every milestone starts with RED tests, implements to GREEN, refactors
- **Code review**: After each milestone, code-reviewer agent reviews changes
- **Security review**: Before Phase 2 completion, security-reviewer audits approval authorization
- **Contract tests**: Spring Cloud Contract tests added alongside each MQ event schema change
- **Documentation**: ADR updates for any architectural decisions made during implementation