---
title: ConfigService
type: service
source: schemaplexai-system/src/main/java/com/schemaplexai/system/service/ConfigService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, system, config, tenant]
confidence: high
---

# ConfigService

> One-sentence summary: Manages tenant-scoped configuration key-value pairs, providing lookup by config key and tenant ID for dynamic system settings.

## Responsibilities

1. Retrieve configuration values by key and tenant ID
2. Manage tenant-scoped configuration records via CRUD operations
3. Support multi-tenant configuration isolation

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `getConfigValue` | Get a configuration value by key and tenant ID | `configKey` — the configuration key; `tenantId` — the tenant identifier | `String` — the config value, or null if not found |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `ConfigService` | `schemaplexai-system/src/main/java/com/schemaplexai/system/service/ConfigService.java` | Service class extending `ServiceImpl<SfConfigMapper, SfConfig>` |
| `SfConfig` | `schemaplexai-system/src/main/java/com/schemaplexai/system/entity/SfConfig.java` | Entity: `tenantId`, `configKey`, `configValue` |
| `SfConfigMapper` | `schemaplexai-system/src/main/java/com/schemaplexai/system/mapper/SfConfigMapper.java` | MyBatis-Plus mapper with `selectByKeyAndTenantId` |

## Dependencies / Collaborators

- **MyBatis-Plus** — `ServiceImpl` provides full CRUD, pagination, and query helpers
- **SfConfig entity** — stores configs in `sf_config` table
- **SfConfigMapper** — custom mapper with `selectByKeyAndTenantId` method
- **TenantService** — provides tenant context for scoped lookups

## Backlinks

- [[services/tenant-service]] — provides tenant context for config isolation
- [[services/system-settings]] — UI/admin settings that interact with configs
- [[entities/config]] — configuration entity
