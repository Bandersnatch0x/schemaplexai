---
title: PermissionController
type: controller
source: schemaplexai-system/src/main/java/com/schemaplexai/system/controller/PermissionController.java
creation_date: 2026-05-01
tags: [system, permission, rbac, controller, crud]
confidence: high
---

# PermissionController

> CRUD controller for permission definitions (menu, button, API).

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/system/permissions` | Paginated list of permissions |
| GET | `/system/permissions/{id}` | Get permission details by ID |
| POST | `/system/permissions` | Create a new permission |
| PUT | `/system/permissions/{id}` | Update a permission by ID |
| DELETE | `/system/permissions/{id}` | Delete a permission by ID |

## DTO / Entity

- **Request/Response**: `SfPermission` entity
  - `tenantId` (String): Owning tenant
  - `name` (String): Permission name
  - `code` (String): Permission code (e.g., `user:create`)
  - `type` (String): Permission type — MENU / BUTTON / API
  - Inherits `BaseEntity`

## Service Dependencies

- `PermissionService` — MyBatis-Plus `IService` for `SfPermission`

## Swagger Tags

- `@Tag(name = "权限管理")`

## Notes

- Permissions form a hierarchical tree (future: `parent_id` support in schema).
- `type` distinguishes UI navigation (MENU), UI actions (BUTTON), and backend endpoints (API).
