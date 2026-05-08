package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.tool.ToolDefinition;
import com.schemaplexai.common.constants.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelRouter {

    private final List<LlmProvider> providers;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, LlmProvider> providerCache = new ConcurrentHashMap<>();

    private static final Duration COOLDOWN_DURATION = Duration.ofMinutes(5);

    public LlmProvider route(String preferredModelId) {
        for (LlmProvider provider : providers) {
            if (isOnCooldown(provider.getProviderName())) {
                log.warn("Provider {} is on cooldown, skipping", provider.getProviderName());
                continue;
            }
            if (provider.isHealthy()) {
                providerCache.put(preferredModelId, provider);
                return provider;
            }
        }
        throw new IllegalStateException("No healthy LLM provider available");
    }

    public String generateWithFallback(String prompt, String modelId, Double temperature) {
        LlmProvider primary = null;
        try {
            primary = route(modelId);
            return primary.generate(prompt, modelId, temperature);
        } catch (Exception e) {
            log.error("Primary provider failed for model {}, attempting fallback", modelId, e);
            if (primary != null) {
                activateCooldown(primary.getProviderName());
            }
            for (LlmProvider fallback : providers) {
                if (fallback.isHealthy()) {
                    try {
                        return fallback.generate(prompt, modelId, temperature);
                    } catch (Exception ex) {
                        log.error("Fallback provider {} failed", fallback.getProviderName(), ex);
                        activateCooldown(fallback.getProviderName());
                    }
                }
            }
            throw new IllegalStateException("All LLM providers failed");
        }
    }

    public String generateWithMessages(List<LlmMessage> messages, String modelId, Double temperature) {
        LlmProvider primary = null;
        try {
            primary = route(modelId);
            return primary.generateWithMessages(messages, modelId, temperature);
        } catch (Exception e) {
            log.error("Primary provider failed for messages, attempting fallback", e);
            if (primary != null) {
                activateCooldown(primary.getProviderName());
            }
            for (LlmProvider fallback : providers) {
                if (fallback.isHealthy()) {
                    try {
                        return fallback.generateWithMessages(messages, modelId, temperature);
                    } catch (Exception ex) {
                        log.error("Fallback provider {} failed", fallback.getProviderName(), ex);
                        activateCooldown(fallback.getProviderName());
                    }
                }
            }
            throw new IllegalStateException("All LLM providers failed for messages");
        }
    }

    public String generateWithTools(List<LlmMessage> messages, List<ToolDefinition> tools,
                                    String modelId, Double temperature) {
        LlmProvider primary = null;
        try {
            primary = route(modelId);
            return primary.generateWithTools(messages, tools, modelId, temperature);
        } catch (Exception e) {
            log.error("Primary provider failed for generateWithTools, attempting fallback", e);
            if (primary != null) {
                activateCooldown(primary.getProviderName());
            }
            for (LlmProvider fallback : providers) {
                if (fallback.isHealthy()) {
                    try {
                        return fallback.generateWithTools(messages, tools, modelId, temperature);
                    } catch (Exception ex) {
                        log.error("Fallback provider {} failed", fallback.getProviderName(), ex);
                        activateCooldown(fallback.getProviderName());
                    }
                }
            }
            throw new IllegalStateException("All LLM providers failed for generateWithTools");
        }
    }

    private boolean isOnCooldown(String providerName) {
        String key = String.format(CommonConstants.REDIS_KEY_MODEL_COOLDOWN, providerName);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private void activateCooldown(String providerName) {
        String key = String.format(CommonConstants.REDIS_KEY_MODEL_COOLDOWN, providerName);
        redisTemplate.opsForValue().set(key, "1", COOLDOWN_DURATION);
    }
}
