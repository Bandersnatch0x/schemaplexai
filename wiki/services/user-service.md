---
title: UserService
type: service
source: schemaplexai-system/src/main/java/com/schemaplexai/system/service/UserService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, system, user, auth, login, jwt]
confidence: high
---

# UserService

> One-sentence summary: Handles user authentication, registration, and paginated user queries with password encoding and JWT token generation for secure multi-tenant access.

## Responsibilities

1. Authenticate users with username/password and issue JWT tokens
2. Register new users with password hashing
3. Retrieve users by username with tenant scoping
4. Provide paginated user listing

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `getByUsernameAndTenantId` | Retrieve a user by username and tenant ID | `username` — login username; `tenantId` — tenant identifier | `SfUser` — matching user, or null |
| `login` | Authenticate user and generate JWT token | `request` — `LoginRequest` with username, password, optional tenantId | `LoginResponse` — token, expiry, username, tenantId |
| `register` | Register a new user with hashed password | `user` — `SfUser` with username, password, etc. | `Long` — the created user ID |
| `pageUsers` | Paginated list of all users | `pageParam` — pagination parameters (current, size) | `PageResult<SfUser>` — paginated user list |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `UserService` | `schemaplexai-system/src/main/java/com/schemaplexai/system/service/UserService.java` | Service class extending `ServiceImpl<SfUserMapper, SfUser>` |
| `SfUser` | `schemaplexai-system/src/main/java/com/schemaplexai/system/entity/SfUser.java` | Entity: `tenantId`, `username`, `password`, `email`, `phone`, `status` |
| `SfUserMapper` | `schemaplexai-system/src/main/java/com/schemaplexai/system/mapper/SfUserMapper.java` | MyBatis-Plus mapper with custom queries |
| `LoginRequest` | `schemaplexai-system/src/main/java/com/schemaplexai/system/user/dto/LoginRequest.java` | DTO for login credentials |
| `LoginResponse` | `schemaplexai-system/src/main/java/com/schemaplexai/system/user/dto/LoginResponse.java` | DTO for login response with JWT token |

## Error Handling

- `login` throws `BaseException` with `ResultCode.USER_NOT_FOUND` if username not found
- `login` throws `BaseException` with `ResultCode.FORBIDDEN` if user is disabled (status == 0)
- `login` throws `BaseException` with `ResultCode.PASSWORD_ERROR` if password mismatch
- `register` throws `BaseException` with `ResultCode.PARAM_ERROR` if username already exists

## Dependencies / Collaborators

- **MyBatis-Plus** — `ServiceImpl` provides CRUD, pagination, and query helpers
- **Spring Security** — `PasswordEncoder` for bcrypt password hashing
- **JwtTokenProvider** — generates JWT tokens with user ID, tenant ID, and username
- **SfUser entity** — stores users in `sf_user` table
- **TenantService** — provides tenant validation context

## Backlinks

- [[services/tenant-service]] — tenant validation and scoping
- [[services/role-service]] — role assignment for users
- [[services/permission-service]] — permission checks for authenticated users
- [[entities/user]] — user entity
