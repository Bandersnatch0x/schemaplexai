---
title: RoleController
type: controller
source: schemaplexai-system/src/main/java/com/schemaplexai/system/controller/RoleController.java
creation_date: 2026-05-01
tags: [system, role, rbac, controller, crud]
confidence: high
---

# RoleController

> CRUD controller for role definitions within tenants.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/system/roles` | Paginated list of roles |
| GET | `/system/roles/{id}` | Get role details by ID |
| POST | `/system/roles` | Create a new role |
| PUT | `/system/roles/{id}` | Update a role by ID |
| DELETE | `/system/roles/{id}` | Delete a role by ID |

## DTO / Entity

- **Request/Response**: `SfRole` entity
  - `tenantId` (String): Owning tenant
  - `name` (String): Role name
  - `code` (String): Unique role code
  - Inherits `BaseEntity`

## Service Dependencies

- `RoleService` — MyBatis-Plus `IService` for `SfRole`

## Swagger Tags

- `@Tag(name = "角色管理")`

## Notes

- Roles are tenant-scoped. The `code` field must be unique per tenant.
- Role-to-permission mapping is managed via `sf_role_permission` join table.
