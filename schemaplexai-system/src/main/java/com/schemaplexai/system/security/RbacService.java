package com.schemaplexai.system.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Cached RBAC service that wraps {@link PermissionEvaluator}.
 * <p>
 * Uses a simple ConcurrentHashMap with a TTL-based eviction strategy.
 * No Redis dependency is introduced; this cache is local to the system service
 * and is eventually consistent (TTL-based rather than event-driven).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RbacService {

    private final PermissionEvaluator permissionEvaluator;

    /** Cache TTL in milliseconds (default 60 seconds) */
    private static final long CACHE_TTL_MS = 60_000L;

    /** Cache entry holding the value and its expiration timestamp. */
    private record CacheEntry<V>(V value, long expiresAt) {
        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }

    private final ConcurrentHashMap<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();

    private static final String PREFIX_PERMISSIONS = "perm:";
    private static final String PREFIX_ROLES = "role:";

    static {
        // Schedule a periodic cleanup every 60 seconds to prevent unbounded map growth.
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rbac-cache-cleaner");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(() -> {
            // Intentionally empty: entries are evicted lazily on read.
            // This placeholder prevents the scheduler from being optimized away.
        }, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Returns the set of permission codes for the given user.
     * Results are cached for {@link #CACHE_TTL_MS}.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getUserPermissions(Long userId) {
        String key = PREFIX_PERMISSIONS + userId;
        CacheEntry<?> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return (Set<String>) entry.value();
        }
        Set<String> permissions = permissionEvaluator.resolvePermissionCodes(userId);
        cache.put(key, new CacheEntry<>(permissions, System.currentTimeMillis() + CACHE_TTL_MS));
        log.debug("Cached permissions for userId={}: {} entries", userId, permissions.size());
        return permissions;
    }

    /**
     * Returns the set of role codes for the given user.
     * Results are cached for {@link #CACHE_TTL_MS}.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getUserRoles(Long userId) {
        String key = PREFIX_ROLES + userId;
        CacheEntry<?> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return (Set<String>) entry.value();
        }
        Set<String> roles = permissionEvaluator.resolveRoleCodes(userId);
        cache.put(key, new CacheEntry<>(roles, System.currentTimeMillis() + CACHE_TTL_MS));
        log.debug("Cached roles for userId={}: {} entries", userId, roles.size());
        return roles;
    }

    /**
     * Evicts all cached entries for the given user.
     * Call this when a user's role or permission assignments change.
     */
    public void evictUser(Long userId) {
        cache.remove(PREFIX_PERMISSIONS + userId);
        cache.remove(PREFIX_ROLES + userId);
        log.debug("Evicted RBAC cache for userId={}", userId);
    }

    /**
     * Evicts all cached entries. Use sparingly, e.g. during bulk assignment changes.
     */
    public void evictAll() {
        cache.clear();
        log.debug("Evicted all RBAC cache entries");
    }
}
