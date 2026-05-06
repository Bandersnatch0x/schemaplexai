---
title: WorkflowInstanceService
type: service
source: schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/service/WorkflowInstanceService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, workflow, instance, bpmn, flowable]
confidence: high
---

# WorkflowInstanceService

> One-sentence summary: Manages workflow instance execution, triggering running instances that are created from deployed workflow templates.

## Responsibilities

1. Trigger workflow instance execution
2. Manage workflow instance lifecycle via CRUD operations from `IService`
3. Track instance status and trigger configuration

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `trigger` | Trigger a workflow instance to start or resume execution | `instanceId` — the workflow instance ID | `void` |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `WorkflowInstanceService` | `schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/service/WorkflowInstanceService.java` | Service interface extending `IService<SfWorkflowInstance>` |
| `SfWorkflowInstance` | `schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/entity/SfWorkflowInstance.java` | Entity: `templateId`, `status`, `triggerType`, `triggerConfig` |

## Dependencies / Collaborators

- **MyBatis-Plus** — `IService<SfWorkflowInstance>` provides CRUD, pagination, and query helpers
- **SfWorkflowInstance entity** — stores instances in `sf_workflow_instance` table
- **WorkflowTemplateService** — provides templates from which instances are created
- **Flowable** — BPMN engine for workflow execution

## Backlinks

- [[services/workflow-template-service]] — templates from which instances are created
- [[services/agent-execution-lifecycle-service]] — agents may trigger workflow instances
- [[entities/workflow-instance]] — workflow instance entity
