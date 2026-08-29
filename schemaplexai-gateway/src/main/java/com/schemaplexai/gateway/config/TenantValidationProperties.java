package com.schemaplexai.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration properties for gateway tenant validation (issue 913, spec §4.3).
 * <p>
 * Reads from {@code tenant.validation.*} in application.yml:
 * <ul>
 *   <li>{@code tenant.validation.enabled} — master toggle (default: true)</li>
 *   <li>{@code tenant.validation.cache-max-size} — L1 (Caffeine) max entries</li>
 *   <li>{@code tenant.validation.cache-ttl} — L1 entry TTL (Redis is the L2)</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "tenant.validation")
public class TenantValidationProperties {

    /** Master toggle for tenant existence validation. */
    private boolean enabled = true;

    /** Maximum number of tenant status entries kept in the local (L1) cache. */
    private long cacheMaxSize = 10_000;

    /** Time-to-live for local (L1) cache entries; Redis acts as the shared L2. */
    private Duration cacheTtl = Duration.ofMinutes(5);
}
