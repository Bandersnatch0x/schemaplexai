---
title: Schema Evolution
type: architecture
source: docker/postgres/init/*.sql
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [schema, migration, database, evolution]
confidence: high
---

# Schema Evolution

> One-sentence summary: Database schema is initialized via 3 ordered SQL scripts; no Flyway/Liquibase migration framework is currently used.

## Initialization Scripts

| Order | File | Purpose |
|-------|------|---------|
| 1 | `01-init-schema.sql` | Core governance (tenant/user/role/permission), AI model, Spec domain |
| 2 | `02-init-schema-agent.sql` | Agent domain, chat message (partitioned), agent memory |
| 3 | `03-init-schema-others.sql` | Workflow, Context, Quality, Integration, Ops, Sync/Idempotency |

## Key Schema Decisions

### Chat Message Partitioning
- `sf_chat_message` uses PostgreSQL HASH partitioning by `conversation_id` into 16 partitions
- Rationale: High write volume for conversational data, even distribution
- Archive table `sf_chat_message_archive` for cold data migration

### Idempotency & Message Reliability
- `sf_idempotency_key` for MQ consumer deduplication
- `sf_message_fail_log` with retry_count for dead-letter tracking
- `sf_sync_cursor` + `sf_sync_batch_log` for CDC-style sync to ClickHouse

### ShedLock
- `shedlock` table for distributed scheduled job locking

## Gaps & Questions

- No Flyway or Liquibase — schema changes require manual SQL script management
- No automated migration testing
- `act_*` tables (Flowable) are not in init scripts — created by Flowable auto-ddl

## Backlinks

- Full schema in [[data-model]]
- Database decisions in [[decisions]]
