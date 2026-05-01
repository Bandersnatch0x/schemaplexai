---
title: TenantController
type: controller
source: schemaplexai-system/src/main/java/com/schemaplexai/system/controller/TenantController.java
creation_date: 2026-05-01
tags: [system, tenant, rbac, controller, crud]
confidence: high
---

# TenantController

> CRUD controller for tenant management (multi-tenant RBAC).

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/system/tenants` | Paginated list of tenants |
| GET | `/system/tenants/{id}` | Get tenant details by ID |
| POST | `/system/tenants` | Create a new tenant |
| PUT | `/system/tenants/{id}` | Update a tenant by ID |
| DELETE | `/system/tenants/{id}` | Delete a tenant by ID |

## DTO / Entity

- **Request/Response**: `SfTenant` entity
  - `name` (String): Tenant name
  - `code` (String): Unique tenant code
  - `status` (Integer): Tenant status
  - `configJson` (String): Tenant-specific configuration JSON
  - Inherits `BaseEntity`

## Service Dependencies

- `TenantService` — MyBatis-Plus `IService` for `SfTenant`

## Swagger Tags

- `@Tag(name = "租户管理")`

## Notes

- Tenants are the top-level isolation boundary. All other entities are scoped to a tenant.
- The `sf_tenant` table is a global table (excluded from `TenantLineInterceptor`).
