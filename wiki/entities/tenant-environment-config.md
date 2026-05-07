<!-- AUTO-GENERATED: manual-fix at 2026-05-07T19:00:18Z -->
---
title: TenantEnvironmentConfig
type: entity
source: schemaplexai-model/src/main/java/com/schemaplexai/model/entity/config/TenantEnvironmentConfig.java
creation_date: 2026-05-08
update_date: 2026-05-08
tags: [entity, tenant, environment, security, config, global-table]
confidence: high
---

# TenantEnvironmentConfig (sf_tenant_environment_config)

> Tenant environment security configuration — per-tenant security policies controlling tool access, operation permissions, and concurrency limits.

## Schema

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | BIGSERIAL | No | auto | Primary key |
| tenant_id | VARCHAR(64) | No | — | Tenant identifier (data only — global table) |
| environment | VARCHAR(32) | Yes | — | Environment: dev / staging / prod |
| allowed_tools | TEXT | Yes | — | JSON array of permitted tool names |
| security_level | VARCHAR(32) | Yes | — | LOW / MEDIUM / HIGH / CRITICAL |
| allow_http_calls | BOOLEAN | Yes | TRUE | Whether HTTP call tools are allowed |
| allow_file_read | BOOLEAN | Yes | TRUE | Whether file read tools are allowed |
| allow_irreversible_ops | BOOLEAN | Yes | FALSE | Whether irreversible operations are allowed |
| max_concurrent_tool_calls | INT | Yes | 10 | Maximum concurrent tool calls permitted |
| extra_config | TEXT | Yes | — | Additional configuration (JSON) |
| created_at | TIMESTAMP | No | CURRENT_TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | No | CURRENT_TIMESTAMP | Last update time |
| deleted | INT | No | 0 | Soft-delete flag |

## Java Entity

- `TenantEnvironmentConfig` in `schemaplexai-model`
  - `tenantId` (String)
  - `environment` (String)
  - `allowedTools` (String)
  - `securityLevel` (String)
  - `allowHttpCalls` (Boolean)
  - `allowFileRead` (Boolean)
  - `allowIrreversibleOps` (Boolean)
  - `maxConcurrentToolCalls` (Integer)
  - `extraConfig` (String)

## Important Rules

- **GLOBAL table** — NOT filtered by `TenantLineInterceptor`
- The `tenant_id` field serves as a data identifier, not a filtering condition
- Used by agent-engine to enforce runtime security policies per tenant

## Notes

- Security levels control the blast radius of agent-executed operations.
- `allowed_tools` whitelist prevents unauthorized tool invocation.
- `max_concurrent_tool_calls` protects against resource exhaustion.
