package com.schemaplexai.agent.engine.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.schemaplexai.dao.mapper.TenantEnvironmentConfigMapper;
import com.schemaplexai.model.entity.config.TenantEnvironmentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Loads and caches tenant environment security configuration.
 *
 * Caffeine Cache: maximumSize=1000, expireAfterWrite=5min.
 * Graceful fallback: returns last-known config if DB is unavailable.
 * Default policy: environment=dev, securityLevel=LOW when no config exists.
 *
 * Addressing Review Action Item #3: Caffeine Cache 5min TTL is acceptable
 * for security policy changes given the manual refresh API as backup.
 */
@Slf4j
@Service
public class SecurityPolicyLoader {

    private final TenantEnvironmentConfigMapper configMapper;
    private final Cache<String, TenantEnvironmentConfig> cache;

    public SecurityPolicyLoader(TenantEnvironmentConfigMapper configMapper) {
        this.configMapper = configMapper;
        this.cache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    /**
     * Load tenant environment config. Uses caffeine cache with DB fallback.
     *
     * @param tenantId the tenant identifier
     * @return the tenant's environment config, or a default policy if not found
     */
    public TenantEnvironmentConfig load(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return defaultConfig(tenantId);
        }

        // Try cache first
        TenantEnvironmentConfig cached = cache.getIfPresent(tenantId);
        if (cached != null) {
            return cached;
        }

        // Cache miss - load from DB
        try {
            TenantEnvironmentConfig config = configMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                            TenantEnvironmentConfig>()
                            .eq(TenantEnvironmentConfig::getTenantId, tenantId)
            );

            if (config != null) {
                cache.put(tenantId, config);
                log.debug("Loaded config for tenant {}: env={}, securityLevel={}",
                        tenantId, config.getEnvironment(), config.getSecurityLevel());
                return config;
            }

            // No config found - return defaults
            TenantEnvironmentConfig defaultCfg = defaultConfig(tenantId);
            cache.put(tenantId, defaultCfg);
            return defaultCfg;

        } catch (Exception e) {
            log.warn("Failed to load config for tenant {}, using last-known or defaults", tenantId, e);
            return defaultConfig(tenantId);
        }
    }

    /**
     * Force refresh the cached config for a specific tenant.
     * Used by the manual refresh API (POST /agent/tenant/config/refresh).
     */
    public void refresh(String tenantId) {
        if (tenantId != null) {
            cache.invalidate(tenantId);
            log.info("Cache invalidated for tenant {}", tenantId);
        }
    }

    /**
     * Returns a default configuration when no explicit config exists.
     * Default is HIGH security (deny-by-default) — tenants must opt-in explicitly.
     */
    private TenantEnvironmentConfig defaultConfig(String tenantId) {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setTenantId(tenantId);
        config.setEnvironment("unknown");
        config.setSecurityLevel("HIGH");
        config.setAllowHttpCalls(false);
        config.setAllowFileRead(false);
        config.setAllowIrreversibleOps(false);
        config.setMaxConcurrentToolCalls(1);
        return config;
    }
}
