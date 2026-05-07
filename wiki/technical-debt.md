---
title: Technical Debt
type: project
source: Codebase analysis, docs/plans/
creation_date: 2026-04-30
update_date: 2026-05-07
tags: [debt, todo, cleanup, stubs]
confidence: high
---

# Technical Debt

> One-sentence summary: Current gaps between implemented scaffolding and production-ready AI capabilities, plus known architectural limitations.

## Critical (Blocking Production)

| Item | Priority | Details |
|------|----------|---------|
| SSE Event Bus single-node only | P0 | `ExecutionEventBus` holds SSE emitters in local `ConcurrentHashMap`. Horizontal scaling requires Redis pub/sub or sticky sessions. See [[services/execution-event-bus]] |
| Tool Use sandbox missing | P0 | `ToolCallingStateHandler` + `ToolRegistry` implemented, but no `SandboxProvider` execution for SCRIPT/HTTP tools |

## High (Major Feature Gaps)

| Item | Priority | Details |
|------|----------|---------|
| Multi-Agent orchestration | P1 | Single-Agent execution only; no Coordinator/Swarm/Crew |
| HITL approval flow incomplete | P1 | `PAUSED` state exists but no UI approval/reject/resume flow |
| Long-term memory missing | P1 | Chat Memory (Redis L1) complete; no vector long-term memory |
| Reflection shallow | P1 | `ReflectingStateHandler` exists but reflection prompt is basic; no structured rubric |
| Exception handling shallow | P1 | `Failed`/`Retrying` states exist but no fallback chain or circuit breaker |
| schemaplexai-admin empty | P1 | Placeholder aggregator module with no code |

## Medium (Quality & Completeness)

| Item | Priority | Details |
|------|----------|---------|
| No Flyway/Liquibase | P2 | Schema changes managed via manual SQL scripts |
| Parallelization absent | P2 | Single-threaded execution; no parallel sub-task scheduling |
| Prompt Chaining absent | P2 | No chain-of-prompt pipeline |
| Goal Setting absent | P2 | No goal tracking or completion detection |
| MCP integration stub | P2 | `McpServerController` is a stub; no FastMCP integration |
| Frontend page depth | P2 | 16 pages exist but many are UI shells |
| Resource Optimization skeleton | P2 | `TokenBudget` + `AdmissionControl` have scaffolding |
| Evaluation framework skeleton | P2 | `ToolErrorCategory` added; no LLM-as-Judge |

## Low (Polish & Enhancement)

| Item | Priority | Details |
|------|----------|---------|
| Checkstyle/SpotBugs | P3 | CI runs them but results not actively reviewed |
| A2A protocol | P3 | No Agent Card or streaming inter-agent communication |
| Learning/Adaptation | P3 | Shadow Review writes but has no read path |
| Prioritization | P3 | No task queue priority or SLA guarantees |
| Exploration | P3 | No Agent Laboratory or research automation |

## Resolved (Fixed on 2026-05-07)

| Item | Resolution | Note |
|------|------------|------|
| Zero test coverage | 2026-05-07 | 967/967 tests passing in agent-engine |
| Agent execution async | 2026-05-07 | `AgentExecutionAsyncConfig` + `AgentExecuteDispatcher` |
| Cross-service integration tests | 2026-05-07 | Removed boundary-violating tests |
| Flaky `RetryingStateHandlerTest` | 2026-05-07 | Fixed via Mockito.reset |
| Gateway route mismatch | 2026-05-07 | Routes aligned |
| ClickHouse schema | 2026-05-06 | 4 tables + 2 materialized views |
| Milvus collection setup | 2026-05-06 | `knowledge_doc_embedding` with IVF_FLAT/COSINE |
| Workflow node executors | 2026-05-06 | 7 executors: HTTP, SCRIPT, START, END, AI_MODEL, TOOL_CALL, CONDITION |
| Wiki documentation gaps | 2026-05-07 | 68 service + 34 controller + all entity pages |
| agents-sdk-2026-alignment | 2026-05-07 | `SandboxProvider` + `AgentsManifest` + `SfAgentMapper.findByNameAndTenant` |
| **RAG wired into agent-engine** | 2026-05-07 | `ContextInjector.inject()` now calls `retrieveRagContext()` via `TenantContextHolder`; 13 tests added |
| **Guardrails integrated** | 2026-05-07 | `GuardrailsConfig` + `ThinkingStateHandler.validateInput()` + 101 new tests |
| **Planning mode implemented** | 2026-05-07 | `PLANNING` state + `PlanningStateHandler` + `SubTask`/`SubTaskPlan` + `ThinkingStateHandler` plan progression; 35 tests added |

## Backlinks

- See `docs/plans/README.md` for active work
- See [[gaps]] for undocumented areas (resolved)
- See [[roadmap]] for planned resolution timeline
- See [[architecture-gap-analysis]] for Agentic pattern gap matrix
