package com.schemaplexai.system.service;

import com.schemaplexai.system.entity.SfTenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Republishes every tenant to the gateway tenant cache channel at startup
 * (issue 913). Guarantees the channel converges to the database state after a
 * Redis flush or a missed write-through, and provides the initial population
 * for tenants created before the channel existed.
 *
 * <p>Best-effort by design: startup must never be blocked by the cache channel
 * (the gateway reads fail-closed, so worst case tenants are re-synced on the
 * next mutation or restart).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantCacheBackfillRunner implements ApplicationRunner {

    private final TenantService tenantService;
    private final TenantCacheSyncer tenantCacheSyncer;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<SfTenant> tenants = tenantService.list();
            tenants.forEach(tenantCacheSyncer::sync);
            log.info("Tenant cache backfill complete: {} tenants published for gateway validation",
                    tenants.size());
        } catch (Exception e) {
            log.warn("Tenant cache backfill failed (gateway tenant validation may reject "
                    + "tenants until the next mutation or restart): {}", e.getMessage());
        }
    }
}
