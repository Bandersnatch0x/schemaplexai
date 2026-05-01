---
title: AgentStateMachine
type: service
source: schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/AgentStateMachine.java
creation_date: 2026-05-01
update_date: 2026-05-01
tags: [service, agent, state-machine, execution]
confidence: high
---

# AgentStateMachine

> One-sentence summary: In-memory state machine that manages agent execution lifecycle transitions, dispatches state handlers, and prevents transitions from terminal states.

## Responsibilities

1. **State tracking** — Maintain current state per execution ID in `ConcurrentHashMap`
2. **State transitions** — Validate and execute transitions with handler dispatch
3. **Terminal state guard** — Block transitions from `COMPLETED`, `FAILED`, `CANCELLED`
4. **Handler dispatch** — Auto-invoke registered `AgentStateHandler` for each state
5. **Persistence** — Save state to database via `SfAgentExecutionMapper`

## Key Code

```java
@Component
public class AgentStateMachine {

    private final Map<Long, AgentExecutionState> executionStates = new ConcurrentHashMap<>();
    private final Map<AgentExecutionState, AgentStateHandler> handlers;

    public void start(SfAgentExecution execution) {
        transition(AgentExecutionState.INITIALIZING, execution);
    }

    public void transition(AgentExecutionState newState, SfAgentExecution execution) {
        AgentExecutionState current = executionStates.get(execution.getId());
        if (current != null && current.isTerminal()) {
            log.warn("Cannot transition from terminal state {} to {}", current, newState);
            return;
        }
        execution.setState(newState.name());
        saveExecution(execution);
        executionStates.put(execution.getId(), newState);

        if (newState.isTerminal()) {
            removeExecution(execution.getId());
        }

        AgentStateHandler handler = handlers.get(newState);
        if (handler != null) {
            try {
                handler.handle(this, execution);
            } catch (Exception e) {
                log.error("State handler error", e);
                transition(AgentExecutionState.FAILED, execution);
            }
        }
    }
}
```

## States

| State | Terminal | Description |
|-------|----------|-------------|
| `INITIALIZING` | No | Setup phase |
| `READY` | No | Waiting for input |
| `THINKING` | No | LLM reasoning |
| `TOOL_CALLING` | No | Executing tool |
| `OBSERVATION` | No | Processing tool result |
| `PAUSED` | No | Execution paused |
| `GATE_BLOCKED` | No | Admission denied |
| `RETRYING` | No | Retry attempt |
| `COMPLETED` | Yes | Success |
| `FAILED` | Yes | Error |
| `CANCELLED` | Yes | User cancelled |

## State Transition Rules

- **No transition from terminal** — Once `COMPLETED`/`FAILED`/`CANCELLED`, state is locked
- **Handler errors cascade** — Handler exception triggers transition to `FAILED`
- **Terminal cleanup** — In-memory state removed on terminal transition

## Dependencies

| Component | Role |
|-----------|------|
| `SfAgentExecutionMapper` | Persist state to database |
| `AgentStateHandler` | State-specific behavior (injected list) |

## Backlinks

- Orchestrator: [[services/agent-runtime-orchestrator]]
- Lifecycle: [[services/agent-execution-lifecycle-service]]
- Entity: [[entities/agent]]
