package com.schemaplexai.agent.engine.external;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for external agent adapters.
 *
 * <p>Bound from {@code agent.external.*} in application.yml.
 * Disabled by default; set {@code agent.external.enabled=true} to activate.
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.external")
public class ExternalAgentConfig {

    /** Feature flag — external agent adapters are disabled by default. */
    private boolean enabled = false;

    /** Provider identifier (e.g. "codex", "openai", "anthropic"). */
    private String provider = "";

    /** Model identifier to use with the external agent. */
    private String model = "";

    /** API key for the external agent provider (read from env, never hardcoded). */
    private String apiKey = "";

    /** Base URL for the external agent API (empty = provider default). */
    private String baseUrl = "";

    /** Request timeout in milliseconds. */
    private int timeoutMs = 60000;

    /** Maximum number of retries for failed requests. */
    private int maxRetries = 3;
}
