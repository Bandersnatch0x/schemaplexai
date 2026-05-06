---
title: TenantEnvironmentConfigService
type: service
source: schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/impl/TenantEnvironmentConfigServiceImpl.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, agent-config, tenant, environment, security, multi-tenant]
confidence: high
---

# TenantEnvironmentConfigService

> One-sentence summary: Manages per-tenant environment and security configurations, bridging CRUD operations with the SecurityPolicyLoader cache for runtime policy enforcement.

## Responsibilities

1. Persist tenant environment configurations via MyBatis-Plus `ServiceImpl`
2. Query configurations by tenant ID for runtime security policy lookups
3. Delegate cache refresh to `SecurityPolicyLoader` when configurations change
4. Validate required fields (`tenantId`) on create and update operations
5. Enforce existence checks before updates to prevent silent no-ops

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `getByTenantId` | Retrieve a tenant environment config by its tenant identifier | `tenantId` — tenant identifier | `TenantEnvironmentConfig` — matching config, or null |
| `refreshCache` | Invalidate the cached config for a tenant in `SecurityPolicyLoader` | `tenantId` — tenant identifier | void |
| `pageList` | Paginated list of all tenant environment configs, ordered by creation time desc | `page` — MyBatis-Plus page parameter | `IPage<TenantEnvironmentConfig>` |
| `save` | Create a new tenant environment config with tenantId validation | `entity` — config to create | boolean — success indicator |
| `updateById` | Update an existing config with existence and id validation | `entity` — config to update | boolean — success indicator |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `TenantEnvironmentConfigService` | `schemaplexai-agent-config/.../service/TenantEnvironmentConfigService.java` | Service interface extending `IService<TenantEnvironmentConfig>` |
| `TenantEnvironmentConfigServiceImpl` | `schemaplexai-agent-config/.../service/impl/TenantEnvironmentConfigServiceImpl.java` | Service implementation extending `ServiceImpl<TenantEnvironmentConfigMapper, TenantEnvironmentConfig>` |
| `TenantEnvironmentConfig` | `schemaplexai-model/.../entity/config/TenantEnvironmentConfig.java` | Entity: `tenantId`, `environment`, `securityLevel`, `allowedTools`, `allowHttpCalls`, `allowFileRead`, `allowIrreversibleOps`, `maxConcurrentToolCalls`, `extraConfig` |
| `TenantEnvironmentConfigMapper` | `schemaplexai-dao/.../mapper/TenantEnvironmentConfigMapper.java` | MyBatis-Plus mapper extending `BaseMapperX` |
| `TenantEnvironmentConfigController` | `schemaplexai-web/.../controller/config/TenantEnvironmentConfigController.java` | REST controller exposing CRUD + refresh endpoints |

## Error Handling

- `save` throws `BaseException` with `ResultCode.PARAM_ERROR` if `tenantId` is null or blank
- `updateById` throws `BaseException` with `ResultCode.PARAM_ERROR` if `id` is null
- `updateById` throws `BaseException` with `ResultCode.NOT_FOUND` if the config does not exist

## Dependencies / Collaborators

- **MyBatis-Plus** — `ServiceImpl` provides full CRUD, pagination, and query helpers
- **SecurityPolicyLoader** — caffeine-backed cache for runtime security policy loading; receives `refresh()` calls on cache invalidation
- **TenantEnvironmentConfig entity** — stored in `sf_tenant_environment_config` table (global table, excluded from tenant interceptor)
- **TenantEnvironmentConfigMapper** — data access layer extending `BaseMapperX`

## Backlinks

- [[services/security-policy-loader]] — loads and caches tenant environment configs at runtime; cache is invalidated via this service
- [[services/tenant-service]] — manages tenant records; tenant IDs from this service are referenced here
- [[services/security-policy-service]] — security policy definitions may reference environment configs
- [[services/tool-approval-service]] — tool usage approvals may be constrained by environment config settings
- [[entities/config]] — config domain entities
