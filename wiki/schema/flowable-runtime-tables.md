---
title: Flowable BPMN Runtime Tables (act_*)
type: schema
source: Flowable 7.0.0 auto-DDL + SchemaPlexAI BPMN processes
creation_date: 2026-05-07
tags: [schema, flowable, bpmn, workflow, act]
confidence: high
---

# Flowable BPMN Runtime Tables (act_*)

> One-sentence summary: Flowable 7 auto-generates `act_*` tables at startup to store BPMN deployments, process definitions, runtime executions, tasks, variables, jobs, and history. SchemaPlexAI uses these for `aiAgentExecution` and `specReviewApproval` workflows.

## Overview

Flowable 7.0.0 is configured with `database-schema-update=true` (Spring Boot default), which auto-creates all required tables on first startup. These tables are **not** in the PostgreSQL init scripts (`docker/postgres/init/`) because Flowable manages its own schema lifecycle.

SchemaPlexAI interacts with these tables via:
- **Flowable Java API** (`RuntimeService`, `TaskService`, `RepositoryService`, `HistoryService`)
- **6 Java delegates** implementing `org.flowable.engine.delegate.JavaDelegate`
- **1 task listener** implementing `org.flowable.engine.delegate.TaskListener`
- **`FlowableDelegateAdapter`** bridging service tasks to `WorkflowNodeEngine`

## Table Categories

| Prefix | Category | Purpose | Lifecycle |
|--------|----------|---------|-----------|
| `act_re_*` | **RE**pository | Deployments, process definitions, models | Persistent |
| `act_ru_*` | **RU**ntime | Active executions, tasks, variables, jobs | Deleted on completion |
| `act_hi_*` | **HI**story | Completed process instances, activities, tasks | Persistent (configurable retention) |
| `act_ge_*` | **GE**neral | Properties, byte arrays | Persistent |
| `act_id_*` | **ID**entity | Users, groups, memberships | Optional (SchemaPlexAI uses its own `sf_user`/`sf_role`) |

## Key Tables for SchemaPlexAI

### Repository Tables (act_re_*)

#### `act_re_deployment`
Stores BPMN deployment records. Each `.bpmn20.xml` file deployment creates one row.

| Column | Type | Meaning |
|--------|------|---------|
| `id_` | VARCHAR(64) | Deployment UUID |
| `name_` | VARCHAR(255) | Deployment name |
| `category_` | VARCHAR(255) | Deployment category |
| `deploy_time_` | TIMESTAMP | When deployed |
| `tenant_id_` | VARCHAR(255) | Tenant isolation (matches `X-Tenant-Id`) |

#### `act_re_procdef`
Stores process definitions. One row per `<process>` element in a deployed BPMN file.

| Column | Type | Meaning |
|--------|------|---------|
| `id_` | VARCHAR(64) | `key:version:deploymentId` (e.g., `aiAgentExecution:1:2501`) |
| `key_` | VARCHAR(255) | Process key from BPMN (`aiAgentExecution`, `specReviewApproval`) |
| `version_` | INT | Auto-incremented on redeploy |
| `name_` | VARCHAR(255) | Process name from BPMN |
| `resource_name_` | VARCHAR(4000) | Source XML file path |
| `suspension_state_` | INT | 1=active, 2=suspended |
| `tenant_id_` | VARCHAR(255) | Tenant isolation |

**SchemaPlexAI processes:**
- `aiAgentExecution` — AI Agent Execution with Human-in-the-Loop
- `specReviewApproval` — Spec Review Approval Workflow

### Runtime Tables (act_ru_*)

#### `act_ru_execution`
The core execution table. Each active process instance has at least one row (the root execution).

| Column | Type | Meaning |
|--------|------|---------|
| `id_` | VARCHAR(64) | Execution UUID |
| `proc_inst_id_` | VARCHAR(64) | Root process instance ID |
| `business_key_` | VARCHAR(255) | Correlation key (SchemaPlexAI sets `executionId`) |
| `parent_id_` | VARCHAR(64) | Parent execution (for sub-processes, parallel gateways) |
| `proc_def_id_` | VARCHAR(64) | FK to `act_re_procdef.id_` |
| `act_id_` | VARCHAR(255) | Current activity ID (BPMN element id) |
| `is_active_` | TINYINT | 1 = currently executing |
| `is_concurrent_` | TINYINT | 1 = parallel branch |
| `is_scope_` | TINYINT | 1 = scope boundary (process, sub-process) |
| `suspension_state_` | INT | 1=active, 2=suspended |
| `tenant_id_` | VARCHAR(255) | Tenant isolation |

