# Agent-Engine Agentic Pattern Gaps — Implementation Spec

**Date**: 2026-05-07
**Scope**: schemaplexai-agent-engine
**Status**: Implemented

## Background

The `architecture-gap-analysis.md` identified 21 Agentic Design Patterns with significant gaps in Layer 1 (Foundation). Three critical gaps were targeted for parallel implementation.

## Changes

### 1. RAG Integration

**Problem**: `ContextInjector.inject()` was a no-op; RAG pipeline existed in `context` module but was not invoked by agent-engine.

**Solution**:
- `ContextInjector.inject(List<LlmMessage>, Long)` now extracts the latest user message, retrieves `tenantId` from `TenantContextHolder`, builds an `AgentContext`, and calls `retrieveRagContext()`.
- RAG results are injected as a `system` message before the last user message.
- Failures are non-blocking (try/catch with warning log).

**Files**: `ContextInjector.java`, `ContextInjectorTest.java`

### 2. Guardrails Layer

**Problem**: `GuardrailsEngine`, `BlacklistGuardrail`, `LengthGuardrail` existed but had no Spring wiring and were not invoked by `ThinkingStateHandler`.

**Solution**:
- `GuardrailsConfig.java`: `@Configuration` class wiring all guardrail beans.
- `ThinkingStateHandler`: added `GuardrailsEngine` constructor parameter; calls `validateInput(prompt)` before LLM invocation; transitions to `GATE_BLOCKED` on failure with `admissionType=GUARDRAILS`.

**Files**: `GuardrailsConfig.java`, `ThinkingStateHandler.java`, `GuardrailsEngineTest.java`, `BlacklistGuardrailTest.java`, `LengthGuardrailTest.java`, `ToolSafetyGuardTest.java`, `ThinkingStateHandlerTest.java`

### 3. Planning Mode

**Problem**: `THINKING` was a single LLM call with no task decomposition.

**Solution**:
- `AgentExecutionState.PLANNING`: new state between `READY` and `THINKING`.
- `PlanningStateHandler`: LLM-based task decomposition into `SubTaskPlan`; stores plan JSON in execution metadata; transitions to `THINKING`.
- `SubTask` / `SubTaskPlan`: models with dependency resolution, next-ready selection, all-completed check.
- `ThinkingStateHandler`: `resolveNextStateForPlan()` advances sub-tasks on direct-answer completion.

**Files**: `AgentExecutionState.java`, `PlanningStateHandler.java`, `SubTask.java`, `SubTaskPlan.java`, `PlanningStateHandlerTest.java`, `SubTaskPlanTest.java`, `ThinkingStateHandler.java`

## Testing

- 967 tests run, 0 failures, 0 errors, 1 skipped
- New tests: 149 (RAG: 13, Guardrails: 101, Planning: 35)

## Backlinks

- `wiki/architecture-gap-analysis.md`
- `wiki/technical-debt.md`
