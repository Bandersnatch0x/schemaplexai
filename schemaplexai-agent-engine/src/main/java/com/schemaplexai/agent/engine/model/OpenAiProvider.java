package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.config.LlmProviderProperties;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * OpenAI LLM provider implementation using LangChain4j.
 *
 * <p>Extends {@link LlmProviderAdapter} which handles caching, message conversion,
 * tool enrichment, and common validation. This class only implements the
 * OpenAI-specific model creation and health check logic.
 *
 * <p>Excluded when the {@code mock} profile is active (see {@link MockLlmProvider}).
 */
@Slf4j
@Component
@Profile("!mock")
public class OpenAiProvider extends LlmProviderAdapter {

    private static final String PROVIDER_NAME = "OPENAI";
    private static final String DEFAULT_MODEL = "gpt-4o";
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5);

    private final LlmProviderProperties properties;

    public OpenAiProvider(LlmProviderProperties properties) {
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
        return properties.getOpenai();
    }

    @Override
    protected String getDefaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }

    @Override
    protected ChatLanguageModel createChatModel(String modelId, double temperature) {
        LlmProviderProperties.ProviderConfig config = properties.getOpenai();

        log.info("Creating OpenAI model: modelId={}, temperature={}, baseUrl={}",
                modelId, temperature, resolveBaseUrl());

        return OpenAiChatModel.builder()
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
        String apiKey = properties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.error("OpenAI API key is not configured");
            throw new BaseException(ResultCode.AGENT_EXECUTION_FAILED,
                    "OpenAI API key is not configured. Set OPENAI_API_KEY environment variable.");
        }
    }

    @Override
    public boolean isHealthy() {
        String apiKey = properties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("OpenAI health check: API key not configured");
            return false;
        }

        try {
            ChatLanguageModel testModel = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(resolveBaseUrl())
                    .modelName("gpt-4o-mini")
                    .timeout(HEALTH_CHECK_TIMEOUT)
                    .maxRetries(0)
                    .temperature(0.0)
                    .build();

            testModel.generate("hi");
            log.debug("OpenAI health check passed");
            return true;
        } catch (Exception e) {
            log.warn("OpenAI health check failed: {}", e.getMessage());
            return false;
        }
    }
}
