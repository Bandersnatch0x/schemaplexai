package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.config.LlmProviderProperties;
import com.schemaplexai.agent.engine.tool.ToolDefinition;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for LangChain4j-backed LLM providers.
 *
 * <p>Encapsulates common logic shared by all providers:
 * <ul>
 *   <li>Per-modelId ChatLanguageModel caching</li>
 *   <li>Temperature clamping and default resolution</li>
 *   <li>Message conversion via {@link LlmMessageConverter}</li>
 *   <li>Tool description enrichment</li>
 *   <li>Configuration validation</li>
 * </ul>
 *
 * <p>Subclasses only need to implement:
 * <ul>
 *   <li>{@link #getProviderName()} — unique provider identifier</li>
 *   <li>{@link #getDefaultModelId()} — fallback model ID</li>
 *   <li>{@link #getProviderConfig()} — provider-specific configuration</li>
 *   <li>{@link #createChatModel(String, double)} — LangChain4j model builder</li>
 *   <li>{@link #isHealthy()} — provider-specific health check</li>
 * </ul>
 *
 * <p>Adding a new provider (e.g. Gemini, Mistral) requires only extending this class
 * and implementing the 5 abstract methods. No changes to the routing layer needed.
 */
@Slf4j
public abstract class LlmProviderAdapter implements LlmProvider {

    protected static final double DEFAULT_TEMPERATURE = 0.7;

    private final Map<String, ChatLanguageModel> modelCache = new ConcurrentHashMap<>();

    // --- LlmProvider implementation ---

    @Override
    public String generate(String prompt, String modelId, Double temperature) {
        log.debug("{} generate called: model={}, temp={}", getProviderName(), modelId, temperature);
        validateConfiguration();

        ChatLanguageModel model = getOrCreateModel(modelId, temperature);
        try {
            String response = model.generate(prompt);
            log.debug("{} generate success: model={}, responseLength={}", getProviderName(), modelId,
                    response != null ? response.length() : 0);
            return response != null ? response : "";
        } catch (Exception e) {
            log.error("{} generate failed: model={}, error={}", getProviderName(), modelId, e.getMessage(), e);
            throw new BaseException(ResultCode.AGENT_EXECUTION_FAILED,
                    getProviderName() + " generation failed: " + e.getMessage());
        }
    }

    @Override
    public String generateWithMessages(List<LlmMessage> messages, String modelId, Double temperature) {
        log.debug("{} chat completion called: model={}, messageCount={}, temp={}",
                getProviderName(), modelId, messages != null ? messages.size() : 0, temperature);
        validateConfiguration();

        if (messages == null || messages.isEmpty()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Message list cannot be null or empty");
        }

        var chatMessages = LlmMessageConverter.toChatMessages(messages);
        ChatLanguageModel model = getOrCreateModel(modelId, temperature);

        try {
            Response<AiMessage> response = model.generate(chatMessages);
            String text = LlmMessageConverter.extractText(response);
            log.debug("{} chat completion success: model={}, responseLength={}",
                    getProviderName(), modelId, text.length());
            return text;
        } catch (Exception e) {
            log.error("{} chat completion failed: model={}, error={}",
                    getProviderName(), modelId, e.getMessage(), e);
            throw new BaseException(ResultCode.AGENT_EXECUTION_FAILED,
                    getProviderName() + " chat completion failed: " + e.getMessage());
        }
    }

    @Override
    public String generateWithTools(List<LlmMessage> messages, List<ToolDefinition> tools,
                                    String modelId, Double temperature) {
        log.debug("{} generateWithTools called: model={}, messageCount={}, toolCount={}, temp={}",
                getProviderName(), modelId, messages != null ? messages.size() : 0,
                tools != null ? tools.size() : 0, temperature);
        validateConfiguration();

        if (messages == null || messages.isEmpty()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Message list cannot be null or empty");
        }

        List<LlmMessage> enriched = LlmMessageConverter.enrichWithToolDescriptions(messages, tools);
        return generateWithMessages(enriched, modelId, temperature);
    }

    // --- Abstract methods for subclasses ---

    /**
     * Returns the unique provider name (e.g. "OPENAI", "ANTHROPIC", "GEMINI").
     */
    @Override
    public abstract String getProviderName();

    /**
     * Returns the fallback model ID when none is specified.
     */
    protected abstract String getDefaultModelId();

    /**
     * Returns the provider-specific configuration properties.
     */
    protected abstract LlmProviderProperties.ProviderConfig getProviderConfig();

    /**
     * Creates a new LangChain4j {@link ChatLanguageModel} instance.
     *
     * @param modelId     the resolved model ID
     * @param temperature the resolved temperature
     * @return a new model instance
     */
    protected abstract ChatLanguageModel createChatModel(String modelId, double temperature);

    /**
     * Validates that the provider is properly configured (e.g. API key present).
     *
     * @throws BaseException if configuration is invalid
     */
    protected abstract void validateConfiguration();

    /**
     * Performs a provider-specific health check.
     */
    @Override
    public abstract boolean isHealthy();

    // --- Shared helper methods ---

    /**
     * Returns an existing cached model, or creates a new one.
     */
    protected ChatLanguageModel getOrCreateModel(String modelId, Double temperature) {
        String cacheKey = resolveModelId(modelId) + "@" + resolveTemperature(temperature);
        return modelCache.computeIfAbsent(cacheKey, k ->
                createChatModel(resolveModelId(modelId), resolveTemperature(temperature)));
    }

    /**
     * Resolves the model ID, falling back to the provider's default.
     */
    protected String resolveModelId(String modelId) {
        if (modelId != null && !modelId.isBlank()) {
            return modelId.trim();
        }
        return getDefaultModelId();
    }

    /**
     * Resolves and clamps temperature to [0.0, 2.0].
     */
    protected double resolveTemperature(Double temperature) {
        if (temperature != null) {
            return Math.max(0.0, Math.min(2.0, temperature));
        }
        return DEFAULT_TEMPERATURE;
    }

    /**
     * Resolves the base URL, falling back to the provider's default.
     */
    protected String resolveBaseUrl() {
        String baseUrl = getProviderConfig().getBaseUrl();
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl.trim();
        }
        return getDefaultBaseUrl();
    }

    /**
     * Returns the default base URL for the provider (e.g. "https://api.openai.com/v1").
     */
    protected abstract String getDefaultBaseUrl();
}
