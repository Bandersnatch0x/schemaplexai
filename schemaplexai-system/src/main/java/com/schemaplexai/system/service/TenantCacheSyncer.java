package com.schemaplexai.system.service;

import com.schemaplexai.common.redis.TenantRedisKeyResolver;
import com.schemaplexai.system.entity.SfTenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Publishes tenant existence/status to the shared cache channel read by the
 * gateway's TenantResolveFilter (issue 913, spec §4.3 "验证租户存在性（缓存查询）").
 *
 * <p>Channel contract: key {@code sf:global:cache:tenant:{tenantCode}}
 * (see {@link TenantRedisKeyResolver#tenantStatus}) holds {@code ACTIVE} or
 * {@code DISABLED}. The gateway has no database access, so this channel is its
 * only tenant query path — a tenant absent from it is rejected as unknown.
 *
 * <p>Writers: tenant mutations in this service (and the admin module, which
 * scans this package) call {@link #sync}/{@link #evict}; {@link
 * TenantCacheBackfillRunner} republishes every tenant at startup so the cache
 * converges even after a flush.
 *
 * <p>Sync failures are logged and swallowed: the cache is best-effort on the
 * write side (fail-open), while the gateway reads fail-closed. The startup
 * backfill heals any missed write.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantCacheSyncer {

    private final StringRedisTemplate stringRedisTemplate;

    /** Publish the tenant's current status to the shared channel. */
    public void sync(SfTenant tenant) {
        if (tenant == null || !StringUtils.hasText(tenant.getCode())) {
            log.warn("Skipping tenant cache sync: tenant has no code");
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    TenantRedisKeyResolver.tenantStatus(tenant.getCode()),
                    statusOf(tenant));
        } catch (Exception e) {
            log.warn("Failed to sync tenant cache for '{}': {}", tenant.getCode(), e.getMessage());
        }
    }

    /** Remove the tenant from the shared channel (deleted tenants become unknown). */
    public void evict(String tenantCode) {
        if (!StringUtils.hasText(tenantCode)) {
            return;
        }
        try {
            stringRedisTemplate.delete(TenantRedisKeyResolver.tenantStatus(tenantCode));
        } catch (Exception e) {
            log.warn("Failed to evict tenant cache for '{}': {}", tenantCode, e.getMessage());
        }
    }

    /** Map the sf_tenant.status convention (DISABLED = disabled, ACTIVE/null/other = enabled). */
    public static String statusOf(SfTenant tenant) {
        boolean disabled = TenantRedisKeyResolver.TENANT_STATUS_DISABLED.equals(tenant.getStatus());
        return disabled
                ? TenantRedisKeyResolver.TENANT_STATUS_DISABLED
                : TenantRedisKeyResolver.TENANT_STATUS_ACTIVE;
    }
}
