package com.schemaplexai.spec.security;

import com.schemaplexai.common.context.TenantContextHolder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Establishes the Spring Security {@link Authentication} for requests that the
 * API gateway already authenticated.
 * <p>
 * The gateway re-forwards the original signed token
 * ({@code Authorization: Bearer ...}) after validating it; this filter
 * re-validates the signature locally (defence in depth — the service must not
 * trust identity headers it could receive without passing the gateway) and
 * lifts the identity claims into the SecurityContext:
 * <ul>
 *   <li>principal  = userId (JWT subject)</li>
 *   <li>authorities = role codes bridged via {@link SpecRoleAuthorityMapper},
 *       taken from an optional {@code roles} claim when present or resolved
 *       from the RBAC tables otherwise</li>
 *   <li>tenantId claim is also published to {@link TenantContextHolder} so the
 *       tenant-line SQL interceptor filters every query of this request</li>
 * </ul>
 * Requests without a valid token stay anonymous; the filter chain then rejects
 * them at the authorization stage (401) rather than here.
 */
@Slf4j
public class GatewayAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey key;
    private final SpecRoleProvider roleProvider;

    public GatewayAuthFilter(String jwtSecret, SpecRoleProvider roleProvider) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.roleProvider = roleProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            buildAuthentication(request).ifPresent(authentication -> {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                Object tenantId = ((Map<?, ?>) authentication.getDetails()).get("tenantId");
                if (tenantId instanceof String tenant && !tenant.isBlank()) {
                    TenantContextHolder.setTenantId(tenant);
                }
            });
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            TenantContextHolder.clear();
        }
    }

    /**
     * Visible for testing: validates the forwarded bearer token and builds the
     * authentication, or returns empty for a missing / invalid token.
     */
    Optional<Authentication> buildAuthentication(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(header.substring(BEARER_PREFIX.length()))
                    .getPayload();
        } catch (Exception e) {
            log.debug("Rejecting forwarded JWT: {}", e.getMessage());
            return Optional.empty();
        }

        String userId = claims.getSubject();
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        String tenantId = claims.get("tenantId", String.class);
        String username = claims.get("username", String.class);

        // Tenant context must exist before the RBAC lookup so the
        // tenant-line interceptor scopes the role queries.
        if (tenantId != null && !tenantId.isBlank()) {
            TenantContextHolder.setTenantId(tenantId);
        }

        Set<String> authorities = resolveAuthorities(claims, userId, tenantId);
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        userId,
                        null,
                        authorities.stream().map(SimpleGrantedAuthority::new).toList());
        Map<String, String> details = new HashMap<>();
        details.put("tenantId", tenantId == null ? "" : tenantId);
        details.put("username", username == null ? "" : username);
        authentication.setDetails(details);
        return Optional.of(authentication);
    }

    @SuppressWarnings("unchecked")
    private Set<String> resolveAuthorities(Claims claims, String userId, String tenantId) {
        // Forward-compatible: a token that already carries role codes wins
        // over the DB lookup.
        List<String> roleCodes = null;
        try {
            roleCodes = claims.get("roles", List.class);
        } catch (Exception e) {
            log.debug("Ignoring malformed 'roles' claim: {}", e.getMessage());
        }
        if (roleCodes != null && !roleCodes.isEmpty()) {
            return new LinkedHashSet<>(SpecRoleAuthorityMapper.toAuthorities(roleCodes));
        }
        return roleProvider.authoritiesFor(userId, tenantId);
    }
}
