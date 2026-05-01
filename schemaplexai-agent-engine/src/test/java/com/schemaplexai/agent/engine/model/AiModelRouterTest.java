package com.schemaplexai.agent.engine.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiModelRouterTest {

    @Mock(lenient = true)
    private LlmProvider openAiProvider;

    @Mock(lenient = true)
    private LlmProvider azureProvider;

    @Mock(lenient = true)
    private StringRedisTemplate redisTemplate;

    @Mock(lenient = true)
    private ValueOperations<String, String> valueOperations;

    private AiModelRouter aiModelRouter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(openAiProvider.getProviderName()).thenReturn("OPENAI");
        when(azureProvider.getProviderName()).thenReturn("AZURE");
        List<LlmProvider> providerList = new ArrayList<>();
        providerList.add(openAiProvider);
        providerList.add(azureProvider);
        aiModelRouter = new AiModelRouter(providerList, redisTemplate);
    }

    @Test
    void routeShouldReturnHealthyProvider() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(openAiProvider.isHealthy()).thenReturn(true);

        LlmProvider result = aiModelRouter.route("gpt-4");

        assertEquals(openAiProvider, result);
    }

    @Test
    void routeShouldSkipProviderOnCooldown() {
        when(redisTemplate.hasKey(contains("OPENAI"))).thenReturn(true);
        when(redisTemplate.hasKey(contains("AZURE"))).thenReturn(false);
        when(azureProvider.isHealthy()).thenReturn(true);

        LlmProvider result = aiModelRouter.route("gpt-4");

        assertEquals(azureProvider, result);
    }

    @Test
    void routeShouldThrowWhenNoHealthyProvider() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(openAiProvider.isHealthy()).thenReturn(false);
        when(azureProvider.isHealthy()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> aiModelRouter.route("gpt-4"));
    }

    @Test
    void generateWithFallbackShouldUsePrimaryProvider() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(openAiProvider.isHealthy()).thenReturn(true);
        when(openAiProvider.generate("prompt", "gpt-4", 0.7)).thenReturn("response");

        String result = aiModelRouter.generateWithFallback("prompt", "gpt-4", 0.7);

        assertEquals("response", result);
        verify(openAiProvider, times(1)).generate("prompt", "gpt-4", 0.7);
    }

    @Test
    void generateWithFallbackShouldFallbackOnPrimaryFailure() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(openAiProvider.isHealthy()).thenReturn(true);
        when(openAiProvider.generate(anyString(), anyString(), anyDouble()))
                .thenThrow(new RuntimeException("OpenAI down"));
        when(azureProvider.isHealthy()).thenReturn(true);
        when(azureProvider.generate("prompt", "gpt-4", 0.7)).thenReturn("azure response");

        String result = aiModelRouter.generateWithFallback("prompt", "gpt-4", 0.7);

        assertEquals("azure response", result);
        verify(valueOperations, atLeast(1)).set(contains("OPENAI"), eq("1"), any());
    }

    @Test
    void generateWithFallbackShouldThrowWhenAllProvidersFail() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(openAiProvider.isHealthy()).thenReturn(true);
        when(openAiProvider.generate(anyString(), anyString(), anyDouble()))
                .thenThrow(new RuntimeException("OpenAI down"));
        when(azureProvider.isHealthy()).thenReturn(true);
        when(azureProvider.generate(anyString(), anyString(), anyDouble()))
                .thenThrow(new RuntimeException("Azure down"));

        assertThrows(IllegalStateException.class,
                () -> aiModelRouter.generateWithFallback("prompt", "gpt-4", 0.7));
    }

    @Test
    void generateWithMessagesShouldUsePrimaryProvider() {
        List<LlmMessage> messages = List.of(new LlmMessage("user", "Hello"));
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(openAiProvider.isHealthy()).thenReturn(true);
        when(openAiProvider.generateWithMessages(messages, "gpt-4", 0.7)).thenReturn("response");

        String result = aiModelRouter.generateWithMessages(messages, "gpt-4", 0.7);

        assertEquals("response", result);
        verify(openAiProvider, times(1)).generateWithMessages(messages, "gpt-4", 0.7);
    }

    @Test
    void generateWithMessagesShouldFallbackOnPrimaryFailure() {
        List<LlmMessage> messages = List.of(new LlmMessage("user", "Hello"));
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(openAiProvider.isHealthy()).thenReturn(true);
        when(openAiProvider.generateWithMessages(anyList(), anyString(), anyDouble()))
                .thenThrow(new RuntimeException("OpenAI down"));
        when(azureProvider.isHealthy()).thenReturn(true);
        when(azureProvider.generateWithMessages(messages, "gpt-4", 0.7)).thenReturn("azure response");

        String result = aiModelRouter.generateWithMessages(messages, "gpt-4", 0.7);

        assertEquals("azure response", result);
    }

    @Test
    void generateWithFallbackShouldSkipUnhealthyProviders() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(openAiProvider.isHealthy()).thenReturn(false);
        when(azureProvider.isHealthy()).thenReturn(true);
        when(azureProvider.generate("prompt", "gpt-4", 0.7)).thenReturn("azure response");

        String result = aiModelRouter.generateWithFallback("prompt", "gpt-4", 0.7);

        assertEquals("azure response", result);
        verify(openAiProvider, never()).generate(anyString(), anyString(), anyDouble());
    }

    @Test
    void routeShouldCacheProviderForModel() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(openAiProvider.isHealthy()).thenReturn(true);

        aiModelRouter.route("gpt-4");
        aiModelRouter.route("gpt-4");

        verify(openAiProvider, times(2)).isHealthy();
    }
}
