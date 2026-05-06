package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.config.LlmProviderProperties;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Anthropic (Claude) LLM provider implementation using LangChain4j.
 *
 * <p>Features:
 * <ul>
 *   <li>Per-modelId ChatLanguageModel caching (immutable, thread-safe)</li>
 *   <li>Configurable API key, base URL, timeout, and retries via {@link LlmProviderProperties}</li>
 *   <li>Lightweight health check with minimal API call</li>
 *   <li>Comprehensive error handling with {@link BaseException}</li>
 * </ul>
 */
@Slf4j
@Component
public class AnthropicProvider implements LlmProvider {

    private static final String PROVIDER_NAME = "ANTHROPIC";
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5);
    private static final double DEFAULT_TEMPERATURE = 0.7;

    private final LlmProviderProperties properties;
    private final Map<String, ChatLanguageModel> modelCache = new ConcurrentHashMap<>();

    public AnthropicProvider(LlmProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public String generate(String prompt, String modelId, Double temperature) {
        log.debug("Anthropic generate called: model={}, temp={}", modelId, temperature);
        validateConfiguration();

        ChatLanguageModel model = getOrCreateModel(modelId, temperature);
        try {
            String response = model.generate(prompt);
            log.debug("Anthropic generate success: model={}, responseLength={}", modelId,
                    response != null ? response.length() : 0);
            return response != null ? response : "";
        } catch (Exception e) {
            log.error("Anthropic generate failed: model={}, error={}", modelId, e.getMessage(), e);
            throw new BaseException(ResultCode.AGENT_EXECUTION_FAILED,
                    "Anthropic generation failed: " + e.getMessage());
        }
    }

    @Override
    public String generateWithMessages(List<LlmMessage> messages, String modelId, Double temperature) {
        log.debug("Anthropic chat completion called: model={}, messageCount={}, temp={}",
                modelId, messages != null ? messages.size() : 0, temperature);
        validateConfiguration();

        if (messages == null || messages.isEmpty()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Message list cannot be null or empty");
        }

        List<ChatMessage> chatMessages = convertMessages(messages);
        ChatLanguageModel model = getOrCreateModel(modelId, temperature);

        try {
            Response<AiMessage> response = model.generate(chatMessages);
            String text = extractText(response);
            log.debug("Anthropic chat completion success: model={}, responseLength={}", modelId,
                    text.length());
            return text;
        } catch (Exception e) {
            log.error("Anthropic chat completion failed: model={}, error={}", modelId, e.getMessage(), e);
            throw new BaseException(ResultCode.AGENT_EXECUTION_FAILED,
                    "Anthropic chat completion failed: " + e.getMessage());
        }
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

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    private ChatLanguageModel getOrCreateModel(String modelId, Double temperature) {
        String cacheKey = buildCacheKey(modelId, temperature);
        return modelCache.computeIfAbsent(cacheKey, k -> createModel(modelId, temperature));
    }

    private ChatLanguageModel createModel(String modelId, Double temperature) {
        String resolvedModelId = resolveModelId(modelId);
        double resolvedTemp = resolveTemperature(temperature);
        LlmProviderProperties.ProviderConfig config = properties.getAnthropic();

        log.info("Creating Anthropic model: modelId={}, temperature={}, baseUrl={}",
                resolvedModelId, resolvedTemp, maskBaseUrl(config.getBaseUrl()));

        return AnthropicChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(resolveBaseUrl())
                .modelName(resolvedModelId)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .maxRetries(config.getMaxRetries())
                .temperature(resolvedTemp)
                .build();
    }

    private String buildCacheKey(String modelId, Double temperature) {
        return resolveModelId(modelId) + "@" + resolveTemperature(temperature);
    }

    private String resolveModelId(String modelId) {
        if (modelId != null && !modelId.isBlank()) {
            return modelId.trim();
        }
        return "claude-3-sonnet-20240229";
    }

    private double resolveTemperature(Double temperature) {
        if (temperature != null) {
            return Math.max(0.0, Math.min(1.0, temperature));
        }
        return DEFAULT_TEMPERATURE;
    }

    private String resolveBaseUrl() {
        String baseUrl = properties.getAnthropic().getBaseUrl();
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl.trim();
        }
        return "https://api.anthropic.com/v1";
    }

    private void validateConfiguration() {
        String apiKey = properties.getAnthropic().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Anthropic API key is not configured");
            throw new BaseException(ResultCode.AGENT_EXECUTION_FAILED,
                    "Anthropic API key is not configured. Set ANTHROPIC_API_KEY environment variable.");
        }
    }

    private List<ChatMessage> convertMessages(List<LlmMessage> messages) {
        return messages.stream()
                .map(this::toChatMessage)
                .toList();
    }

    private ChatMessage toChatMessage(LlmMessage msg) {
        if (msg == null || msg.getRole() == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Message or role cannot be null");
        }
        String role = msg.getRole().toLowerCase();
        String content = msg.getContent() != null ? msg.getContent() : "";
        return switch (role) {
            case "system" -> new SystemMessage(content);
            case "assistant", "ai" -> new AiMessage(content);
            case "user", "human" -> new UserMessage(content);
            default -> new UserMessage(content);
        };
    }

    private String maskBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "(default)";
        }
        return baseUrl;
    }

    private String extractText(Response<AiMessage> response) {
        if (response == null || response.content() == null) {
            return "";
        }
        return response.content().text() != null ? response.content().text() : "";
    }
}
