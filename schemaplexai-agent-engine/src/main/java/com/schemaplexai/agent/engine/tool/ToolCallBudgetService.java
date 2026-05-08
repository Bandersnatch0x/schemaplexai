package com.schemaplexai.agent.engine.tool;

import com.schemaplexai.common.redis.TenantRedisKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Per-tenant per-day tool-call budget enforcement via Redis.
 *
 * <p>Prevents agents from hammering tools in a loop across executions by tracking
 * aggregate tool calls per tenant within a sliding window. This closes the exploit
 * where a single tenant could exhaust system resources by spawning many executions,
 * each staying within per-execution limits but collectively overwhelming the system.
 *
 * <p>Key pattern: {@code sf:{tenantId}:toolcall:count:{windowKey}}
 * Uses {@link TenantRedisKeyResolver} for tenant isolation.
 *
 * <p>Budget is enforced at two levels:
 * <ol>
 *   <li><b>Per-execution</b>: {@link com.schemaplexai.agent.engine.admission.TokenBudget#consumeToolCall()}</li>
 *   <li><b>Per-tenant aggregate</b>: This service (Redis-backed daily counter)</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolCallBudgetService {

    private static final String CATEGORY = "toolcall";

    private final StringRedisTemplate redisTemplate;

    @Value("${agent.engine.tool-call.daily-limit:500}")
    private long dailyLimit = 500;

    /**
     * Check if a tenant has remaining tool-call budget for today.
     *
     * @param tenantId the tenant ID
     * @return true if the tenant can make more tool calls
     */
    public boolean hasRemainingBudget(String tenantId) {
        long current = getCurrentCount(tenantId);
        return current < dailyLimit;
    }

    /**
     * Consume one tool-call from the tenant's daily budget.
     *
     * <p>Atomically increments the counter and sets a TTL if this is the first call.
     *
     * @param tenantId the tenant ID
     * @return true if the call was within budget (counter incremented), false if budget exceeded
     */
    public boolean consume(String tenantId) {
        String key = resolveKey(tenantId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // Set TTL to end of day (approximately 24 hours)
            redisTemplate.expire(key, Duration.ofHours(24));
        }
        if (count != null && count > dailyLimit) {
            log.warn("Tenant {} exceeded daily tool-call budget: {}/{}", tenantId, count, dailyLimit);
            return false;
        }
        return true;
    }

    /**
     * Get the current tool-call count for a tenant today.
     *
     * @param tenantId the tenant ID
     * @return the current count, or 0 if no calls recorded
     */
    public long getCurrentCount(String tenantId) {
        String key = resolveKey(tenantId);
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0 : Long.parseLong(value);
    }

    /**
     * Get the remaining tool-call budget for a tenant.
     *
     * @param tenantId the tenant ID
     * @return remaining calls (never negative)
     */
    public long getRemaining(String tenantId) {
        long current = getCurrentCount(tenantId);
        long remaining = dailyLimit - current;
        return Math.max(remaining, 0);
    }

    /**
     * Get the configured daily limit.
     */
    public long getDailyLimit() {
        return dailyLimit;
    }

    private String resolveKey(String tenantId) {
        String windowKey = java.time.LocalDate.now().toString();
        return TenantRedisKeyResolver.tenantKey(tenantId, CATEGORY, "count", windowKey);
    }
}
