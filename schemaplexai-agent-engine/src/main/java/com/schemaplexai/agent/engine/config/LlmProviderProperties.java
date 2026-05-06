package com.schemaplexai.agent.engine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for LLM provider integrations.
 *
 * <p>Bound from {@code agent.llm.*} in application.yml.
 * API keys are read from environment variables (never hardcoded).
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.llm")
public class LlmProviderProperties {

    private ProviderConfig openai = new ProviderConfig();
    private ProviderConfig anthropic = new ProviderConfig();

    @Data
    public static class ProviderConfig {
        private String apiKey = "";
        private String baseUrl = "";
        private int timeoutSeconds = 60;
        private int maxRetries = 3;
    }
}
