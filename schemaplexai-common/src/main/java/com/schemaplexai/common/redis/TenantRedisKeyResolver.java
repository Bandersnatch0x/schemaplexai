package com.schemaplexai.common.redis;

/**
 * Centralized Redis key resolver that enforces tenant-scoped key prefixes.
 * <p>
 * All tenant-scoped keys follow the pattern: {@code sf:{tenantId}:<category>:<detail>}
 * All global keys follow the pattern: {@code sf:global:<category>:<detail>}
 * <p>
 * This prevents cross-tenant rate limit bypass and cache poisoning by ensuring
 * every Redis operation uses a tenant-namespaced key.
 */
public final class TenantRedisKeyResolver {

    private static final String PREFIX = "sf";
    private static final String GLOBAL = "global";

    private TenantRedisKeyResolver() {}

    // -----------------------------------------------------------------------
    // Category constants
    // -----------------------------------------------------------------------

    public static final String CAT_CACHE = "cache";
    public static final String CAT_RATELIMIT = "ratelimit";
    public static final String CAT_ADMISSION = "admission";
    public static final String CAT_IDEMPOTENCY = "idempotency";
    public static final String CAT_EXECUTION = "execution";
    public static final String CAT_SUBAGENT = "subagent";
    public static final String CAT_MEMORY = "memory";
    public static final String CAT_FILES = "files";
    public static final String CAT_TOKEN = "token";
    public static final String CAT_MODEL = "model";
    public static final String CAT_HEALTH = "health";

    // -----------------------------------------------------------------------
    // Core resolution
    // -----------------------------------------------------------------------

    /**
     * Build a tenant-scoped key: {@code sf:{tenantId}:{category}:{detail}}
     *
     * @param tenantId the tenant identifier (must not be blank)
     * @param category the key category (e.g. "cache", "ratelimit")
     * @param detail   the specific key detail (may contain colons)
     * @return the fully qualified Redis key
     * @throws IllegalArgumentException if tenantId or category is blank
     */
    public static String tenantKey(String tenantId, String category, String detail) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category must not be blank");
        }
        return PREFIX + ":" + tenantId + ":" + category + ":" + detail;
    }

    /**
     * Build a tenant-scoped key with sub-category: {@code sf:{tenantId}:{category}:{sub}:{detail}}
     */
    public static String tenantKey(String tenantId, String category, String sub, String detail) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category must not be blank");
        }
        return PREFIX + ":" + tenantId + ":" + category + ":" + sub + ":" + detail;
    }

    /**
     * Build a global (non-tenant-scoped) key: {@code sf:global:{category}:{detail}}
     *
     * @param category the key category
     * @param detail   the specific key detail
     * @return the fully qualified Redis key
     */
    public static String globalKey(String category, String detail) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category must not be blank");
        }
        return PREFIX + ":" + GLOBAL + ":" + category + ":" + detail;
    }

    /**
     * Build a global key with sub-category: {@code sf:global:{category}:{sub}:{detail}}
     */
    public static String globalKey(String category, String sub, String detail) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category must not be blank");
        }
        return PREFIX + ":" + GLOBAL + ":" + category + ":" + sub + ":" + detail;
    }

    // -----------------------------------------------------------------------
    // Convenience methods for well-known key patterns
    // -----------------------------------------------------------------------

    /** Cache key for chat memory: sf:{tenantId}:cache:chat:{conversationId} */
    public static String chatMemory(String tenantId, String conversationId) {
        return tenantKey(tenantId, CAT_CACHE, "chat", conversationId);
    }

    /** Cache key for chat memory backfill lock: sf:{tenantId}:cache:chat:{conversationId}:backfill_lock */
    public static String chatMemoryBackfillLock(String tenantId, String conversationId) {
        return tenantKey(tenantId, CAT_CACHE, "chat:" + conversationId + ":backfill_lock");
    }

    /** Cache key for conversation file tracking: sf:{tenantId}:cache:files:{conversationId} */
    public static String conversationFiles(String tenantId, String conversationId) {
        return tenantKey(tenantId, CAT_CACHE, "files", conversationId);
    }

    /** Rate limit key: sf:{tenantId}:ratelimit:{scope}:{windowKey} */
    public static String rateLimit(String tenantId, String scope, String windowKey) {
        return tenantKey(tenantId, CAT_RATELIMIT, scope, windowKey);
    }

    /** Gateway rate limit key (IP-based fallback): sf:global:ratelimit:ip:{ip}:{windowKey} */
    public static String rateLimitGlobal(String scope, String identifier, String windowKey) {
        return globalKey(CAT_RATELIMIT, scope, identifier + ":" + windowKey);
    }

    /** Admission rate limit: sf:{tenantId}:admission:rate:{agentId} */
    public static String admissionRate(String tenantId, String agentId) {
        return tenantKey(tenantId, CAT_ADMISSION, "rate", agentId);
    }

    /** Admission concurrency: sf:{tenantId}:admission:concurrency:{agentId} */
    public static String admissionConcurrency(String tenantId, String agentId) {
        return tenantKey(tenantId, CAT_ADMISSION, "concurrency", agentId);
    }

    /** Admission cost budget: sf:{tenantId}:admission:cost */
    public static String admissionCost(String tenantId) {
        return tenantKey(tenantId, CAT_ADMISSION, "cost");
    }

    /** Sub-agent quota by parent execution: sf:{tenantId}:subagent:count:{parentExecutionId} */
    public static String subagentParentCount(String tenantId, String parentExecutionId) {
        return tenantKey(tenantId, CAT_SUBAGENT, "count", parentExecutionId);
    }

    /** Sub-agent quota by tenant: sf:{tenantId}:subagent:tenant */
    public static String subagentTenantCount(String tenantId) {
        return tenantKey(tenantId, CAT_SUBAGENT, "tenant");
    }

    /** Execution paused state: sf:{tenantId}:execution:paused:{executionId} */
    public static String executionPaused(String tenantId, String executionId) {
        return tenantKey(tenantId, CAT_EXECUTION, "paused", executionId);
    }

    /** MQ idempotency key: sf:global:idempotency:{consumerGroup}:{messageId} */
    public static String idempotency(String consumerGroup, String messageId) {
        return globalKey(CAT_IDEMPOTENCY, consumerGroup, messageId);
    }

    /** Agent execution idempotency: sf:global:idempotency:agent:execute:{detail} */
    public static String idempotencyAgentExecute(String detail) {
        return globalKey(CAT_IDEMPOTENCY, "agent:execute", detail);
    }

    /** Cost sync idempotency: sf:global:idempotency:cost:sync:{detail} */
    public static String idempotencyCostSync(String detail) {
        return globalKey(CAT_IDEMPOTENCY, "cost:sync", detail);
    }

    /** Model cooldown (global): sf:global:model:cooldown:{providerName} */
    public static String modelCooldown(String providerName) {
        return globalKey(CAT_MODEL, "cooldown", providerName);
    }

    /** Token blacklist (global): sf:global:token:blacklist:{jti} */
    public static String tokenBlacklist(String jti) {
        return globalKey(CAT_TOKEN, "blacklist", jti);
    }

    /** Auth session token: sf:global:token:session:{userId} */
    public static String tokenSession(String userId) {
        return globalKey(CAT_TOKEN, "session", userId);
    }

    /** Health check key: sf:global:health:check */
    public static String healthCheck() {
        return globalKey(CAT_HEALTH, "check");
    }
}
