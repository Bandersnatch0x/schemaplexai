---
title: BudgetController
type: controller
source: schemaplexai-ops/src/main/java/com/schemaplexai/ops/controller/BudgetController.java
creation_date: 2026-05-01
tags: [ops, budget, cost, controller, crud]
confidence: high
---

# BudgetController

> CRUD controller for budget definitions (cost limits and thresholds).

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/ops/budgets` | Create a new budget |
| PUT | `/ops/budgets/{id}` | Update an existing budget by ID |
| DELETE | `/ops/budgets/{id}` | Delete a budget by ID |
| GET | `/ops/budgets/{id}` | Get a single budget by ID |
| GET | `/ops/budgets` | List all budgets |

## DTO / Entity

- **Request/Response**: `SfBudget` entity
  - `budgetType` (String): Type of budget (tenant, agent, user, etc.)
  - `limitAmount` (BigDecimal): Maximum allowed spending
  - `usedAmount` (BigDecimal): Current spending
  - `alertThreshold` (BigDecimal): Threshold for alert generation
  - Inherits `BaseEntity`

## Service Dependencies

- `BudgetService` — MyBatis-Plus `IService` for `SfBudget`

## Notes

- Budgets are enforced by `ExecutionAdmissionService` before agent executions start.
- Alerts are generated when `usedAmount` approaches `alertThreshold`.
