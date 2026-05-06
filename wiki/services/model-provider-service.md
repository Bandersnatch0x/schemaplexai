---
title: ModelProviderService
type: service
source: schemaplexai-system/src/main/java/com/schemaplexai/system/service/ModelProviderService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, system, ai, provider, llm]
confidence: high
---

# ModelProviderService

> One-sentence summary: Provides CRUD operations for AI model provider configurations, managing external LLM API endpoints and rate limits.

## Responsibilities

1. Manage model provider records (create, read, update, delete)
2. Store provider metadata including API base URL and rate limits
3. Provide pagination and query helpers via MyBatis-Plus `ServiceImpl`

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| *(inherited)* | All CRUD operations inherited from `ServiceImpl<SfModelProviderMapper, SfModelProvider>` | — | — |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `ModelProviderService` | `schemaplexai-system/src/main/java/com/schemaplexai/system/service/ModelProviderService.java` | Service class extending `ServiceImpl<SfModelProviderMapper, SfModelProvider>` |
| `SfModelProvider` | `schemaplexai-system/src/main/java/com/schemaplexai/system/entity/SfModelProvider.java` | Entity: `name`, `code`, `apiBaseUrl`, `status`, `rateLimit` |
| `SfModelProviderMapper` | `schemaplexai-system/src/main/java/com/schemaplexai/system/mapper/SfModelProviderMapper.java` | MyBatis-Plus mapper |

## Dependencies / Collaborators

- **MyBatis-Plus** — `ServiceImpl` provides full CRUD, pagination, and query helpers
- **SfModelProvider entity** — stores provider configs in `sf_model_provider` table

## Backlinks

- [[services/ai-model-service]] — references providers via `providerId`
- [[services/agent-execution-lifecycle-service]] — uses providers for LLM API calls
- [[entities/model-provider]] — model provider entity
