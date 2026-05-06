---
title: NodeExecutorRegistry
type: service
source: schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/service/WorkflowNodeEngine.java
creation_date: 2026-05-06
update_date: 2026-05-06
tags: [service, workflow, node, executor, engine, flowable, bpmn]
confidence: high
---

# NodeExecutorRegistry

> One-sentence summary: Extensible node execution engine for workflow templates that auto-discovers all `NodeExecutor` beans by type, bridges Flowable BPMN service tasks to the registry, and encapsulates execution results with success/failure state.

## Responsibilities

1. Auto-wire all `NodeExecutor` Spring beans into a type-indexed registry at startup
2. Dispatch workflow node executions to the correct executor by `nodeType`
3. Persist node execution status (RUNNING → COMPLETED/FAILED) via `SfWorkflowNodeExecutionMapper`
4. Bridge Flowable BPMN `JavaDelegate` service tasks to the node execution engine
5. Provide a uniform `NodeExecutionResult` encapsulating success, message, and output map

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `WorkflowNodeEngine` | `schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/service/WorkflowNodeEngine.java` | Registry and dispatcher |
| `NodeExecutor` | `schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/node/NodeExecutor.java` | Interface: `getNodeType()` + `execute(input, tenantId)` |
| `NodeExecutionResult` | `schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/node/NodeExecutionResult.java` | Result DTO: success, message, output map |
| `FlowableDelegateAdapter` | `schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/service/FlowableDelegateAdapter.java` | Bridges Flowable `JavaDelegate` → `WorkflowNodeEngine` |

### Current Executors (7 types)

| Executor | Type | Description |
|----------|------|-------------|
| `HttpNodeExecutor` | HTTP | Placeholder for REST API calls (returns statusCode 200 stub) |
| `ScriptNodeExecutor` | SCRIPT | Placeholder for script execution (Groovy/JS planned) |
| `StartNodeExecutor` | START | Workflow start marker; validates `workflowInstanceId` |
| `EndNodeExecutor` | END | Workflow end marker; captures result and endedAt timestamp |
| `AIModelNodeExecutor` | AI_MODEL | Placeholder for LLM inference; validates prompt, returns preview stub |
| `ToolCallNodeExecutor` | TOOL_CALL | Placeholder for tool invocation; validates toolName, returns execution stub |
| `ConditionNodeExecutor` | CONDITION | Evaluates comparison expressions (`>`, `<`, `==`, `!=`, `>=`, `<=`) against variables map |

### Registry Initialization

```java
@PostConstruct
public void init() {
    this.executors = executorList.stream()
        .collect(Collectors.toMap(NodeExecutor::getNodeType, Function.identity()));
}
```

All `@Component` classes implementing `NodeExecutor` are auto-discovered by Spring and indexed by their `getNodeType()` return value.

### Execution Flow

```
FlowableDelegateAdapter.execute(DelegateExecution)
  ├─ Build SfWorkflowNodeExecution from Flowable context
  ├─ WorkflowNodeEngine.executeNode(nodeExecution)
  │   ├─ Lookup executor by nodeType
  │   ├─ Set status RUNNING, persist
  │   ├─ Parse input JSON → Map
  │   ├─ executor.execute(input, tenantId)
  │   ├─ Set status COMPLETED/FAILED, persist output
  │   └─ Return NodeExecutionResult
  └─ Set Flowable variables: nodeResult, nodeOutput
```

### NodeExecutionResult Factory Methods

```java
NodeExecutionResult.success(Map.of("key", "value"))  // success with output
NodeExecutionResult.success()                           // success, empty output
NodeExecutionResult.failure("error message")           // failure with message
```

### Key Code

```java
public interface NodeExecutor {
    String getNodeType();
    NodeExecutionResult execute(Map<String, Object> input, String tenantId);
}
```

## Known Issues

- **Most executors are placeholders** — HTTP, SCRIPT, AI_MODEL, and TOOL_CALL return stub responses. Real integrations (RestTemplate, Groovy engine, LangChain4j, ToolRegistry) are planned.
- **Condition evaluator is basic** — supports numeric and string comparisons only; no logical operators (AND/OR) or complex expressions.

## Related

- [[services/workflow-node-engine]] — existing wiki page for the workflow node engine
- [[entities/workflow]] — workflow template, instance, and node execution entities
- [[services/tool-registry]] — will be invoked by ToolCallNodeExecutor in production
- [[services/agent-execution-engine]] — AI model execution for AIModelNodeExecutor
