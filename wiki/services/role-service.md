---
title: RoleService
type: service
source: schemaplexai-system/src/main/java/com/schemaplexai/system/service/RoleService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, system, auth, role, rbac]
confidence: high
---

# RoleService

> One-sentence summary: Provides CRUD operations for role definitions, supporting tenant-scoped role-based access control (RBAC).

## Responsibilities

1. Manage role records (create, read, update, delete)
2. Support tenant-scoped role isolation
3. Provide pagination and query helpers via MyBatis-Plus `ServiceImpl`

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| *(inherited)* | All CRUD operations inherited from `ServiceImpl<SfRoleMapper, SfRole>` | — | — |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `RoleService` | `schemaplexai-system/src/main/java/com/schemaplexai/system/service/RoleService.java` | Service class extending `ServiceImpl<SfRoleMapper, SfRole>` |
| `SfRole` | `schemaplexai-system/src/main/java/com/schemaplexai/system/entity/SfRole.java` | Entity: `tenantId`, `name`, `code` |
| `SfRoleMapper` | `schemaplexai-system/src/main/java/com/schemaplexai/system/mapper/SfRoleMapper.java` | MyBatis-Plus mapper |

## Dependencies / Collaborators

- **MyBatis-Plus** — `ServiceImpl` provides full CRUD, pagination, and query helpers
- **SfRole entity** — stores roles in `sf_role` table
- **PermissionService** — permissions are assigned to roles
- **UserService** — users are assigned roles

## Backlinks

- [[services/permission-service]] — permissions assigned to roles
- [[services/user-service]] — users assigned to roles
- [[services/tenant-service]] — tenant context for role scoping
- [[entities/role]] — role entity
