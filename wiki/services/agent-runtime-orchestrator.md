---
title: AgentRuntimeOrchestrator
type: service
source: schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/orchestrator/AgentRuntimeOrchestrator.java
creation_date: 2026-05-01
update_date: 2026-05-01
tags: [service, agent, orchestrator, runtime, core]
confidence: high
---

# AgentRuntimeOrchestrator

> One-sentence summary: Core orchestration engine that drives a single agent execution through its state machine loop, with admission control, token budgeting, chat memory persistence, and observability tracing.

## Responsibilities

1. **Trace initialization** — Start a distributed trace via `ObservabilityRecorder`
2. **Token budget setup** — Initialize input/output token limits from `CommonConstants`
3. **Admission control** — Gate execution via `ExecutionAdmissionService` (rate, concurrency, budget)
4. **Memory persistence** — Save user prompt to `CompositeChatMemoryStore`
5. **State machine loop** — Drive execution through `AgentStateMachine` with iteration guard
6. **Cleanup** — End trace and release concurrency slot in `finally` block

## Key Code

```java
public void run(SfAgentExecution execution, String tenantId, String prompt) {
    String traceId = observabilityRecorder.startTrace(...);
    try {
        // 1. Token budget
        TokenBudget tokenBudget = new TokenBudget(
            CommonConstants.DEFAULT_MAX_INPUT_TOKENS,   // 32000
            CommonConstants.DEFAULT_MAX_OUTPUT_TOKENS   // 4096
        );

        // 2. Admission check
        var admission = admissionService.admit(tenantId, execution.getAgentId(), tokenBudget);
        if (!admission.isAllowed()) {
            stateMachine.transition(AgentExecutionState.GATE_BLOCKED, execution);
            return;
        }

        // 3. Save prompt to memory
        chatMemoryStore.saveMessage(execution.getConversationId(),
            new LlmMessage("user", prompt));

        // 4. Start state machine
        stateMachine.start(execution);

        // 5. State loop (max 50 iterations)
        int iteration = 0;
        while (iteration < MAX_ITERATIONS) {
            AgentExecutionState current = stateMachine.getCurrentState(execution.getId());
            if (current == null || current.isTerminal()) break;
            stateMachine.transition(current, execution);
            iteration++;
        }

        if (iteration >= MAX_ITERATIONS) {
            stateMachine.transition(AgentExecutionState.COMPLETED, execution);
        }
    } catch (Exception e) {
        stateMachine.transition(AgentExecutionState.FAILED, execution);
    } finally {
        observabilityRecorder.endTrace(traceId, ...);
        admissionService.releaseConcurrency(tenantId, execution.getAgentId());
    }
}
```

## Execution Flow

```
startTrace()
  |
  v
TokenBudget(init)
  |
  v
admissionService.admit() ──denied──> GATE_BLOCKED ──> endTrace()
  | allowed
  v
saveMessage(user prompt)
  |
  v
stateMachine.start() -> INITIALIZING
  |
  v
while (not terminal && iterations < 50):
  stateMachine.transition(currentState)
  |
  v
endTrace() + releaseConcurrency()
```

## Configuration

| Constant | Value | Purpose |
|----------|-------|---------|
| `MAX_ITERATIONS` | 50 | Hard loop guard to prevent infinite execution |
| `DEFAULT_MAX_INPUT_TOKENS` | 32000 | Input token budget |
| `DEFAULT_MAX_OUTPUT_TOKENS` | 4096 | Output token budget |

## Dependencies

| Component | Role |
|-----------|------|
| `AgentStateMachine` | State transitions and handler dispatch |
| `ExecutionAdmissionService` | Rate, concurrency, token, and cost gating |
| `CompositeChatMemoryStore` | Persist chat messages to Redis |
| `ObservabilityRecorder` | Distributed tracing (traceId, start/end) |

## Notes

- **Synchronous execution** — Called directly by `AgentExecutionEngine`; async wrapper not yet implemented
- **Defensive loop guard** — `MAX_ITERATIONS = 50` prevents runaway agent loops
- **Terminal states** — `COMPLETED`, `FAILED`, `CANCELLED` (see `AgentExecutionState.isTerminal()`)
- **Concurrency release** — Always called in `finally` to prevent slot leaks

## Backlinks

- Called by: [[services/agent-execution-engine]]
- States: [[services/agent-state-machine]]
- Admission: [[services/execution-admission-service]]
- Controller: [[controllers/agent-execution-controller]]
- Entity: [[entities/agent]]
