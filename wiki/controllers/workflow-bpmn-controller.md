---
title: WorkflowBpmnController
type: controller
source: schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/controller/WorkflowBpmnController.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [controller, workflow, bpmn, flowable, process, execution]
confidence: high
---

# WorkflowBpmnController

> One-sentence summary: Flowable BPMN workflow controller for deploying, listing, starting, suspending, and activating process instances.

## Base Path

`/workflow/bpmn` (routed via Gateway to `schemaplexai-workflow` port 8087)

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/processes` | List all deployed (active) BPMN process definitions |
| POST | `/processes/{processKey}/start` | Start a new process instance by process definition key |
| POST | `/processes/{processKey}/suspend` | Suspend a process definition by key |
| POST | `/processes/{processKey}/activate` | Activate a suspended process definition by key |

## Key Request/Response DTOs

### StartProcessRequest
```java
public record StartProcessRequest(
    String businessKey,           // Business identifier for the instance
    Map<String, Object> variables // Flowable process variables
) {}
```

### ProcessDefinitionInfo (Response)
- Returned by `WorkflowDeployService.listDeployedProcesses()`
- Contains process definition metadata (key, name, version, deploymentId)

### Responses
- `Result<List<ProcessDefinitionInfo>>` — list deployed processes
- `Result<String>` — started process instance ID
- `Result<Boolean>` — suspend/activate success flag

## Dependencies

- `WorkflowDeployService workflowDeployService` — handles Flowable process deployment, instance lifecycle, and state management

## Notes

- Returns `Result<T>` wrapper (see [[architecture]])
- Process start failures return `ResultCode.WORKFLOW_NOT_FOUND` with error details
- Example process keys: `"specReviewApproval"`

## Backlinks

- Workflow templates: [[controllers/WorkflowTemplateController]]
- Workflow instances: [[controllers/WorkflowInstanceController]]
- See [[routes]] for routing
