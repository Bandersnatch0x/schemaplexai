---
title: Sync & Idempotency Entities
type: model
source: docker/postgres/init/03-init-schema-others.sql
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [entity, sync, idempotency, mq, cdc]
confidence: high
---

# Sync & Idempotency Entities

> One-sentence summary: Infrastructure tables for CDC-style sync to ClickHouse, MQ idempotency, message failure tracking, and distributed locking.

## sf_sync_cursor
- sync_name, source_table, target_table, last_sync_id, last_sync_time
- sync_batch_size (default 1000), sync_interval_sec (default 60), failed_count, last_error

## sf_sync_batch_log
- sync_name, batch_id, start_id, end_id, record_count, status, error_msg

## sf_idempotency_key
- message_id, consumer_group, status, consumed_at
- UNIQUE(message_id, consumer_group)

## sf_message_fail_log
- message_id, exchange, routing_key, payload, error_msg
- consumer_group, status (PENDING), retry_count (default 0)

## shedlock
- name, lock_until, locked_at, locked_by
- For distributed scheduled job locking

## Backlinks

- Task module: `schemaplexai-task`
- See [[data-model]] for full schema
