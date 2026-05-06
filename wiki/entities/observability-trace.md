---
title: ObservabilityTrace
type: entity
source: schemaplexai-model/src/main/java/com/schemaplexai/model/entity/observability/ObservabilityTrace.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [entity, observability, trace, telemetry, distributed-tracing]
confidence: high
---

# ObservabilityTrace

> One-sentence summary: Top-level distributed trace capturing end-to-end AI execution flows including input, output, session context, and metadata.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key (auto-assigned Snowflake ID) |
| tenantId | String | Multi-tenant isolation field |
| traceId | String | Unique trace identifier (UUID format) |
| name | String | Trace name / operation label |
| userId | String | Initiating user identifier |
| sessionId | String | Session identifier for grouping related traces |
| input | String | Serialized initial request payload |
| output | String | Serialized final response payload |
| metadata | String | Additional trace metadata (JSON) |
| tags | String | Searchable tag labels (JSON array or comma-separated) |
| version | String | Trace schema / instrumentation version |
| createdAt | LocalDateTime | Creation timestamp |
| updatedAt | LocalDateTime | Last update timestamp |
| createdBy | Long | Creator user ID |
| updatedBy | Long | Last updater user ID |
| deleted | Integer | Soft-delete flag (0 = active, 1 = deleted) |

## Relationships

- One trace has many spans: `sf_observability_span.traceId` → `traceId`
- **userId** → `sf_user.id` (logical reference)

## Backlinks

- Child span entity: [[entities/observability-span]]
- Agent execution engine: [[services/agent-execution-engine]]
- Observability and cost analytics: [[services/ops]]
- See [[data-model]] for full schema
