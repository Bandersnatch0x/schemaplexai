---
title: AiModelController
type: controller
source: schemaplexai-system/src/main/java/com/schemaplexai/system/controller/AiModelController.java
creation_date: 2026-05-01
tags: [system, ai-model, controller, crud]
confidence: high
---

# AiModelController

> CRUD controller for AI model configurations within tenants.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/system/models` | Paginated list of AI models |
| GET | `/system/models/{id}` | Get AI model details by ID |
| POST | `/system/models` | Create a new AI model config |
| PUT | `/system/models/{id}` | Update an AI model by ID |
| DELETE | `/system/models/{id}` | Delete an AI model by ID |

## DTO / Entity

- **Request/Response**: `SfAiModel` entity
  - `tenantId` (String): Owning tenant
  - `name` (String): Model display name
  - `providerId` (Long): Reference to `SfModelProvider`
  - `modelCode` (String): Provider-specific model code (e.g., `gpt-4o`, `claude-3-sonnet`)
  - `status` (Integer): Model status
  - Inherits `BaseEntity`

## Service Dependencies

- `AiModelService` — MyBatis-Plus `IService` for `SfAiModel`

## Swagger Tags

- `@Tag(name = "AI模型管理")`

## Notes

- Models are linked to providers via `providerId`.
- `modelCode` is the string passed to the provider API (e.g., OpenAI model name).
