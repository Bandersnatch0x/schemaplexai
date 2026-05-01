---
title: ModelProviderController
type: controller
source: schemaplexai-system/src/main/java/com/schemaplexai/system/controller/ModelProviderController.java
creation_date: 2026-05-01
tags: [system, model-provider, controller, crud]
confidence: high
---

# ModelProviderController

> CRUD controller for AI model provider configurations.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/system/model-providers` | Paginated list of model providers |
| GET | `/system/model-providers/{id}` | Get provider details by ID |
| POST | `/system/model-providers` | Create a new model provider |
| PUT | `/system/model-providers/{id}` | Update a provider by ID |
| DELETE | `/system/model-providers/{id}` | Delete a provider by ID |

## DTO / Entity

- **Request/Response**: `SfModelProvider` entity
  - `name` (String): Provider name
  - `code` (String): Unique provider code
  - `apiBaseUrl` (String): Provider API base URL
  - `status` (Integer): Provider status
  - `rateLimit` (Integer): Requests per minute limit
  - Inherits `BaseEntity`

## Service Dependencies

- `ModelProviderService` — MyBatis-Plus `IService` for `SfModelProvider`

## Swagger Tags

- `@Tag(name = "模型供应商管理")`

## Notes

- Providers are global (not tenant-scoped) since they represent external API endpoints.
- API keys are stored in `config_json` at the database level (see `sf_model_provider` schema).
