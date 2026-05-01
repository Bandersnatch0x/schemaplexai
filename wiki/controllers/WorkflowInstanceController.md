---
title: WorkflowInstanceController
type: controller
source: schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/controller/WorkflowInstanceController.java
creation_date: 2026-05-01
tags: [workflow, instance, controller, crud]
confidence: high
---

# WorkflowInstanceController

> CRUD controller for workflow runtime instances, plus a manual trigger endpoint.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/workflow/instances` | Create a new workflow instance |
| PUT | `/workflow/instances/{id}` | Update an existing instance by ID |
| DELETE | `/workflow/instances/{id}` | Delete an instance by ID |
| GET | `/workflow/instances/{id}` | Get a single instance by ID |
| GET | `/workflow/instances/page` | Paginated list of instances |
| POST | `/workflow/instances/{id}/trigger` | Manually trigger a workflow instance |

## DTO / Entity

- **Request/Response**: `SfWorkflowInstance` entity
  - `templateId` (Long): Reference to the workflow template
  - `status` (String): Instance runtime status
  - `triggerType` (String): How the instance was triggered
  - `triggerConfig` (String): Trigger-specific configuration
  - Inherits `BaseEntity`

## Service Dependencies

- `WorkflowInstanceService` — MyBatis-Plus `IService` for `SfWorkflowInstance`
  - Additional method: `trigger(Long id)` — starts the instance execution

## Notes

- The `/trigger` endpoint delegates to `WorkflowInstanceService.trigger(id)` to start BPMN execution.
- Instances link back to `WorkflowTemplate` via `templateId`.