**SchemaPlexAI usage:** `WorkflowInstanceServiceImpl.startProcessInstance()` sets `businessKey` to the `executionId` for correlation with `sf_workflow_instance`.

#### `act_ru_task`
User tasks awaiting human action. SchemaPlexAI uses these for review checkpoints.

| Column | Type | Meaning |
|--------|------|---------|
| `id_` | VARCHAR(64) | Task UUID |
| `name_` | VARCHAR(255) | Task name from BPMN |
| `assignee_` | VARCHAR(255) | Assigned user (e.g., `${userId}`, `ai-supervisor`) |
| `owner_` | VARCHAR(255) | Task owner |
| `delegation_` | VARCHAR(64) | Delegation state |
| `priority_` | INT | Task priority (default 50) |
| `create_time_` | TIMESTAMP | When task was created |
| `due_date_` | TIMESTAMP | Optional deadline |
| `category_` | VARCHAR(255) | Task category |
| `suspension_state_` | INT | 1=active, 2=suspended |
| `tenant_id_` | VARCHAR(255) | Tenant isolation |
| `form_key_` | VARCHAR(255) | Associated form reference |
| `proc_inst_id_` | VARCHAR(64) | FK to process instance |
| `proc_def_id_` | VARCHAR(64) | FK to process definition |
| `task_def_key_` | VARCHAR(255) | BPMN userTask element id |

**SchemaPlexAI user tasks:**
- `humanReviewTask` — Human supervisor reviews agent execution plan
- `humanFeedbackTask` — Provide feedback to improve agent output
- `primaryReviewTask` — Primary reviewer evaluates spec document
- `escalatedReviewTask` — Senior reviewer performs escalated review
- `reviseTask` — Submitter revises spec based on feedback

#### `act_ru_variable`
Process variables (transient, deleted on completion).

| Column | Type | Meaning |
|--------|------|---------|
| `id_` | VARCHAR(64) | Variable UUID |
| `type_` | VARCHAR(255) | Variable type (`string`, `integer`, `long`, `double`, `boolean`, `json`, `serializable`) |
| `name_` | VARCHAR(255) | Variable name |
| `text_` | VARCHAR(4000) | String value or JSON serialized value |
| `text2_` | VARCHAR(4000) | Secondary text (for long strings) |
| `long_` | BIGINT | Long/Integer value |
| `double_` | DOUBLE | Double value |
| `bytearray_id_` | VARCHAR(64) | FK to `act_ge_bytearray` (for serializable objects) |
| `execution_id_` | VARCHAR(64) | FK to execution |
| `proc_inst_id_` | VARCHAR(64) | FK to process instance |
| `task_id_` | VARCHAR(64) | FK to task (task-local variable) |

**SchemaPlexAI key variables (aiAgentExecution):**
- `tenantId` — Tenant isolation
- `agentId` — Agent to execute
- `executionId` / `executionTrackingId` — Correlation with `sf_workflow_instance`
- `taskDescription` — User request
- `requireHumanApproval` — Boolean flag
- `trustScore` — 0.0-1.0 trust metric
- `agentResult` — Map with `success`, `message`, `output`, `qualityScore`
- `humanApproved` — Boolean from user task
- `humanFeedback` — String from user task
- `retryCount` — Integer for re-execution loop

**SchemaPlexAI key variables (specReviewApproval):**
- `tenantId`, `specId`, `submitterId`, `specTitle`
- `riskLevel` — `LOW` / `MEDIUM` / `HIGH`
- `reviewerId` — Assigned reviewer
- `approvalDecision` — `APPROVE` / `REJECT` / `REVISE` / `ESCALATE`
- `rejectionReason` — String

#### `act_ru_job` / `act_ru_timer_job` / `act_ru_suspended_job` / `act_ru_deadletter_job`
Async job tables. SchemaPlexAI uses async service tasks for non-blocking agent execution.

| Column | Type | Meaning |
|--------|------|---------|
| `id_` | VARCHAR(64) | Job UUID |
| `type_` | VARCHAR(255) | `timer`, `message`, `async`, `deadletter` |
| `retries_` | INT | Remaining retry count |
| `exception_msg_` | VARCHAR(4000) | Last exception message |
| `handler_type_` | VARCHAR(255) | Job handler class |
| `handler_cfg_` | VARCHAR(4000) | Handler configuration |
| `due_date_` | TIMESTAMP | When job should execute |
| `execution_id_` | VARCHAR(64) | FK to execution |
| `proc_inst_id_` | VARCHAR(64) | FK to process instance |

**Dead letter jobs** (`act_ru_deadletter_job`): After retries are exhausted, failed jobs move here for manual inspection.

