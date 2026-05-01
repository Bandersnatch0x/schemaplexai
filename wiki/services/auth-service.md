---
title: AuthService
type: service
source: schemaplexai-system/src/main/java/com/schemaplexai/system/service/AuthService.java
creation_date: 2026-05-01
update_date: 2026-05-01
tags: [service, auth, jwt, login, security]
confidence: high
---

# AuthService

> One-sentence summary: Authentication business logic — login with BCrypt verification, JWT token generation/refresh, and Redis-backed token blacklist for logout.

## Responsibilities

1. **Login** — Verify username/password with BCrypt, issue access + refresh tokens
2. **Token refresh** — Validate refresh token, issue new token pair
3. **Logout** — Delete Redis session key and blacklist the token
4. **Token blacklist** — Track revoked tokens by JTI with TTL-based expiration

## Key Code

### Login
```java
public Map<String, String> login(String username, String password, String tenantId) {
    SfUser user = userService.getByUsernameAndTenantId(username, tenantId);
    if (!passwordEncoder.matches(password, user.getPassword())) {
        throw new BaseException(ResultCode.PASSWORD_ERROR);
    }

    String accessToken = generateToken(user.getId().toString(), tenantId, jwtExpiration);      // 24h
    String refreshToken = generateToken(user.getId().toString(), tenantId, jwtRefreshExpiration); // 7d

    // Store in Redis
    stringRedisTemplate.opsForValue().set(
        REDIS_KEY_CHAT_MEMORY + ":" + user.getId(), accessToken,
        Duration.ofMillis(jwtExpiration));

    return Map.of("accessToken", accessToken, "refreshToken", refreshToken,
                  "tokenType", "Bearer");
}
```

### Token Generation
```java
private String generateToken(String userId, String tenantId, long expiration) {
    SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(UTF_8));
    return Jwts.builder()
        .id(UUID.randomUUID().toString())      // JTI for blacklist
        .subject(userId)
        .claim("tenantId", tenantId)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expiration))
        .signWith(key)
        .compact();
}
```

### Logout & Blacklist
```java
public void logout(String userId, String token) {
    if (StringUtils.hasText(userId)) {
        stringRedisTemplate.delete(REDIS_KEY_CHAT_MEMORY + ":" + userId);
    }
    if (StringUtils.hasText(token)) {
        blacklistToken(token);
    }
}

public void blacklistToken(String token) {
    String jti = jwtTokenProvider.getJti(token);
    Date expiration = jwtTokenProvider.getExpirationDate(token);
    long ttl = expiration.getTime() - System.currentTimeMillis();
    if (ttl > 0) {
        stringRedisTemplate.opsForValue().set(
            "sf:token:blacklist:" + jti, "1", Duration.ofMillis(ttl));
    }
}
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `jwt.secret` | — | HMAC key (must be >= 32 bytes) |
| `jwt.expiration` | 86400000 (24h) | Access token TTL |
| `jwt.refresh-expiration` | 604800000 (7d) | Refresh token TTL |

## Redis Keys

| Key Pattern | Purpose |
|-------------|---------|
| `sf:memory:chat:{userId}` | Active access token storage |
| `sf:token:blacklist:{jti}` | Revoked token tracking |

## Dependencies

| Component | Role |
|-----------|------|
| `UserService` | Load user by username + tenant |
| `JwtTokenProvider` | Extract JTI and expiration from token |
| `StringRedisTemplate` | Token storage and blacklist |
| `BCryptPasswordEncoder` | Password hashing verification |

## Security Notes

- **BCrypt for passwords** — Never store plain text
- **JTI-based blacklist** — Each token has unique ID; blacklist TTL matches token expiry
- **Secret length guard** — `@PostConstruct` fails fast if secret < 32 bytes
- **Tenant-scoped login** — `getByUsernameAndTenantId` enforces tenant isolation

## Backlinks

- Controller: [[controllers/auth-controller]]
- Gateway filter: [[services/jwt-auth-filter]]
- RBAC entities: [[entities/user]]
