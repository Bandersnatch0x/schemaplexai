package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.config.LlmProviderProperties;
import com.schemaplexai.agent.engine.tool.ToolDefinition;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenAI LLM provider implementation using LangChain4j.
 *
 * <p>Features:
 * <ul>
 *   <li>Per-modelId ChatLanguageModel caching (immutable, thread-safe)</li>
 *   <li>Configurable API key, base URL, timeout, and retries via {@link LlmProviderProperties}</li>
 *   <li>Lightweight health check via models list endpoint</li>
 *   <li>Comprehensive error handling with {@link BaseException}</li>
 * </ul>
 *
 * <p>Excluded when the {@code mock} profile is active (see {@link MockLlmProvider}).
 */
@Slf4j
@Component
@Profile("!mock")
public class OpenAiProvider implements LlmProvider {

    private static final String PROVIDER_NAME = "OPENAI";
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5);
    private static final double DEFAULT_TEMPERATURE = 0.7;

    private final LlmProviderProperties properties;
    private final Map<String, ChatLanguageModel> modelCache = new ConcurrentHashMap<>();

    public OpenAiProvider(LlmProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public String generate(String prompt, String modelId, Double temperature) {
        log.debug("OpenAI generate called: model={}, temp={}", modelId, temperature);
        validateConfiguration();

        ChatLanguageModel model = getOrCreateModel(modelId, temperature);
        try {
            String response = model.generate(prompt);
            log.debug("OpenAI generate success: model={}, responseLength={}", modelId,
                    response != null ? response.length() : 0);
            return response != null ? response : "";
        } catch (Exception e) {
            log.error("OpenAI generate failed: model={}, error={}", modelId, e.getMessage(), e);
            throw new BaseException(ResultCode.AGENT_EXECUTION_FAILED,
                    "OpenAI generation failed: " + e.getMessage());
        }
    }

    @Override
    public String generateWithMessages(List<LlmMessage> messages, String modelId, Double temperature) {
        log.debug("OpenAI chat completion called: model={}, messageCount={}, temp={}",
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
            log.debug("OpenAI chat completion success: model={}, responseLength={}", modelId,
                    text.length());
            return text;
        } catch (Exception e) {
            log.error("OpenAI chat completion failed: model={}, error={}", modelId, e.getMessage(), e);
            throw new BaseException(ResultCode.AGENT_EXECUTION_FAILED,
                    "OpenAI chat completion failed: " + e.getMessage());
        }
    }

    @Override
    public String generateWithTools(List<LlmMessage> messages, List<ToolDefinition> tools,
                                    String modelId, Double temperature) {
        log.debug("OpenAI generateWithTools called: model={}, messageCount={}, toolCount={}, temp={}",
                modelId, messages != null ? messages.size() : 0,
                tools != null ? tools.size() : 0, temperature);
        validateConfiguration();

        if (messages == null || messages.isEmpty()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Message list cannot be null or empty");
        }

        List<LlmMessage> enriched = enrichWithToolDescriptions(messages, tools);
        return generateWithMessages(enriched, modelId, temperature);
    }

    /**
     * Enriches the message list by prepending tool descriptions to the first system message,
     * or inserting a new system message if none exists.
     */
    private List<LlmMessage> enrichWithToolDescriptions(List<LlmMessage> messages, List<ToolDefinition> tools) {
        if (tools == null || tools.isEmpty()) {
            return messages;
        }

        String toolSection = formatToolSection(tools);
        List<LlmMessage> enriched = new ArrayList<>(messages.size() + 1);

        boolean systemInjected = false;
        for (LlmMessage msg : messages) {
            if (!systemInjected && "system".equalsIgnoreCase(msg.getRole())) {
                enriched.add(new LlmMessage("system",
                        msg.getContent() + "\n\n" + toolSection));
                systemInjected = true;
            } else {
                enriched.add(msg);
            }
        }

        if (!systemInjected) {
            enriched.add(0, new LlmMessage("system", toolSection));
        }

        return enriched;
    }

    private String formatToolSection(List<ToolDefinition> tools) {
        StringBuilder sb = new StringBuilder();
        sb.append("Available tools:\n\n");
        for (ToolDefinition tool : tools) {
            sb.append("- ").append(tool.name()).append(": ").append(tool.description()).append("\n");
            if (tool.parameters() != null) {
                tool.parameters().forEach(p ->
                        sb.append(String.format("  - %s (%s)%s: %s%n",
                                p.name(), p.type(), p.required() ? ", required" : "", p.description())));
            }
        }
        sb.append("\nUse Thought/Action/Action Input format to invoke tools. ");
        sb.append("Use Final Answer when you have the answer.");
        return sb.toString();
    }

    @Override
    public boolean isHealthy() {
        String apiKey = properties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("OpenAI health check: API key not configured");
            return false;
        }

        try {
            // Lightweight health check: attempt to create a model instance
            // If the API key is invalid, this will typically fail fast during model creation
            // For a more robust check, we could make an actual API call, but that costs tokens
            ChatLanguageModel testModel = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(resolveBaseUrl())
                    .modelName("gpt-4o-mini")
                    .timeout(HEALTH_CHECK_TIMEOUT)
                    .maxRetries(0)
                    .temperature(0.0)
                    .build();

            // Minimal generation to verify connectivity
            testModel.generate("hi");
            log.debug("OpenAI health check passed");
            return true;
        } catch (Exception e) {
            log.warn("OpenAI health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * Returns an existing cached model for the given key, or creates a new one.
     * The cache key combines modelId and temperature to ensure correct behavior.
     */
    private ChatLanguageModel getOrCreateModel(String modelId, Double temperature) {
        String cacheKey = buildCacheKey(modelId, temperature);
        return modelCache.computeIfAbsent(cacheKey, k -> createModel(modelId, temperature));
    }

    private ChatLanguageModel createModel(String modelId, Double temperature) {
        String resolvedModelId = resolveModelId(modelId);
        double resolvedTemp = resolveTemperature(temperature);
        LlmProviderProperties.ProviderConfig config = properties.getOpenai();

        log.info("Creating OpenAI model: modelId={}, temperature={}, baseUrl={}",
                resolvedModelId, resolvedTemp, maskBaseUrl(config.getBaseUrl()));

        return OpenAiChatModel.builder()
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
        return "gpt-4o";
    }

    private double resolveTemperature(Double temperature) {
        if (temperature != null) {
            return Math.max(0.0, Math.min(2.0, temperature));
        }
        return DEFAULT_TEMPERATURE;
    }

    private String resolveBaseUrl() {
        String baseUrl = properties.getOpenai().getBaseUrl();
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl.trim();
        }
        return "https://api.openai.com/v1";
    }

    private void validateConfiguration() {
        String apiKey = properties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.error("OpenAI API key is not configured");
            throw new BaseException(ResultCode.AGENT_EXECUTION_FAILED,
                    "OpenAI API key is not configured. Set OPENAI_API_KEY environment variable.");
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
