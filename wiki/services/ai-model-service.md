---
title: AiModelService
type: service
source: schemaplexai-system/src/main/java/com/schemaplexai/system/service/AiModelService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, system, ai, model, llm]
confidence: high
---

# AiModelService

> One-sentence summary: Provides CRUD operations for AI model configurations, managing registered LLM models available for agent execution and inference tasks.

## Responsibilities

1. Manage AI model records (create, read, update, delete)
2. Provide pagination and query helpers via MyBatis-Plus `ServiceImpl`
3. Associate models with providers via `providerId`

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| *(inherited)* | All CRUD operations inherited from `ServiceImpl<SfAiModelMapper, SfAiModel>` | — | — |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `AiModelService` | `schemaplexai-system/src/main/java/com/schemaplexai/system/service/AiModelService.java` | Service class extending `ServiceImpl<SfAiModelMapper, SfAiModel>` |
| `SfAiModel` | `schemaplexai-system/src/main/java/com/schemaplexai/system/entity/SfAiModel.java` | Entity: `tenantId`, `name`, `providerId`, `modelCode`, `status` |
| `SfAiModelMapper` | `schemaplexai-system/src/main/java/com/schemaplexai/system/mapper/SfAiModelMapper.java` | MyBatis-Plus mapper |

## Dependencies / Collaborators

- **MyBatis-Plus** — `ServiceImpl` provides full CRUD, pagination, and query helpers
- **SfAiModel entity** — stores AI model configs in `sf_ai_model` table
- **ModelProviderService** — referenced via `providerId` for provider details

## Backlinks

- [[services/model-provider-service]] — provider management for AI models
- [[services/agent-execution-lifecycle-service]] — consumes AI model configs for inference
- [[entities/ai-model]] — AI model entity
