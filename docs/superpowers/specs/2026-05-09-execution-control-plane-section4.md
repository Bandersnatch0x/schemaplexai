# Section 4: Implementation Phases & Milestones

## Phase 1: Foundation (Weeks 1-2)

- Deployable units: Gateway + Core (system, web, ops, quality, task)
- DB migrations for all v1 tables
- Basic auth, tenant isolation, rate limiting
- Execution CRUD via Web controllers
- MQ infrastructure (RabbitMQ topics, queues, DLQ)

## Phase 2: Engine Integration (Weeks 3-4)

- Agent-Engine state machine + admission service
- Outbox/Inbox pattern implementation
- Event streaming (SSE) from Core
- Approval request/response loop (FAST path)
- Cost projection and budget alerts

## Phase 3: Workflow & Resilience (Weeks 5-6)

- BPMN approval deployer + human task delegation
- Policy cache + fail-closed degradation
- Execution snapshot save/restore
- Gap recovery + dead letter handling
- Secret masking + audit immutability

## Phase 4: Hardening (Weeks 7-8)

- Contract tests + chaos testing
- Load testing (admission, SSE, MQ)
- 24h soak test
- Security audit + penetration testing
- Documentation + operator runbooks

## Milestones

| Milestone | Date | Deliverable |
|-----------|------|-------------|
| M0 | Week 0 | Architecture plan approved |
| M1 | Week 2 | Core services deployed, auth working |
| M2 | Week 4 | Engine executing, events flowing |
| M3 | Week 6 | Approvals working, resilience proven |
| M4 | Week 8 | Production ready, security signed off |
