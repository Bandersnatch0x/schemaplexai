---
title: EvaluationController
type: controller
source: schemaplexai-ops/src/main/java/com/schemaplexai/ops/controller/EvaluationController.java
creation_date: 2026-05-01
tags: [ops, evaluation, benchmark, controller, crud]
confidence: high
---

# EvaluationController

> CRUD controller for evaluation tasks (agent benchmarking and testing).

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/ops/evaluations` | Create a new evaluation task |
| PUT | `/ops/evaluations/{id}` | Update an existing evaluation by ID |
| DELETE | `/ops/evaluations/{id}` | Delete an evaluation by ID |
| GET | `/ops/evaluations/{id}` | Get a single evaluation by ID |
| GET | `/ops/evaluations` | List all evaluation tasks |

## DTO / Entity

- **Request/Response**: `SfEvalTask` entity
  - `datasetId` (Long): Reference to the evaluation dataset
  - `agentId` (Long): Agent being evaluated
  - `status` (Integer): Evaluation status
  - `resultJson` (String): JSON-serialized evaluation results
  - Inherits `BaseEntity`

## Service Dependencies

- `EvaluationService` — MyBatis-Plus `IService` for `SfEvalTask`

## Notes

- Evaluation tasks run benchmark datasets against agents to measure performance.
- `resultJson` stores metrics such as accuracy, latency, token usage, and cost.
