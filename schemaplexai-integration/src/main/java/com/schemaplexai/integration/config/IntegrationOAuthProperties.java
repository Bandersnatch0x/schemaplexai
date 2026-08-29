package com.schemaplexai.integration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth client configuration for Git providers (issue 916).
 *
 * <p>Client ids and secrets are sourced from environment variables via
 * {@code application.yml} placeholders; nothing sensitive is hardcoded.
 * Providers are looked up by their lower-cased name (e.g. {@code github},
 * {@code gitlab}); unknown providers simply have no entry.
 */
@Data
@ConfigurationProperties(prefix = "integration.oauth")
public class IntegrationOAuthProperties {

    /** Provider name (lower-case) -> OAuth client settings. */
    private Map<String, Provider> providers = new HashMap<>();

    @Data
    public static class Provider {
        private String clientId = "";
        private String clientSecret = "";
        /** Provider token endpoint used for the authorization_code exchange. */
        private String tokenUrl = "";
        /** Optional redirect URI registered with the provider. */
        private String redirectUri = "";
    }

    /** Resolve provider settings by name, or null when unconfigured. */
    public Provider provider(String name) {
        if (name == null) {
            return null;
        }
        return providers.get(name.toLowerCase());
    }
}
