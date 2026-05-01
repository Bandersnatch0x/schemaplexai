---
title: UserController
type: controller
source: schemaplexai-system/src/main/java/com/schemaplexai/system/controller/UserController.java
creation_date: 2026-05-01
tags: [system, user, rbac, controller, crud]
confidence: high
---

# UserController

> CRUD controller for user management within tenants.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/system/users` | Paginated list of users |
| GET | `/system/users/{id}` | Get user details by ID |
| POST | `/system/users` | Create a new user |
| PUT | `/system/users/{id}` | Update a user by ID |
| DELETE | `/system/users/{id}` | Delete a user by ID |

## DTO / Entity

- **Request/Response**: `SfUser` entity
  - `tenantId` (String): Owning tenant
  - `username` (String): Login username
  - `password` (String): Hashed password
  - `email` (String): Email address
  - `phone` (String): Phone number
  - `status` (Integer): User status
  - Inherits `BaseEntity`

## Service Dependencies

- `UserService` — MyBatis-Plus `IService` for `SfUser`

## Swagger Tags

- `@Tag(name = "用户管理")`

## Notes

- User passwords should be hashed before storage (bcrypt or similar).
- User-to-role mapping is managed via `sf_user_role` join table.
