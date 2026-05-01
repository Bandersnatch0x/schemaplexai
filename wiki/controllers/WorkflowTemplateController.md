---
title: WorkflowTemplateController
type: controller
source: schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/controller/WorkflowTemplateController.java
creation_date: 2026-05-01
tags: [workflow, template, controller, crud]
confidence: high
---

# WorkflowTemplateController

> CRUD controller for workflow template definitions (BPMN template metadata).

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/workflow/templates` | Create a new workflow template |
| PUT | `/workflow/templates/{id}` | Update an existing template by ID |
| DELETE | `/workflow/templates/{id}` | Delete a template by ID |
| GET | `/workflow/templates/{id}` | Get a single template by ID |
| GET | `/workflow/templates/page` | Paginated list of templates |

## DTO / Entity

- **Request/Response**: `SfWorkflowTemplate` entity
  - `name` (String): Template name
  - `description` (String): Template description
  - `nodeConfigJson` (String): JSON-serialized node configuration
  - `status` (String): Template status
  - Inherits `BaseEntity` (id, tenantId, createdAt, updatedAt, createdBy, updatedBy, deleted)

## Service Dependencies

- `WorkflowTemplateService` — MyBatis-Plus `IService` for `SfWorkflowTemplate`

## Notes

- Standard CRUD with pagination via `PageParam` / `PageResult`.
- `nodeConfigJson` stores the BPMN node graph structure as JSON.