#### `act_ru_identitylink`
Links users/groups to tasks/processes (assignees, candidates, owners).

| Column | Type | Meaning |
|--------|------|---------|
| `id_` | VARCHAR(64) | Link UUID |
| `type_` | VARCHAR(255) | `assignee`, `candidate`, `owner`, `starter`, `participant` |
| `user_id_` | VARCHAR(255) | User identifier |
| `group_id_` | VARCHAR(255) | Group identifier |
| `task_id_` | VARCHAR(64) | FK to task |
| `proc_inst_id_` | VARCHAR(64) | FK to process instance |

**SchemaPlexAI usage:** `HumanTaskAssignmentDelegate` populates this via `TaskService.addCandidateGroup()` for escalated reviews (`senior-reviewers`).

### History Tables (act_hi_*)

#### `act_hi_procinst`
Historic process instances. One row per completed or active process instance.

| Column | Type | Meaning |
|--------|------|---------|
| `id_` | VARCHAR(64) | Process instance UUID |
| `proc_inst_id_` | VARCHAR(64) | Same as id_ |
| `business_key_` | VARCHAR(255) | Correlation key (`executionId`) |
| `proc_def_id_` | VARCHAR(64) | FK to process definition |
| `start_time_` | TIMESTAMP | Process start time |
| `end_time_` | TIMESTAMP | Process end time (null if active) |
| `duration_` | BIGINT | Duration in milliseconds |
| `start_user_id_` | VARCHAR(255) | User who started the process |
| `start_act_id_` | VARCHAR(255) | Start activity ID |
| `end_act_id_` | VARCHAR(255) | End activity ID |
| `super_process_instance_id_` | VARCHAR(64) | Parent process (for call activities) |
| `delete_reason_` | VARCHAR(4000) | Why deleted (if applicable) |
| `tenant_id_` | VARCHAR(255) | Tenant isolation |

#### `act_hi_actinst`
Historic activity instances. One row per visited BPMN element.

| Column | Type | Meaning |
|--------|------|---------|
| `id_` | VARCHAR(64) | Activity instance UUID |
| `proc_def_id_` | VARCHAR(64) | Process definition |
| `proc_inst_id_` | VARCHAR(64) | Process instance |
| `execution_id_` | VARCHAR(64) | Execution |
| `act_id_` | VARCHAR(255) | BPMN element id |
| `act_name_` | VARCHAR(255) | BPMN element name |
| `act_type_` | VARCHAR(255) | `startEvent`, `serviceTask`, `userTask`, `exclusiveGateway`, `endEvent`, etc. |
| `assignee_` | VARCHAR(255) | For user tasks |
| `start_time_` | TIMESTAMP | Activity start |
| `end_time_` | TIMESTAMP | Activity end |
| `duration_` | BIGINT | Duration in ms |
| `tenant_id_` | VARCHAR(255) | Tenant isolation |

**SchemaPlexAI usage:** This table powers the workflow monitoring UI (`WorkflowMonitor` page) showing execution timelines.

#### `act_hi_taskinst`
Historic task instances.

| Column | Type | Meaning |
|--------|------|---------|
| `id_` | VARCHAR(64) | Task UUID |
| `proc_def_id_` | VARCHAR(64) | Process definition |
| `task_def_key_` | VARCHAR(255) | BPMN userTask id |
| `proc_inst_id_` | VARCHAR(64) | Process instance |
| `execution_id_` | VARCHAR(64) | Execution |
| `name_` | VARCHAR(255) | Task name |
| `parent_task_id_` | VARCHAR(64) | Parent task (for sub-tasks) |
| `description_` | VARCHAR(4000) | Task description |
| `owner_` | VARCHAR(255) | Owner |
| `assignee_` | VARCHAR(255) | Assignee |
| `start_time_` | TIMESTAMP | Task creation |
| `claim_time_` | TIMESTAMP | When claimed |
| `end_time_` | TIMESTAMP | Task completion |
| `duration_` | BIGINT | Duration in ms |
| `delete_reason_` | VARCHAR(4000) | Why deleted |
| `due_date_` | TIMESTAMP | Deadline |
| `form_key_` | VARCHAR(255) | Form key |
| `category_` | VARCHAR(255) | Category |
| `tenant_id_` | VARCHAR(255) | Tenant isolation |

#### `act_hi_varinst`
Historic variable instances. Captures all variable values at process completion.

