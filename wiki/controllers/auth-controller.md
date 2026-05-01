---
title: AuthController
type: controller
source: schemaplexai-system/src/main/java/com/schemaplexai/system/controller/AuthController.java
creation_date: 2026-05-01
update_date: 2026-05-01
tags: [controller, auth, login, jwt, security]
confidence: high
---

# AuthController

> One-sentence summary: Authentication REST API — login with tenant-scoped credentials, refresh tokens, and logout with session cleanup.

## Base Path

`/auth` (routed via Gateway to `schemaplexai-system` port 8081)

## Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| POST | `/login` | Login with username/password, returns access + refresh tokens | No |
| POST | `/refresh` | Exchange refresh token for new token pair | No |
| POST | `/logout` | Logout user, clear Redis session | No (reads X-User-Id header) |

## Request/Response

### POST /login
```json
// Request
{
  "username": "admin",
  "password": "password"
}
// Headers: X-Tenant-Id: tenant_123

// Response
{
  "code": 200,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "tokenType": "Bearer"
  }
}
```

### POST /refresh
```json
// Request
{
  "refreshToken": "eyJ..."
}

// Response
{
  "code": 200,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "tokenType": "Bearer"
  }
}
```

### POST /logout
```json
// Headers: X-User-Id: 123
// Response
{
  "code": 200,
  "data": null
}
```

## Dependencies

- `AuthService authService` — Business logic for login/refresh/logout

## Security

- JWT validation happens at Gateway (`JwtAuthFilter`) before reaching this controller
- Passwords verified with BCrypt
- Tokens stored in Redis with TTL matching expiration
- Blacklist check on refresh token

## Backlinks

- Auth service: [[services/auth-service]]
- Gateway filter: [[services/jwt-auth-filter]]
- System controllers: [[controllers/system-controllers]]
- RBAC entities: [[entities/user]]
