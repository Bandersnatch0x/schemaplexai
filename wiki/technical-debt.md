<!-- AUTO-GENERATED: manual-maintained wiki at 2026-05-08T01:50:00Z -->
---
title: Technical Debt
type: project
source: Codebase analysis, docs/plans/
creation_date: 2026-04-30
update_date: 2026-05-08
tags: [debt, todo, cleanup, stubs]
confidence: high
---

# Technical Debt

> One-sentence summary: Current gaps between implemented scaffolding and production-ready AI capabilities, plus known architectural limitations.

## Critical (Blocking Production)

| Item | Priority | Details |
|------|----------|---------|
| SSE Event Bus single-node only | P0 | `ExecutionEventBus` holds SSE emitters in local `ConcurrentHashMap`. Horizontal scaling requires Redis pub/sub or sticky sessions. See [[services/execution-event-bus]] |

## High (Major Feature Gaps)

| Item | Priority | Details |
|------|----------|---------|
| Multi-Agent orchestration | P1 | Single-Agent execution only; no Coordinator/Swarm/Crew |
| HITL approval flow incomplete | P1 | `PAUSED` state exists but no UI approval/reject/resume flow |

## MAF Absorption Roadmap (from 2026-05-08 roundtable debate)

> Source: [[comparisons/microsoft-agent-framework]]

### Phase 1: Foundation (~1-2 weeks) — Do First

| Item | Effort | Why |
|------|--------|-----|
| OpenTelemetry integration | ~2 days | Replace custom PG traces; enables Jaeger/Tempo; production blocker per security review |
| Progressive skill disclosure | ~1 day | Stop eager-loading full skill content; reduce token costs and latency |
| Checkpoint graph signature hash | ~2 days | Prevent silent corruption on workflow restore; validate topology on resume |
| Tool-call budget counter | ~1 day | Per-execution per-tenant tool call limit; closes exploit where agent hammers tools in loop |

### Phase 2: Architecture (~2-3 weeks)

| Item | Effort | Why |
|------|--------|-----|
| Pluggable middleware pipeline | ~1 week | Extract cross-cutting concerns from 15 state handlers; prevents FSM from becoming unmaintainable monolith; unlocks all future features |
| ApprovalMode for tool execution | ~1 week | Human-in-the-loop for destructive ops; production blocker per security review; reuse existing `PAUSED` state |
| Provider-agnostic core (SPI) | ~1-2 weeks | Reduce LangChain4j coupling; new providers become adapter-only changes |

### Phase 3: Multi-Agent (~3-4 weeks)

| Item | Effort | Why |
|------|--------|-----|
| Concurrent Fan-Out/Fan-In | ~1 week | Highest-value lowest-complexity multi-agent pattern; add `CONCURRENT`/`JOIN` workflow node types |
| Handoff (agent self-routing) | ~1 week | Agents recognize wrong specialty and transfer control; reuse `PAUSED`/`RESUMING` + `ExecutionSnapshot` |
| Group Chat (speaker selection) | ~2-3 weeks | Collaborative reasoning; agents debate and refine; requires Concurrent + Handoff first |

## Medium (Quality & Completeness)

| Item | Priority | Details |
|------|----------|---------|
| No Flyway/Liquibase | P2 | Schema changes managed via manual SQL scripts |
| Parallelization absent | P2 | Single-threaded execution; no parallel sub-task scheduling |
| Prompt Chaining absent | P2 | No chain-of-prompt pipeline |
| Goal Setting absent | P2 | No goal tracking or completion detection |
| MCP integration stub | P2 | `McpServerController` is a stub; no FastMCP integration |
| Frontend page depth | P2 | 16 pages exist but many are UI shells |
| Resource Optimization skeleton | P2 | `TokenBudget` + `AdmissionControl` + per-iteration tool-call budget have scaffolding; LLM-as-Judge missing |
| Evaluation framework skeleton | P2 | `Evaluator` + `GuardrailsEngine` + `ToolErrorCategory` added; LLM-as-Judge missing |

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
| **Tool Use sandbox** | 2026-05-08 | `ToolSandbox` + `ContainerToolSandbox` + `InputValidator` + `SseTokenValidator`; Layer 1 Task 1 |
| **Milvus consistency_level** | 2026-05-08 | `MilvusIsolationService` with configurable consistency level; Layer 1 Task 4 |
| **Long-term memory** | 2026-05-08 | `MemoryStrategy` + `RagIsolationConfig` + `TenantKeyService`; Layer 1 Task 4 |
| **Reflection structured** | 2026-05-08 | `ReflectingStateHandler` with structured rubric + `Evaluator`; Layer 1 Task 5 |
| **Exception handling chain** | 2026-05-08 | `RetryRecoveryStrategy` + `FallbackRecoveryStrategy` + `ExceptionHandlingStateHandler`; Layer 1 Task 3 |
| **schemaplexai-admin not empty** | 2026-05-08 | Corrected: has 7 controllers, DTOs, services, 12 test files |
| **Tool-call budget** | 2026-05-08 | Per-execution per-iteration tool-call budget enforcement; Layer 1 Task 3 |
| **Guardrails engine** | 2026-05-08 | `GuardrailsEngine` with input/output validation; Layer 1 Task 5 |

## Backlinks

- See `docs/plans/README.md` for active work
- See [[gaps]] for undocumented areas (resolved)
- See [[roadmap]] for planned resolution timeline
- See [[architecture-gap-analysis]] for Agentic pattern gap matrix
