---
change_id: v1-release-readiness
status: draft
created_at: 2026-05-05
author: Claude
---

# Proposal: SchemaPlexAI v1.0 Release Readiness

## 1. Problem Statement

SchemaPlexAI is an enterprise AI R&D collaboration platform. The codebase has:
- **Build**: Now compiles successfully (fixed duplicate TokenBudget, Lombok issues, code bugs in this session)
- **Tests**: Zero test coverage across all 16 modules
- **P0/P1 Issues**: 9 P0 + 22 P1 issues from code review (security, concurrency, data integrity)
- **Stub Logic**: 7 modules have CRUD scaffold but stub business logic
- **No CI/CD**: No automated build/test pipeline

**Goal**: Bring the project to a shippable v1.0 state with passing tests, resolved P0/P1 issues, and functional core modules.

## 2. Scope

### In Scope (v1.0 MVP)

**Phase 1: Build & Test Infrastructure** (Priority: P0)
- [ ] Fix all remaining compilation warnings
- [ ] Set up test framework (JUnit 5 + Mockito + Testcontainers)
- [ ] Write unit tests for schemaplexai-common, schemaplexai-model, schemaplexai-dao
- [ ] Write unit tests for schemaplexai-agent-engine (core state machine, tool registry, safety guard)

**Phase 2: P0 Issue Resolution** (Priority: P0)
- [ ] Fix DB driver mismatch (P0-001)
- [ ] Fix duplicate entity definitions (P0-005)
- [ ] Fix duplicate main class (P0-006)
- [ ] Fix JWT secret hardcoding (P1-001)
- [ ] Fix all 9 P0 blocking issues from CODE_REVIEW_REPORT.md

**Phase 3: Core Module Tests** (Priority: P1)
- [ ] Agent engine: state machine transitions, tool execution, loop detection
- [ ] Gateway: JWT filter, tenant filter, rate limiter
- [ ] System: auth, tenant, user CRUD
- [ ] Integration tests for agent-engine end-to-end flow

**Phase 4: P1 Issue Resolution** (Priority: P1)
- [ ] Security fixes (input validation, SQL injection prevention, XSS)
- [ ] Concurrency fixes (thread safety, race conditions)
- [ ] Data integrity fixes (transaction boundaries, idempotency)

**Phase 5: Documentation & Release** (Priority: P2)
- [ ] API documentation (Knife4j/OpenAPI)
- [ ] Deployment guide
- [ ] README with setup instructions
- [ ] CHANGELOG

### Out of Scope
- New feature development
- Performance optimization
- Frontend UI polish
- Production deployment configuration
- ClickHouse/Milvus/Redis infrastructure setup

## 3. Impact Assessment

| Module | Impact | Reason |
|--------|--------|--------|
| schemaplexai-common | HIGH | Foundation module, all others depend on it |
| schemaplexai-model | HIGH | Entity definitions, all services use it |
| schemaplexai-dao | HIGH | Data access layer |
| schemaplexai-agent-engine | HIGH | Core business logic |
| schemaplexai-gateway | HIGH | Entry point, security |
| schemaplexai-system | MEDIUM | Auth/tenant management |
| All other modules | MEDIUM | CRUD services, lower priority |
| schemaplexai-admin | LOW | Empty module, skip for v1.0 |

## 4. Success Criteria

- [ ] `mvn clean test` passes for all modules (0 failures)
- [ ] Test coverage >= 80% for core modules (common, model, dao, agent-engine, gateway, system)
- [ ] All P0 issues resolved
- [ ] All P1 security issues resolved
- [ ] Agent-engine end-to-end flow works (create agent → execute → complete)
- [ ] API documentation accessible at `/doc.html`

## 5. Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Test infrastructure setup complexity | HIGH | Use Spring Boot Test + Testcontainers |
| P0 issues may reveal deeper problems | MEDIUM | Fix incrementally, test each fix |
| Agent-engine integration complexity | HIGH | Focus on unit tests first, integration tests for critical paths |
| Time constraint | MEDIUM | Parallel work streams, skip non-critical modules |

## 6. Parallel Work Streams

To accelerate delivery, launch 4 parallel subagents:

1. **Test Infrastructure Agent**: Set up JUnit 5 + Mockito, write tests for common/model/dao
2. **P0 Fix Agent**: Fix all P0 issues from CODE_REVIEW_REPORT.md
3. **Agent Engine Test Agent**: Write comprehensive tests for agent-engine module
4. **Gateway/System Test Agent**: Write tests for gateway and system modules

Each agent reads `spec.md` before executing. Results merged in Deliver phase.
