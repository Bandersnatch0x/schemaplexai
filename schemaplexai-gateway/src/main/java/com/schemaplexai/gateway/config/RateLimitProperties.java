package com.schemaplexai.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for gateway rate limiting.
 * <p>
 * Reads from {@code rate-limit.*} prefix in application.yml:
 * <ul>
 *   <li>{@code rate-limit.enabled} — master toggle (default: true)</li>
 *   <li>{@code rate-limit.default-limit} — max requests per window (default: 100)</li>
 *   <li>{@code rate-limit.window-size} — window in seconds (default: 60)</li>
 *   <li>{@code rate-limit.whitelist-paths} — path patterns to skip rate limiting</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    /** Master toggle for rate limiting. */
    private boolean enabled = true;

    /** Maximum number of requests allowed within the window. */
    private int defaultLimit = 100;

    /** Rate limit window duration in seconds. */
    private int windowSize = 60;

    /** Path patterns (AntPathMatcher) that bypass rate limiting entirely. */
    private List<String> whitelistPaths = new ArrayList<>();
}
