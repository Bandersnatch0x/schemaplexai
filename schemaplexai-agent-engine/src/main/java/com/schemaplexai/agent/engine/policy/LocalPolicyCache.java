package com.schemaplexai.agent.engine.policy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Caffeine-backed local cache for tenant approval policies.
 *
 * <p>Populated via MQ event {@code tenant.policy.updated}.
 * Cache miss triggers synchronous fallback to Core via HTTP (2s timeout).
 */
@Slf4j
@Component
public class LocalPolicyCache {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int MAX_ENTRIES = 10_000;

    /**
     * Cache key format: "{tenantId}:{policyType}"
     * Value: JSON-serialized policy string.
     */
    private final Cache<String, String> cache;

    public LocalPolicyCache() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(MAX_ENTRIES)
                .expireAfterWrite(TTL)
                .recordStats()
                .build();
    }

    /**
     * Retrieves a cached policy for the given tenant and policy type.
     *
     * @param tenantId   the tenant ID
     * @param policyType the policy type (e.g. "approval", "tool_whitelist")
     * @return cached policy value, or null if not present
     */
    public String get(Long tenantId, String policyType) {
        String key = buildKey(tenantId, policyType);
        return cache.getIfPresent(key);
    }

    /**
     * Caches a policy entry.
     *
     * @param tenantId   the tenant ID
     * @param policyType the policy type
     * @param value      the policy value (JSON string)
     */
    public void put(Long tenantId, String policyType, String value) {
        String key = buildKey(tenantId, policyType);
        cache.put(key, value);
        log.debug("Cached policy for tenant={} type={}", tenantId, policyType);
    }

    /**
     * Invalidates a specific policy entry (e.g. on {@code tenant.policy.updated} event).
     *
     * @param tenantId   the tenant ID
     * @param policyType the policy type to invalidate
     */
    public void invalidate(Long tenantId, String policyType) {
        String key = buildKey(tenantId, policyType);
        cache.invalidate(key);
        log.info("Invalidated cached policy for tenant={} type={}", tenantId, policyType);
    }

    /**
     * Invalidates all policies for a specific tenant.
     *
     * @param tenantId the tenant ID
     */
    public void invalidateTenant(Long tenantId) {
        // Caffeine doesn't support prefix invalidation natively;
        // iterate and remove matching keys.
        cache.asMap().keySet().removeIf(key -> key.startsWith(tenantId + ":"));
        log.info("Invalidated all cached policies for tenant={}", tenantId);
    }

    /**
     * Clears the entire cache (useful for testing or full reload).
     */
    public void invalidateAll() {
        cache.invalidateAll();
        log.info("Cleared entire policy cache");
    }

    /**
     * Returns cache statistics for observability.
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats stats() {
        return cache.stats();
    }

    private String buildKey(Long tenantId, String policyType) {
        return tenantId + ":" + policyType;
    }
}
