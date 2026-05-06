---
title: ClickHouseCostSyncService
type: service
source: schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/ClickHouseCostSyncService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, ops, clickhouse, sync, etl, cost, analytics]
confidence: high
---

# ClickHouseCostSyncService

> One-sentence summary: Scheduled ETL service that incrementally syncs agent execution records from PostgreSQL to ClickHouse for cost analytics.

## Responsibilities

1. Incrementally sync `sf_agent_execution` rows from PostgreSQL to ClickHouse
2. Track sync progress with a cursor (`SfSyncCursor`) to enable resume after failure
3. Log batch operations with `SfSyncBatchLog` for observability
4. Handle partial batch failures without advancing the cursor past successfully synced records
5. Skip execution when ClickHouse is disabled via configuration

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `syncIncrementalData` | Scheduled incremental sync (runs every 5 minutes) | — | `void` |

## Private Helpers

| Method | Description |
|--------|-------------|
| `createBatchLog` | Create a `RUNNING` batch log entry |
| `getOrCreateCursor` | Retrieve or initialize the sync cursor for `sf_agent_execution` |
| `fetchExecutionRecords` | Query PostgreSQL for records with `id > cursor.lastSyncId` |
| `insertIntoClickHouse` | Batch insert records into ClickHouse with per-row result tracking |
| `updateCursor` | Advance cursor to the max successfully synced ID |
| `completeBatchLog` | Mark batch log as `COMPLETED` |
| `failBatchLog` | Mark batch log as `FAILED` with error message |

## Dependencies / Collaborators

- **SyncCursorMapper** — cursor persistence for incremental sync tracking
- **SyncBatchLogMapper** — batch operation logging
- **JdbcTemplate (PostgreSQL)** — source database queries
- **DataSource (ClickHouse)** — target database writes
- **Configuration**: `clickhouse.enabled` property gates execution

## Key Code

```java
@Scheduled(fixedDelay = 300_000)
@Transactional(rollbackFor = Exception.class)
public void syncIncrementalData() {
    // Fetches records with id > cursor.lastSyncId, batch inserts to ClickHouse,
    // advances cursor only on full success
}
```

## Internal Types

- `ExecutionRecord` — immutable record mapping `sf_agent_execution` columns
- `BatchInsertResult` — tracks success count and failed record IDs from batch insert

## Known Issues

- **No backfill support** — only incremental forward sync; historical backfill requires manual cursor reset
- **Fixed batch size** — `DEFAULT_BATCH_SIZE = 1000` is not configurable
- **Single table sync** — cursor key is hardcoded to `sf_agent_execution`

## Related

- [[services/cost-service]] — consumes synced data for cost analytics
- [[services/budget-service]] — budget limits checked against cost data
- [[services/clickhouse-cost-schema]] — ClickHouse schema definitions
- [[services/agent-execution-lifecycle-service]] — produces execution records
