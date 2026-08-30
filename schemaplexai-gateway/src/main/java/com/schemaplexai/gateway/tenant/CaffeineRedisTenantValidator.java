package com.schemaplexai.gateway.tenant;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.schemaplexai.common.redis.TenantRedisKeyResolver;
import com.schemaplexai.gateway.config.TenantValidationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Tenant existence/status validator backed by a two-level cache (issue 913,
 * spec §4.3 "验证租户存在性（缓存查询）").
 *
 * <p><b>Channel selection.</b> The gateway has no database access (by design it
 * does not depend on the dao module), so validation uses the same facility as
 * rate limiting — Redis — as the shared L2 source of truth, with a Caffeine L1
 * in front of it to keep the per-request cost at a local map lookup:
 * <pre>
 *   request → L1 Caffeine (TTL-bound) → L2 Redis GET sf:global:cache:tenant:{code}
 * </pre>
 * The tenant-owning services (system/admin) write {@code ACTIVE}/{@code DISABLED}
 * to that key on every mutation and backfill it at startup. A missing key means
 * the tenant is unknown → {@link TenantStatus#NOT_FOUND}.
 *
 * <p><b>Failure mode.</b> Redis errors propagate to the caller
 * ({@code TenantResolveFilter}), which denies the request (fail-closed,
 * consistent with the rate limiter's spec §4.1 stance). NOT_FOUND results are
 * cached too, so floods with forged tenant ids do not hammer Redis.
 */
@Slf4j
@Component
public class CaffeineRedisTenantValidator implements ReactiveTenantValidator {

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private final Cache<String, TenantStatus> localCache;

    public CaffeineRedisTenantValidator(ReactiveStringRedisTemplate reactiveStringRedisTemplate,
                                        TenantValidationProperties properties) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(properties.getCacheMaxSize())
                .expireAfterWrite(properties.getCacheTtl())
                .build();
    }

    @Override
    public Mono<TenantStatus> validate(String tenantId) {
        TenantStatus cached = localCache.getIfPresent(tenantId);
        if (cached != null) {
            return Mono.just(cached);
        }
        return reactiveStringRedisTemplate.opsForValue()
                .get(TenantRedisKeyResolver.tenantStatus(tenantId))
                .map(CaffeineRedisTenantValidator::parseStatus)
                .defaultIfEmpty(TenantStatus.NOT_FOUND)
                .doOnNext(status -> localCache.put(tenantId, status));
    }

    private static TenantStatus parseStatus(String value) {
        if (TenantRedisKeyResolver.TENANT_STATUS_DISABLED.equalsIgnoreCase(value)) {
            return TenantStatus.DISABLED;
        }
        if (TenantRedisKeyResolver.TENANT_STATUS_ACTIVE.equalsIgnoreCase(value)) {
            return TenantStatus.ACTIVE;
        }
        log.warn("Unexpected tenant status value '{}' in cache — treating as NOT_FOUND", value);
        return TenantStatus.NOT_FOUND;
    }
}
