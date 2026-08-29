package com.schemaplexai.spec.security;

import com.schemaplexai.spec.mapper.SpecAuthMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the authority set of a user from the RBAC tables, with a short
 * in-memory TTL cache so a request burst costs one lookup per user.
 * <p>
 * Failure policy: a lookup error degrades to the last-known authorities (or
 * none on first use) and logs a warning — an outage of the role lookup must
 * not take down authentication itself. Privileged operations simply become
 * denied until the lookup recovers.
 */
@Slf4j
@Component
public class SpecRoleProvider {

    static final long CACHE_TTL_MS = 60_000L;

    private final SpecAuthMapper authMapper;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public SpecRoleProvider(SpecAuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    private record CacheEntry(long expiresAt, Set<String> roleCodes, Set<String> permissionCodes) {
    }

    /**
     * Authorities for the user: role codes bridged through
     * {@link SpecRoleAuthorityMapper} plus permission codes granted via
     * sf_role_permission passed through verbatim.
     */
    public Set<String> authoritiesFor(String userId, String tenantId) {
        CacheEntry entry = load(userId, tenantId);
        Set<String> authorities = new LinkedHashSet<>(SpecRoleAuthorityMapper.toAuthorities(entry.roleCodes()));
        for (String permissionCode : entry.permissionCodes()) {
            if (permissionCode != null && !permissionCode.isBlank()) {
                authorities.add(permissionCode.trim());
            }
        }
        return authorities;
    }

    private CacheEntry load(String userId, String tenantId) {
        Long uid = parseUserId(userId);
        if (uid == null) {
            return new CacheEntry(0L, Set.of(), Set.of());
        }
        String key = (tenantId == null ? "" : tenantId) + ":" + uid;
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expiresAt() > System.currentTimeMillis()) {
            return cached;
        }
        try {
            List<String> roles = authMapper.selectRoleCodes(uid);
            List<String> permissions = authMapper.selectPermissionCodes(uid);
            CacheEntry fresh = new CacheEntry(
                    System.currentTimeMillis() + CACHE_TTL_MS,
                    roles == null ? Set.of() : Set.copyOf(roles),
                    permissions == null ? Set.of() : Set.copyOf(permissions));
            cache.put(key, fresh);
            return fresh;
        } catch (Exception e) {
            log.warn("Role lookup failed for user {} (tenant {}): {}", userId, tenantId, e.getMessage());
            return cached != null ? cached : new CacheEntry(0L, Set.of(), Set.of());
        }
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(userId.trim());
        } catch (NumberFormatException e) {
            log.debug("Non-numeric userId '{}'; skipping RBAC lookup", userId);
            return null;
        }
    }
}
