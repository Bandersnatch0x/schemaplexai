---
title: Technical Debt
type: project
source: Codebase analysis, docs/plans/
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [debt, todo, cleanup, stubs]
confidence: high
---

# Technical Debt

> One-sentence summary: Known gaps between current scaffolding and production-ready implementation, extracted from codebase state and documentation.

## Critical

| Item | Priority | Details |
|------|----------|---------|
| Zero test coverage | P0 | No tests exist across any module; TDD mandate in CLAUDE.md not yet implemented |
| Agent execution sync | P0 | `AgentExecutionEngine.startExecution()` is synchronous; needs async with thread pool |
| Gateway route mismatch | P1 | YAML routes (`/agents/**`) differ from Java Config (`/agent/**`) |

## High

| Item | Priority | Details |
|------|----------|---------|
| schemaplexai-admin empty | P1 | Placeholder module with no code |
| Many business stubs | P1 | Controllers and services exist but logic is minimal |
| No Flyway/Liquibase | P1 | Schema changes require manual SQL management |
| JWT filter not explored | P1 | Gateway JWT filter exists but implementation details unknown |

## Medium

| Item | Priority | Details |
|------|----------|---------|
| Missing WebSocket impl | P2 | `AgentWebSocketHandler` stubbed |
| SSE manager incomplete | P2 | `SseEmitterManager` and `AgentSseEmitter` need full review |
| Frontend pages empty | P2 | Many page components are likely stubs |
| ClickHouse schema | P2 | No ClickHouse init scripts explored |
| Milvus collection setup | P2 | Vector collection schema not yet defined |

## Low

| Item | Priority | Details |
|------|----------|---------|
| Checkstyle/SpotBugs | P3 | CI runs them but results not reviewed |
| Documentation gaps | P3 | Some specs need implementation alignment |

## Backlinks

- See `docs/plans/README.md` for active work
- See [[gaps]] for undocumented areas
- See [[roadmap]] for planned resolution
