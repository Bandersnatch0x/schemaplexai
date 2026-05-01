---
title: CostController
type: controller
source: schemaplexai-ops/src/main/java/com/schemaplexai/ops/controller/CostController.java
creation_date: 2026-05-01
tags: [ops, cost, analytics, controller]
confidence: high
---

# CostController

> Controller for cost analytics queries (tenant-level cost aggregation).

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/ops/costs` | Query cost summary for a tenant |

## Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| tenantId | String | Yes | Tenant identifier for cost aggregation |

## Response

- `Result<Map<String, BigDecimal>>` — Map of cost categories to amounts

## Service Dependencies

- `CostService` — Provides `queryCostByTenant(String tenantId)`

## Notes

- Single read-only endpoint for cost analytics.
- Costs are aggregated from agent execution token usage and stored in ClickHouse.
