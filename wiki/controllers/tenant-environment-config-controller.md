<!-- AUTO-GENERATED: manual-fix at 2026-05-07T19:00:17Z -->
---
title: TenantEnvironmentConfigController
type: controller
source: schemaplexai-web/src/main/java/com/schemaplexai/web/controller/config/TenantEnvironmentConfigController.java
creation_date: 2026-05-08
update_date: 2026-05-08
tags: [controller, tenant, environment, security, config]
confidence: high
---

# TenantEnvironmentConfigController

> CRUD controller for tenant environment security configuration. Manages per-tenant security policies including tool whitelists, operation permissions, and concurrency limits.

## Base Path

`/web/tenant-env-configs` (routed via Gateway to `schemaplexai-web` port 8082)

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Page list tenant environment configs |
| GET | `/{id}` | Get config by ID |
| GET | `/tenant/{tenantId}` | Get config by tenant ID |
| POST | `/` | Create a new config |
| PUT | `/{id}` | Update config |
| PATCH | `/{id}/refresh` | Refresh tenant environment config cache |
| DELETE | `/{id}` | Delete config |

## Service

- `TenantEnvironmentConfigService` in `schemaplexai-agent-config`

## Entity

- `TenantEnvironmentConfig` — see [[tenant-environment-config]]

## Notes

- The `PATCH /{id}/refresh` endpoint invalidates cached security policy for the given tenant.
- Tenant environment configs are stored in the global table `sf_tenant_environment_config`.
