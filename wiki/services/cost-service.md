---
title: CostService
type: service
source: schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/CostService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, ops, cost, analytics, budget, alert]
confidence: high
---

# CostService

> One-sentence summary: Queries tenant-scoped cost metrics and monitors budget thresholds for alerting.

## Responsibilities

1. Query cost metrics by tenant (total, today, month) — currently returns placeholder zeros
2. Check all budgets against their limits and alert thresholds
3. Log warnings when budgets are exceeded or thresholds are reached

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `queryCostByTenant` | Query cost metrics for a tenant | `tenantId` (String) | `Map<String, BigDecimal>` |
| `checkBudgetAlerts` | Evaluate all budgets and log alert warnings | — | `void` |

## Dependencies / Collaborators

- **BudgetMapper** — queries all budgets for threshold checking

## Known Issues

- **Placeholder cost data** — `queryCostByTenant` returns zeros; Phase 2 will query ClickHouse for actual cost analytics
- **No notification dispatch** — budget alerts are logged only; no integration with `NotificationService` yet

## Related

- [[services/clickhouse-cost-sync-service]] — syncs execution data to ClickHouse for actual cost analytics
- [[services/budget-service]] — manages budget allocations and limits
- [[services/notification-service]] — should receive budget alert notifications
