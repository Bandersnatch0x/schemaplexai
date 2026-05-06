---
title: CostController
type: controller
source: schemaplexai-ops/src/main/java/com/schemaplexai/ops/controller/CostController.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [controller, cost, analytics, ops, tenant, budget]
confidence: high
---

# CostController

> One-sentence summary: Cost analytics controller providing tenant-level cost aggregation for AI operations including total, daily, and monthly spend metrics.

## Base Path

`/ops/costs` (routed via Gateway to `schemaplexai-ops` port 8089)

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Query cost analytics for a tenant |

## Key Request/Response Details

### GET `/`
- **Query param**: `tenantId` (required) — tenant identifier for cost aggregation
- **Response**: `Result<Map<String, BigDecimal>>` with the following keys:
  - `totalCost` — cumulative total cost
  - `todayCost` — cost accrued today
  - `monthCost` — cost accrued this month

## Dependencies

- `CostService costService` — handles cost aggregation logic
  - Phase 1: Returns placeholder zero values
  - Phase 2: Will query ClickHouse for actual cost analytics

## Notes

- Returns `Result<T>` wrapper (see [[architecture]])
- Currently returns placeholder data (`BigDecimal.ZERO` for all metrics)
- Future implementation will integrate with ClickHouse for real-time cost analytics
- Related budget alerting is handled by `CostService.checkBudgetAlerts()` (not exposed via REST)

## Backlinks

- Budget management: [[controllers/BudgetController]]
- Artifact management: [[controllers/ArtifactController]]
- Evaluation metrics: [[controllers/EvaluationController]]
- See [[routes]] for routing
