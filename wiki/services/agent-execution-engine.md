---
title: AgentExecutionEngine
type: service
source: schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/AgentExecutionEngine.java
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [service, agent, execution, core]
confidence: high
---

# AgentExecutionEngine

> One-sentence summary: Entry point for agent execution — creates execution record with UUID conversation_id, persists state, and delegates to AgentRuntimeOrchestrator.

## Responsibilities

1. Create `SfAgentExecution` with state `INITIALIZING`
2. Generate UUID `conversation_id`
3. Persist via `SfAgentExecutionMapper`
4. Delegate to `AgentRuntimeOrchestrator.run()`

## Key Code

```java
public SfAgentExecution startExecution(Long agentId, String tenantId, String prompt) {
    SfAgentExecution execution = new SfAgentExecution();
    execution.setAgentId(agentId);
    execution.setConversationId(UUID.randomUUID().toString());
    execution.setState("INITIALIZING");
    execution.setTenantId(tenantId);
    executionMapper.insert(execution);
    orchestrator.run(execution, tenantId, prompt);  // currently synchronous
    return execution;
}
```

## Known Issues

- **Currently synchronous** — comment says "Run asynchronously in production; here synchronous for simplicity"
- This is a stub-level implementation; full async with thread pool not yet implemented

## Dependencies

- `SfAgentExecutionMapper` — MyBatis-Plus mapper
- `AgentRuntimeOrchestrator` — actual execution logic

## Backlinks

- Controller: [[controllers/agent-execution-controller]]
- Entity: [[entities/agent]]
