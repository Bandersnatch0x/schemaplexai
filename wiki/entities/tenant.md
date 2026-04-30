---
title: Tenant Entity
type: model
source: docker/postgres/init/01-init-schema.sql
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [entity, tenant, multi-tenant]
confidence: high
---

# Tenant (sf_tenant)

> One-sentence summary: Multi-tenant root entity; global table (no tenant_id column) that all other tenant-scoped tables reference via tenant_id foreign key.

## Schema

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| name | VARCHAR(128) | Display name |
| code | VARCHAR(64) | Unique code |
| status | VARCHAR(32) | ACTIVE |
| config_json | TEXT | Flexible tenant config |

## Rules

- `sf_tenant` is **excluded** from `TenantLineInterceptor` filtering
- All other tables have `tenant_id` referencing this table
- Tenant ID extracted from `X-Tenant-Id` header at Gateway

## Backlinks

- Multi-tenant pattern in [[architecture]]
- See [[data-model]] for full schema
