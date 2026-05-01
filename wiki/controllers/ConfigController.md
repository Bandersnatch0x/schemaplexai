---
title: ConfigController
type: controller
source: schemaplexai-system/src/main/java/com/schemaplexai/system/controller/ConfigController.java
creation_date: 2026-05-01
tags: [system, config, controller, crud]
confidence: high
---

# ConfigController

> CRUD controller for tenant-scoped system configuration key-value pairs.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/system/configs` | Paginated list of system configs |
| GET | `/system/configs/{id}` | Get config details by ID |
| POST | `/system/configs` | Create a new config entry |
| PUT | `/system/configs/{id}` | Update a config by ID |
| DELETE | `/system/configs/{id}` | Delete a config by ID |

## DTO / Entity

- **Request/Response**: `SfConfig` entity
  - `tenantId` (String): Owning tenant
  - `configKey` (String): Configuration key (unique per tenant)
  - `configValue` (String): Configuration value
  - Inherits `BaseEntity`

## Service Dependencies

- `ConfigService` — MyBatis-Plus `IService` for `SfConfig`

## Swagger Tags

- `@Tag(name = "系统配置管理")`

## Notes

- `configKey` is unique per tenant (composite unique constraint: `tenant_id + config_key`).
- Used for tenant-specific feature flags, thresholds, and custom settings.