| Column | Type | Meaning |
|--------|------|---------|
| `id_` | VARCHAR(64) | Variable UUID |
| `proc_inst_id_` | VARCHAR(64) | Process instance |
| `execution_id_` | VARCHAR(64) | Execution |
| `task_id_` | VARCHAR(64) | Task |
| `name_` | VARCHAR(255) | Variable name |
| `var_type_` | VARCHAR(100) | Type |
| `text_` | VARCHAR(4000) | String/JSON value |
| `text2_` | VARCHAR(4000) | Secondary text |
| `long_` | BIGINT | Long value |
| `double_` | DOUBLE | Double value |
| `bytearray_id_` | VARCHAR(64) | FK to byte array |

### General Tables (act_ge_*)

#### `act_ge_property`
Engine version and schema metadata.

| Column | Type | Meaning |
|--------|------|---------|
| `name_` | VARCHAR(64) | Property key (`schema.version`, `schema.history`) |
| `value_` | VARCHAR(300) | Property value |
| `rev_` | INT | Optimistic locking version |

**Current value:** `schema.version` = `7.0.0.0` (matches Flowable version in pom.xml).

#### `act_ge_bytearray`
Binary large objects: BPMN XML, deployment resources, serialized variables.

| Column | Type | Meaning |
|--------|------|---------|
| `id_` | VARCHAR(64) | Byte array UUID |
| `rev_` | INT | Optimistic locking |
| `name_` | VARCHAR(255) | Resource name (e.g., `ai-agent-execution.bpmn20.xml`) |
| `deployment_id_` | VARCHAR(64) | FK to deployment |
| `bytes_` | LONGBLOB / BYTEA | Binary content |

## SchemaPlexAI → Flowable Integration Map

```
sf_workflow_instance
    id              ←→  act_ru_execution.business_key_
    proc_inst_id    ←→  act_ru_execution.id_
    tenant_id       ←→  act_ru_execution.tenant_id_
    status          ←→  Derived from act_hi_procinst.end_time_
    start_time      ←→  act_hi_procinst.start_time_
    end_time        ←→  act_hi_procinst.end_time_

sf_workflow_node_execution
    workflow_inst_id ←→ act_ru_execution.proc_inst_id_
    node_id          ←→ act_hi_actinst.act_id_
    node_type        ←→ act_hi_actinst.act_type_
    start_time       ←→ act_hi_actinst.start_time_
    end_time         ←→ act_hi_actinst.end_time_
    input_json       ←→ act_hi_varinst.text_ (variable snapshot)
    output_json      ←→ NodeExecutionResult from WorkflowNodeEngine
```

## Java Delegate Mapping

| Delegate Class | BPMN Element | Flowable Table Activity |
|----------------|--------------|------------------------|
| `AiAgentInitDelegate` | `startEvent` listener, `initAgentTask` service task | Inserts into `act_hi_actinst` as `serviceTask` |
| `AiAgentExecutionDelegate` | `executeAgentTask`, `reExecuteAgentTask` | Reads/writes `act_ru_variable` |
| `AiAgentResultProcessorDelegate` | `processResultTask`, `finalizeTask`, `successEndEvent` listener | Sets variables, transitions to end |
| `SpecReviewInitDelegate` | `startEvent` listener, `initReviewTask` | Classifies risk level |
| `SpecReviewNotificationDelegate` | `autoApproveTask`, `notifyRejectionTask`, `notifyApprovalTask` | Sends notifications |
| `HumanTaskAssignmentDelegate` | `humanReviewTask`, `primaryReviewTask`, `escalatedReviewTask` task listeners | Populates `act_ru_identitylink` |
| `FlowableDelegateAdapter` | Generic service tasks | Bridges to `WorkflowNodeEngine` |

## Multi-Tenancy

Flowable tables support tenant isolation via the `tenant_id_` column:
- Deployments can be tenant-scoped
- Process definitions can be tenant-scoped
- Runtime executions inherit tenant from deployment
- SchemaPlexAI sets `tenantId` process variable and relies on `TenantContextHolder` in delegates

## Maintenance Notes

- **History cleanup:** Flowable supports history cleanup jobs. Configure `flowable.history.cleaning.after.days` to purge old `act_hi_*` rows.
- **Dead letter jobs:** Monitor `act_ru_deadletter_job` for failed async tasks. Retry via Flowable Admin or REST API.
- **Schema upgrades:** Flowable handles schema migrations automatically on version bumps. Never manually alter `act_*` tables.
- **Performance:** `act_ru_*` tables stay small (only active data). `act_hi_*` grows unbounded — plan archiving strategy.

## Backlinks

- See [[services/workflow-node-engine]] for node execution logic
- See [[services/flowable-delegate-adapter]] for JavaDelegate bridge
- See [[entities/workflow]] for SchemaPlexAI workflow domain entities
- See [[wiki/technical-debt]] for horizontal scaling considerations
