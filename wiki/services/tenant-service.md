---
title: TenantService
type: service
source: schemaplexai-system/src/main/java/com/schemaplexai/system/service/TenantService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, system, tenant, multi-tenant]
confidence: high
---

# TenantService

> One-sentence summary: Manages tenant records with code-based lookup and status validation, serving as the foundation for multi-tenant data isolation across the platform.

## Responsibilities

1. Retrieve tenants by unique code
2. Validate tenant existence and active status
3. Manage tenant records via CRUD operations from `ServiceImpl`
4. Enforce tenant-level access control by rejecting disabled tenants

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `getByCode` | Retrieve a tenant by its unique code | `code` — tenant code | `SfTenant` — matching tenant, or null |
| `getValidTenant` | Retrieve a tenant by ID with existence and status validation | `id` — tenant ID | `SfTenant` — valid active tenant |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `TenantService` | `schemaplexai-system/src/main/java/com/schemaplexai/system/service/TenantService.java` | Service class extending `ServiceImpl<SfTenantMapper, SfTenant>` |
| `SfTenant` | `schemaplexai-system/src/main/java/com/schemaplexai/system/entity/SfTenant.java` | Entity: `name`, `code`, `status`, `configJson` |
| `SfTenantMapper` | `schemaplexai-system/src/main/java/com/schemaplexai/system/mapper/SfTenantMapper.java` | MyBatis-Plus mapper |

## Error Handling

- `getValidTenant` throws `BaseException` with `ResultCode.TENANT_NOT_FOUND` if tenant does not exist
- `getValidTenant` throws `BaseException` with `ResultCode.TENANT_DISABLED` if tenant status is 0 (disabled)

## Dependencies / Collaborators

- **MyBatis-Plus** — `ServiceImpl` provides full CRUD, pagination, and query helpers
- **SfTenant entity** — stores tenants in `sf_tenant` table (global table, excluded from tenant interceptor)
- **TenantContextHolder** — thread-local tenant context used by `TenantLineInterceptor`

## Backlinks

- [[services/user-service]] — users belong to tenants
- [[services/config-service]] — configs are scoped by tenant
- [[services/permission-service]] — permissions are scoped by tenant
- [[services/role-service]] — roles are scoped by tenant
- [[entities/tenant]] — tenant entity
