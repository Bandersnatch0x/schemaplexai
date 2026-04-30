---
title: WorkflowNodeEngine
type: service
source: schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/service/WorkflowNodeEngine.java
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [service, workflow, bpmn, node-executor]
confidence: high
---

# WorkflowNodeEngine

> One-sentence summary: Strategy-pattern node executor registry that dispatches workflow nodes to typed executors (TRIGGER/DOCUMENT/AGENT/APPROVAL/QUALITY/NOTIFICATION/ARTIFACT) with transaction safety.

## Responsibilities

1. **Executor Registry**: `@PostConstruct` builds `Map<String, NodeExecutor>` from all `NodeExecutor` beans
2. **Node Execution**: Finds executor by `nodeType`, updates status to RUNNING, executes, writes output JSON
3. **Error Handling**: Catches exceptions, sets FAILED status, logs error, rethrows as `BaseException`

## Node Types

| Type | Description |
|------|-------------|
| TRIGGER | Workflow trigger node |
| DOCUMENT | Document processing |
| AGENT | AI agent invocation |
| APPROVAL | Human approval gate |
| QUALITY | Quality gate validation |
| NOTIFICATION | Send notification |
| ARTIFACT | Produce deliverable artifact |

## Key Pattern

```java
@PostConstruct
public void init() {
    this.executors = executorList.stream()
        .collect(Collectors.toMap(NodeExecutor::getNodeType, Function.identity()));
}
```

## Transaction

`@Transactional(rollbackFor = Exception.class)` — node execution is atomic

## Backlinks

- Workflow entities: [[entities/workflow]]
- Flowable integration in `schemaplexai-workflow` module
