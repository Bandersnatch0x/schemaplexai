---
title: JwtAuthFilter
type: service
source: schemaplexai-gateway/src/main/java/com/schemaplexai/gateway/filter/JwtAuthFilter.java
creation_date: 2026-05-01
update_date: 2026-05-01
tags: [service, gateway, jwt, auth, filter, security]
confidence: high
---

# JwtAuthFilter

> One-sentence summary: Global Gateway filter that validates JWT tokens on every request, extracts user/tenant claims, and injects them as downstream headers.

## Responsibilities

1. **Whitelist bypass** — Skip auth for public paths (`/auth/**`, `/doc.html`, Swagger, etc.)
2. **Token extraction** — Parse `Authorization: Bearer <token>` header
3. **JWT validation** — Verify signature, expiry, and claims using `jjwt`
4. **Header injection** — Forward `X-User-Id` and `X-Tenant-Id` to downstream services
5. **Error handling** — Return structured 401/403 JSON responses

## Key Code

```java
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final List<String> whiteList = List.of(
        "/auth/**",
        "/system/tenants/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/webjars/**",
        "/doc.html"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isWhiteListed(path)) return chain.filter(exchange);

        String token = resolveToken(exchange.getRequest());
        if (!StringUtils.hasText(token)) {
            return unauthorized(response, ResultCode.UNAUTHORIZED.getMessage());
        }

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).getPayload();

        String userId = claims.getSubject();
        String tenantId = claims.get("tenantId", String.class);

        ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header(HEADER_AUTHORIZATION, TOKEN_PREFIX + token)
            .header("X-User-Id", userId)
            .header(HEADER_TENANT_ID, tenantId)
            .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() { return -100; }  // Early in filter chain
}
```

## Whitelist Patterns

| Pattern | Purpose |
|---------|---------|
| `/auth/**` | Login, refresh, logout endpoints |
| `/system/tenants/**` | Tenant registration (public) |
| `/v3/api-docs/**` | OpenAPI spec |
| `/swagger-ui/**` | Swagger UI |
| `/webjars/**` | Swagger static assets |
| `/doc.html` | Knife4j API docs |

## JWT Claims

| Claim | Source | Downstream Header |
|-------|--------|-------------------|
| `sub` (subject) | `claims.getSubject()` | `X-User-Id` |
| `tenantId` | `claims.get("tenantId")` | `X-Tenant-Id` |
| `jti` | Token ID | Used for blacklist check |

## Error Responses

| Condition | HTTP Status | Body |
|-----------|-------------|------|
| Missing token | 401 | `{"code":401,"message":"Unauthorized","timestamp":...}` |
| Expired token | 401 | `{"code":401,"message":"Token expired","timestamp":...}` |
| Invalid token | 401 | `{"code":401,"message":"Token invalid","timestamp":...}` |

## Security

- **Secret validation at startup** — `@PostConstruct` enforces >= 32 bytes
- **HMAC-SHA signature** — `Keys.hmacShaKeyFor()` with `jjwt`
- **Token blacklist** — Checked by `AuthService.isTokenBlacklisted()` (not in filter, downstream)
- **Order = -100** — Runs early, before routing filters

## Dependencies

| Component | Role |
|-----------|------|
| `jjwt` (io.jsonwebtoken) | JWT parsing and validation |
| `jwt.secret` (application.yml) | HMAC signing key |
| `CommonConstants` | Header names and token prefix |

## Backlinks

- Auth service: [[services/auth-service]]
- Controller: [[controllers/auth-controller]]
- Architecture: [[architecture]]
- Routes: [[routes]]
