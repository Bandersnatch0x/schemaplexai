---
title: ObservabilitySpan
type: entity
source: schemaplexai-model/src/main/java/com/schemaplexai/model/entity/observability/ObservabilitySpan.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [entity, observability, span, tracing, telemetry]
confidence: high
---

# ObservabilitySpan

> One-sentence summary: Distributed tracing span capturing individual operation timing, I/O, model usage, and cost details within an AI execution trace.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key (auto-assigned Snowflake ID) |
| tenantId | String | Multi-tenant isolation field |
| spanId | String | Unique span identifier (UUID format) |
| traceId | String | Parent trace identifier (groups related spans) |
| parentSpanId | String | Reference to parent span for nested operations |
| name | String | Span name / operation label (e.g., "llm.invoke", "tool.call") |
| type | String | Span type classification |
| startTime | Long | Span start timestamp (epoch millis) |
| endTime | Long | Span end timestamp (epoch millis) |
| input | String | Serialized input payload |
| output | String | Serialized output payload |
| metadata | String | Additional span metadata (JSON) |
| status | String | Execution status (OK, ERROR, etc.) |
| model | String | LLM model identifier used (e.g., "gpt-4o") |
| modelParameters | String | Model invocation parameters (JSON) |
| usageDetails | String | Token usage details (JSON) |
| costDetails | String | Cost breakdown (JSON) |
| promptName | String | Named prompt template reference |
| promptVersion | String | Prompt template version |
| createdAt | LocalDateTime | Creation timestamp |
| updatedAt | LocalDateTime | Last update timestamp |
| createdBy | Long | Creator user ID |
| updatedBy | Long | Last updater user ID |
| deleted | Integer | Soft-delete flag (0 = active, 1 = deleted) |

## Relationships

- **traceId** → `sf_observability_trace.traceId` — belongs to a trace
- **parentSpanId** → self-referencing span hierarchy

## Backlinks

- Parent trace entity: [[entities/observability-trace]]
- Observability and cost analytics: [[services/ops]]
- See [[data-model]] for full schema
