---
title: AgentLoopShadowReviewService
type: service
source: schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/shadow/AgentLoopShadowReviewService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, agent, shadow, loop, feedback, memory]
confidence: high
---

# AgentLoopShadowReviewService

> One-sentence summary: Parses shadow-mode feedback actions and applies or previews them against agent executions, persisting feedback as agent memory for continuous improvement.

## Responsibilities

1. Parse JSON feedback actions into typed `FeedbackAction` objects
2. Apply feedback actions (RETRY, SKIP, MODIFY_PROMPT, ESCALATE, ACCEPT) to an execution
3. Persist every applied feedback as `SHADOW_FEEDBACK` memory for the agent
4. In shadow mode, log suggested actions without applying them (review-only)

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `parseFeedbackActions(String feedbackActionsJson)` | Deserialize JSON into list of FeedbackAction | `feedbackActionsJson` — JSON array of actions | `List<FeedbackAction>` |
| `applyFeedbackAction(Long executionId, Long agentId, FeedbackAction action)` | Apply a single feedback action and persist to memory | `executionId` — execution ID; `agentId` — agent ID; `action` — feedback action | void |
| `reviewLoop(Long executionId, Long agentId, String shadowConfigJson)` | Shadow-mode review: parse and log suggested actions without applying | `executionId`, `agentId`, `shadowConfigJson` | void |

## FeedbackAction Types

Defined in `FeedbackAction.java`:

| Type | Behavior |
|------|----------|
| `RETRY` | Log retry intent for execution |
| `SKIP` | Log skip intent for current step |
| `MODIFY_PROMPT` | Log prompt modification intent |
| `ESCALATE` | Log escalation intent |
| `ACCEPT` | Log acceptance of result |

## Key Code

```java
public void applyFeedbackAction(Long executionId, Long agentId, FeedbackAction action) {
    log.info("Applying feedback action {} to execution {}", action.getType(), executionId);
    switch (action.getType()) {
        case RETRY -> log.info("Retrying execution {}", executionId);
        case SKIP -> log.info("Skipping current step for execution {}", executionId);
        case MODIFY_PROMPT -> log.info("Modifying prompt for execution {}", executionId);
        case ESCALATE -> log.info("Escalating execution {}", executionId);
        case ACCEPT -> log.info("Accepting result for execution {}", executionId);
    }
    // Persist feedback as memory for future improvement
    SfAgentMemory memory = new SfAgentMemory();
    memory.setAgentId(agentId);
    memory.setMemoryType("SHADOW_FEEDBACK");
    memory.setContent(action.getPayload());
    memory.setSourceExecutionId(executionId);
    memoryMapper.insert(memory);
}
```

## Dependencies / Collaborators

| Component | Role |
|-----------|------|
| `ObjectMapper` | JSON deserialization of feedback actions |
| `SfAgentMemoryMapper` | Persist feedback as agent memory |
| `FeedbackAction` | Typed feedback action (type, description, payload) |
| `FeedbackActionType` | Enum of supported action types |

## Backlinks

- Related: [[services/agent-loop-detection-service]] — triggers shadow review when loops are detected
- Related: [[services/shadow-config-service]] — provides shadow configuration for agents
- Related: [[services/agent-execution-lifecycle-service]] — execution lifecycle management
- Entity: [[entities/agent]]
