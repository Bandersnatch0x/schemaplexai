---
title: ClickHouseCostSchema
type: service
source: docker/clickhouse/init/01-cost-analytics.sql
creation_date: 2026-05-06
update_date: 2026-05-06
tags: [service, clickhouse, cost, analytics, ops, schema, olap]
confidence: high
---

# ClickHouseCostSchema

> One-sentence summary: ClickHouse OLAP schema for storing and aggregating LLM API call costs, with four tables and two auto-aggregating materialized views for hourly and daily rollups.

## Responsibilities

1. Store every LLM API call as a granular cost record with token usage and cost breakdown
2. Auto-aggregate cost data into hourly model-usage summaries via materialized view
3. Auto-aggregate token consumption into daily tenant-level summaries via materialized view
4. Track per-agent execution cost and token metrics for chargeback analysis
5. Apply time-based TTL for automatic data expiration

## Schema Overview

### 4 Tables

| Table | Engine | Purpose |
|-------|--------|---------|
| `sf_cost_record` | MergeTree | Main fact table — one row per LLM API call |
| `sf_model_usage_hourly` | SummingMergeTree | Hourly aggregates per model (auto-populated by MV) |
| `sf_token_consumption_daily` | SummingMergeTree | Daily aggregates per tenant (auto-populated by MV) |
| `sf_agent_execution_cost` | MergeTree | Per-execution cost summary for chargeback |

### 2 Materialized Views

| View | Source | Target | Aggregation |
|------|--------|--------|-------------|
| `mv_model_usage_hourly` | `sf_cost_record` | `sf_model_usage_hourly` | GROUP BY tenant_id, model_name, provider, hour_bucket |
| `mv_token_consumption_daily` | `sf_cost_record` | `sf_token_consumption_daily` | GROUP BY tenant_id, date |

## Key Files

| File | Path | Role |
|------|------|------|
| `01-cost-analytics.sql` | `docker/clickhouse/init/01-cost-analytics.sql` | DDL for all tables, views, and indexes |
| `CostRecord` | `schemaplexai-ops/src/main/java/com/schemaplexai/ops/entity/CostRecord.java` | POJO for JDBC access (not JPA entity) |
| `ClickHouseConfig` | `schemaplexai-ops/src/main/java/com/schemaplexai/ops/config/ClickHouseConfig.java` | DataSource bean, gated by `clickhouse.enabled` |
| `ClickHouseCostSyncService` | `schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/ClickHouseCostSyncService.java` | Scheduled PG → ClickHouse incremental sync |

### sf_cost_record (Main Fact Table)

```sql
ENGINE = MergeTree()
PARTITION BY toYYYYMMDD(created_at)
ORDER BY (tenant_id, service_name, created_at)
TTL created_at + INTERVAL 1 YEAR
```

Key columns: `tenant_id`, `record_id`, `service_name`, `model_name`, `provider`, `request_type`, `input_tokens`, `output_tokens`, `total_tokens`, `cost_amount`, `currency`, `created_at`, `execution_id`, `agent_id`, `workflow_instance_id`

Indexes: `bloom_filter` on `model_name` and `provider` (GRANULARITY 3)

### sf_model_usage_hourly (Hourly Aggregation)

```sql
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMMDD(hour_bucket)
ORDER BY (tenant_id, model_name, provider, hour_bucket)
TTL hour_bucket + INTERVAL 1 YEAR
```

Aggregated columns: `total_requests`, `total_input_tokens`, `total_output_tokens`, `total_tokens`, `avg_latency_ms`, `total_cost`

Populated automatically by `mv_model_usage_hourly` which reads from `sf_cost_record` and groups by hour.

### sf_token_consumption_daily (Daily Aggregation)

```sql
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(date)
ORDER BY (tenant_id, date)
TTL date + INTERVAL 2 YEAR
```

Aggregated columns: `total_input_tokens`, `total_output_tokens`, `total_tokens`, `total_cost`, `unique_models_count`

Populated automatically by `mv_token_consumption_daily` which reads from `sf_cost_record` and groups by calendar date.

### sf_agent_execution_cost (Per-Execution)

```sql
ENGINE = MergeTree()
PARTITION BY toYYYYMMDD(created_at)
ORDER BY (tenant_id, agent_id, created_at)
TTL created_at + INTERVAL 1 YEAR
```

Columns: `tenant_id`, `agent_id`, `execution_id`, `workflow_instance_id`, `total_cost`, `total_tokens`, `tool_call_count`, `llm_call_count`, `duration_ms`, `created_at`

Index: `bloom_filter` on `agent_id` (GRANULARITY 3)

### CostRecord POJO

Plain POJO (not a JPA entity) used by `ClickHouseCostSyncService` and `CostService` for JDBC-based reads/writes. Maps directly to `schemaplexai_costs.sf_cost_record` columns.

### Conditional Enablement

```yaml
clickhouse:
  enabled: false  # default; set to true to enable DataSource and sync service
```

`ClickHouseConfig` creates the DataSource only when `clickhouse.enabled=true`. `ClickHouseCostSyncService` is also gated by the same property and runs a scheduled incremental sync every 5 minutes (`@Scheduled(fixedDelay = 300_000)`).

## Related

- [[services/execution-admission-service]] — may gate executions based on cost budgets
- [[entities/ops]] — ops module entities (budget, artifact, evaluation)
- [[services/agent-execution-engine]] — produces execution records that generate cost data
- [[services/agent-execution-lifecycle-service]] — tracks execution state for cost attribution
- [[dependencies]] — infrastructure dependency matrix including ClickHouse
