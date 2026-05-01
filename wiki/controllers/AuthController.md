---
title: AuthController
type: controller
source: schemaplexai-system/src/main/java/com/schemaplexai/system/controller/AuthController.java
creation_date: 2026-05-01
tags: [controller, auth, system]
confidence: high
---

# AuthController

> Handles user authentication: login, token refresh, and logout.

## Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /auth/login | User login with username/password | Not required |
| POST | /auth/refresh | Refresh access token using refresh token | Not required |
| POST | /auth/logout | User logout | Required |

## Request/Response Types

### Login

**Request:** `Map<String, String>`
- `username` — User login name
- `password` — User password
- `X-Tenant-Id` header — Tenant identifier (read from request headers)

**Response:** `Result<Map<String, String>>`
- `accessToken` — JWT access token
- `refreshToken` — JWT refresh token
- `tokenType` — Token prefix (e.g., "Bearer")

### Refresh Token

**Request:** `Map<String, String>`
- `refreshToken` — Valid refresh token

**Response:** `Result<Map<String, String>>`
- `accessToken` — New JWT access token
- `refreshToken` — New JWT refresh token
- `tokenType` — Token prefix

### Logout

**Request:** Headers
- `X-User-Id` — User ID to logout

**Response:** `Result<Void>`

## Service Dependencies

| Service | Role |
|---------|------|
| `AuthService` | Validates credentials, generates JWTs, manages Redis sessions |

## Implementation Notes

- JWT secret is validated at startup (must be >= 32 bytes).
- Access tokens are stored in Redis with TTL matching expiration.
- Logout clears the Redis session key for the user.
- Token blacklist is supported via `sf:token:blacklist:{jti}` Redis keys.
- Default token expiration: 24 hours; refresh token: 7 days.
