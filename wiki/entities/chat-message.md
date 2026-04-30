---
title: Chat Message Entity
type: model
source: docker/postgres/init/02-init-schema-agent.sql
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [entity, chat, partitioning, postgresql]
confidence: high
---

# Chat Message (sf_chat_message)

> One-sentence summary: High-volume conversational data stored with PostgreSQL HASH partitioning across 16 partitions by conversation_id, plus a cold archive table.

## Schema

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | Non-unique across partitions |
| tenant_id | BIGINT | |
| conversation_id | VARCHAR(64) | Partition key |
| turn_index | INT | Message order in conversation |
| role | VARCHAR(32) | SYSTEM / USER / ASSISTANT / TOOL |
| content | TEXT | |
| tool_calls | JSONB | Structured tool call data |
| token_count | INT | Default 0 |
| created_at | TIMESTAMP | |

## Partitioning

```sql
PARTITION BY HASH (conversation_id)
-- 16 partitions: p0 through p15
-- MODULUS 16, REMAINDER 0..15
```

## Indexes

- `idx_chat_msg_conversation` on (conversation_id, turn_index)
- `idx_chat_msg_tenant` on (tenant_id, created_at)

## Archive

- `sf_chat_message_archive` — cold data with `archived_at` timestamp

## Backlinks

- Agent execution: [[services/agent-execution-engine]]
- See [[entities/agent]] for related agent entities
