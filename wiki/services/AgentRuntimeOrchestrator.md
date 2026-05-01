---
title: AgentRuntimeOrchestrator
type: service
source: schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/orchestrator/AgentRuntimeOrchestrator.java
creation_date: 2026-05-01
tags: [agent, engine, orchestrator, runtime, core]
confidence: high
---

# AgentRuntimeOrchestrator

> Core orchestrator that drives a single agent execution through the state machine loop.

## Responsibilities

1. Initialize token budget for the execution
2. Run admission checks (budget, concurrency, rate limits)
3. Save the user prompt to chat memory
4. Drive the state machine loop until terminal state or max iterations
5. Handle failures and cleanup

## Dependencies

| Dependency | Type | Purpose |
|------------|------|---------|
| `AgentStateMachine` | Component | State transitions and execution lifecycle |
| `ExecutionAdmissionService` | Service | Pre-execution admission (budget, concurrency) |
| `CompositeChatMemoryStore` | Service | Persist and retrieve chat messages |

## Key Method

### `run(SfAgentExecution execution, String tenantId, String prompt)`

```
1. Create TokenBudget from constants
2. admissionService.admit(tenantId, agentId, budget)
   - If denied → transition to GATE_BLOCKED → return
3. chatMemoryStore.saveMessage(conversationId, user prompt)
4. stateMachine.start(execution)
5. Loop (max 50 iterations):
   - Get current state
   - If terminal → break
   - Transition to next state
6. If max iterations reached → force COMPLETED
7. Catch exceptions → transition to FAILED
8. Finally → releaseConcurrency
```

## Constants

- `MAX_ITERATIONS = 50` — Hard cap on state machine iterations per execution

## Token Budget Serialization

```
maxInputTokens + "," + maxOutputTokens + ",0,0"
```

## Notes

- This is the **entry point** for agent execution in the engine.
- Admission denial is non-fatal; the execution is marked `GATE_BLOCKED` and can be retried.
- The iteration guard prevents infinite loops from runaway state transitions.
