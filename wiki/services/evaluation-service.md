---
title: EvaluationService
type: service
source: schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/EvaluationService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, ops, evaluation, dataset, quality]
confidence: high
---

# EvaluationService

> One-sentence summary: Manages AI evaluation tasks including execution, result retrieval, and filtering by dataset or status.

## Responsibilities

1. Run evaluation tasks, updating their status to running
2. Retrieve evaluation results for completed tasks
3. List evaluation tasks filtered by dataset
4. List evaluation tasks filtered by status

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `runEvaluation` | Execute an evaluation task | `evalTaskId` (Long) | `SfEvalTask` |
| `getEvaluationResults` | Retrieve results for an evaluation task | `evalTaskId` (Long) | `SfEvalTask` |
| `listByDataset` | List evaluation tasks for a dataset | `datasetId` (Long) | `List<SfEvalTask>` |
| `listByStatus` | List evaluation tasks by status code | `status` (Integer) | `List<SfEvalTask>` |

## Dependencies / Collaborators

- **Entity**: `SfEvalTask` — evaluation task persistence via MyBatis-Plus `IService`

## Related

- [[services/quality-gate-service]] — quality gates may trigger or consume evaluation results
- [[services/artifact-service]] — evaluation outputs may be stored as artifacts
- [[entities/ops]] — ops domain entities
