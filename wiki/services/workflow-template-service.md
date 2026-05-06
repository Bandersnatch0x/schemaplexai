---
title: WorkflowTemplateService
type: service
source: schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/service/WorkflowTemplateService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, workflow, template, bpmn, flowable]
confidence: high
---

# WorkflowTemplateService

> One-sentence summary: Manages workflow template lifecycle including deployment, validation, cloning, and activation, defining reusable BPMN process blueprints for AI-augmented workflows.

## Responsibilities

1. Deploy workflow templates for instantiation
2. Validate workflow template node configurations
3. Clone existing templates for customization
4. List deployed templates
5. Deactivate deployed templates
6. Provide CRUD operations via `IService<SfWorkflowTemplate>`

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `deployTemplate` | Deploy a workflow template, changing its status to deployed | `templateId` — the template ID | `SfWorkflowTemplate` — the deployed template |
| `validateTemplate` | Validate a workflow template's node configuration | `templateId` — the template ID | `boolean` — true if valid |
| `cloneTemplate` | Clone an existing workflow template | `templateId` — source template ID; `newName` — name for the cloned template | `SfWorkflowTemplate` — the cloned template |
| `listDeployedTemplates` | List all deployed workflow templates | — | `List<SfWorkflowTemplate>` — deployed templates |
| `deactivateTemplate` | Deactivate a deployed workflow template | `templateId` — the template ID | `SfWorkflowTemplate` — the deactivated template |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `WorkflowTemplateService` | `schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/service/WorkflowTemplateService.java` | Service interface extending `IService<SfWorkflowTemplate>` |
| `SfWorkflowTemplate` | `schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/entity/SfWorkflowTemplate.java` | Entity: `name`, `description`, `nodeConfigJson`, `status` |

## Dependencies / Collaborators

- **MyBatis-Plus** — `IService<SfWorkflowTemplate>` provides CRUD, pagination, and query helpers
- **SfWorkflowTemplate entity** — stores templates in `sf_workflow_template` table
- **Flowable** — BPMN engine for workflow execution
- **WorkflowInstanceService** — creates instances from deployed templates

## Backlinks

- [[services/workflow-instance-service]] — creates instances from deployed templates
- [[services/agent-execution-lifecycle-service]] — agents may be nodes within workflows
- [[entities/workflow-template]] — workflow template entity
