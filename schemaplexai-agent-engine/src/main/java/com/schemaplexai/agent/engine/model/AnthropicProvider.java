package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.config.LlmProviderProperties;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Anthropic (Claude) LLM provider implementation using LangChain4j.
 *
 * <p>Extends {@link LlmProviderAdapter} which handles caching, message conversion,
 * tool enrichment, and common validation. This class only implements the
 * Anthropic-specific model creation and health check logic.
 *
 * <p>Excluded when the {@code mock} profile is active (see {@link MockLlmProvider}).
 */
@Slf4j
@Component
@Profile("!mock")
public class AnthropicProvider extends LlmProviderAdapter {

    private static final String PROVIDER_NAME = "ANTHROPIC";
    private static final String DEFAULT_MODEL = "claude-3-sonnet-20240229";
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com/v1";
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5);

    private final LlmProviderProperties properties;

    public AnthropicProvider(LlmProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    protected String getDefaultModelId() {
        return DEFAULT_MODEL;
    }

    @Override
    protected LlmProviderProperties.ProviderConfig getProviderConfig() {
        return properties.getAnthropic();
    }

    @Override
    protected String getDefaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }

    @Override
    protected ChatLanguageModel createChatModel(String modelId, double temperature) {
        LlmProviderProperties.ProviderConfig config = properties.getAnthropic();

        log.info("Creating Anthropic model: modelId={}, temperature={}, baseUrl={}",
                modelId, temperature, resolveBaseUrl());

        return AnthropicChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(resolveBaseUrl())
                .modelName(modelId)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .maxRetries(config.getMaxRetries())
                .temperature(temperature)
                .build();
    }

    @Override
    protected void validateConfiguration() {
        String apiKey = properties.getAnthropic().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Anthropic API key is not configured");
            throw new BaseException(ResultCode.AGENT_EXECUTION_FAILED,
                    "Anthropic API key is not configured. Set ANTHROPIC_API_KEY environment variable.");
        }
    }

    /**
     * Anthropic temperature range is [0.0, 1.0], narrower than the base adapter's [0.0, 2.0].
     */
    @Override
    protected double resolveTemperature(Double temperature) {
        if (temperature != null) {
            return Math.max(0.0, Math.min(1.0, temperature));
        }
        return DEFAULT_TEMPERATURE;
    }

    @Override
    public boolean isHealthy() {
        String apiKey = properties.getAnthropic().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Anthropic health check: API key not configured");
            return false;
        }

        try {
            ChatLanguageModel testModel = AnthropicChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(resolveBaseUrl())
                    .modelName("claude-3-haiku-20240307")
                    .timeout(HEALTH_CHECK_TIMEOUT)
                    .maxRetries(0)
                    .temperature(0.0)
                    .build();

            testModel.generate("hi");
            log.debug("Anthropic health check passed");
            return true;
        } catch (Exception e) {
            log.warn("Anthropic health check failed: {}", e.getMessage());
            return false;
        }
    }
}
