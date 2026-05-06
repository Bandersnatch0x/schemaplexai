---
title: BudgetService
type: service
source: schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/BudgetService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, ops, budget, cost, finance]
confidence: high
---

# BudgetService

> One-sentence summary: Manages budget allocation, usage tracking, limit checking, and tenant-scoped budget queries.

## Responsibilities

1. Allocate new budgets with limit amounts
2. Check if a budget has exceeded its configured limit
3. Calculate budget usage as a ratio (0.0 to 1.0+)
4. List budgets scoped by tenant
5. Update budget allocation amounts

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `allocateBudget` | Allocate a new budget | `budget` (SfBudget) | `SfBudget` |
| `checkBudgetLimit` | Check if a budget has exceeded its limit | `budgetId` (Long) | `boolean` |
| `getBudgetUsage` | Get budget usage as a decimal ratio | `budgetId` (Long) | `BigDecimal` |
| `listBudgetsByTenant` | List budgets for a tenant | `tenantId` (String) | `List<SfBudget>` |
| `updateBudgetAllocation` | Update a budget's limit amount | `budgetId` (Long), `newLimitAmount` (BigDecimal) | `SfBudget` |

## Dependencies / Collaborators

- **Entity**: `SfBudget` — budget persistence via MyBatis-Plus `IService`

## Related

- [[services/cost-service]] — queries and analyzes cost data against budgets
- [[services/clickhouse-cost-sync-service]] — syncs execution data for cost analytics
- [[services/notification-service]] — may send budget alert notifications
- [[entities/ops]] — ops domain entities
