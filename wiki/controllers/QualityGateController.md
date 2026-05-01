---
title: QualityGateController
type: controller
source: schemaplexai-quality/src/main/java/com/schemaplexai/quality/controller/QualityGateController.java
creation_date: 2026-05-01
tags: [quality, gate, controller, crud]
confidence: high
---

# QualityGateController

> CRUD controller for quality gate definitions (rule collections for execution gating).

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/quality/gates` | Create a new quality gate |
| PUT | `/quality/gates/{id}` | Update an existing gate by ID |
| DELETE | `/quality/gates/{id}` | Delete a gate by ID |
| GET | `/quality/gates/{id}` | Get a single gate by ID |
| GET | `/quality/gates` | List all quality gates |

## DTO / Entity

- **Request/Response**: `SfQualityGate` entity
  - `name` (String): Gate name
  - `rulesJson` (String): JSON array of rule names to evaluate
  - `status` (Integer): Gate status
  - Inherits `BaseEntity`

## Service Dependencies

- `QualityGateService` — MyBatis-Plus `IService` for `SfQualityGate`

## Notes

- `rulesJson` is a JSON list of strings referencing `QualityRule` implementations by name.
- Evaluated at runtime by `QualityOrchestrator`.
