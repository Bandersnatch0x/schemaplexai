---
title: sf_config
type: entity
source: docker/postgres/init/01-init-schema.sql
creation_date: 2026-05-01
tags: [system, config, key-value, schema]
confidence: high
---

# sf_config

> Tenant-scoped system configuration table — key-value pairs for tenant-specific settings.

## Schema

```sql
CREATE TABLE sf_config (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    config_key      VARCHAR(128) NOT NULL,
    config_value    TEXT,
    description     VARCHAR(256),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0,
    UNIQUE(tenant_id, config_key)
);
```

## Fields

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | BIGSERIAL | No | auto | Primary key |
| tenant_id | BIGINT | No | — | Tenant identifier |
| config_key | VARCHAR(128) | No | — | Configuration key (unique per tenant) |
| config_value | TEXT | Yes | — | Configuration value |
| description | VARCHAR(256) | Yes | — | Human-readable description |
| created_at | TIMESTAMP | No | CURRENT_TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | No | CURRENT_TIMESTAMP | Last update time |
| deleted | INT | No | 0 | Soft-delete flag |

## Constraints

- **Unique**: `(tenant_id, config_key)`

## Java Entity

- `SfConfig` in `schemaplexai-system`
  - `tenantId` (String)
  - `configKey` (String)
  - `configValue` (String)

## Controller

- [[ConfigController]] — `/system/configs`

## Notes

- Used for tenant-specific feature flags, thresholds, and custom settings.
- The `ConfigService` implementation is a standard MyBatis-Plus `IService` with no custom logic.
