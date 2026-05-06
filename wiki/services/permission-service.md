---
title: PermissionService
type: service
source: schemaplexai-system/src/main/java/com/schemaplexai/system/service/PermissionService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, system, auth, permission, rbac]
confidence: high
---

# PermissionService

> One-sentence summary: Provides CRUD operations for permission definitions, supporting role-based access control (RBAC) with tenant-scoped permission records.

## Responsibilities

1. Manage permission records (create, read, update, delete)
2. Support tenant-scoped permission isolation
3. Provide pagination and query helpers via MyBatis-Plus `ServiceImpl`

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| *(inherited)* | All CRUD operations inherited from `ServiceImpl<SfPermissionMapper, SfPermission>` | — | — |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `PermissionService` | `schemaplexai-system/src/main/java/com/schemaplexai/system/service/PermissionService.java` | Service class extending `ServiceImpl<SfPermissionMapper, SfPermission>` |
| `SfPermission` | `schemaplexai-system/src/main/java/com/schemaplexai/system/entity/SfPermission.java` | Entity: `tenantId`, `name`, `code`, `type` |
| `SfPermissionMapper` | `schemaplexai-system/src/main/java/com/schemaplexai/system/mapper/SfPermissionMapper.java` | MyBatis-Plus mapper |

## Dependencies / Collaborators

- **MyBatis-Plus** — `ServiceImpl` provides full CRUD, pagination, and query helpers
- **SfPermission entity** — stores permissions in `sf_permission` table
- **RoleService** — roles aggregate permissions

## Backlinks

- [[services/role-service]] — roles that aggregate permissions
- [[services/user-service]] — user authentication and authorization
- [[services/tenant-service]] — tenant context for permission scoping
- [[entities/permission]] — permission entity
