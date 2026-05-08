package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.config.LlmProviderProperties;
import com.schemaplexai.common.exception.BaseException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnthropicProviderTest {

    @Mock(lenient = true)
    private LlmProviderProperties properties;

    @Mock(lenient = true)
    private LlmProviderProperties.ProviderConfig anthropicConfig;

    @Mock(lenient = true)
    private ChatLanguageModel chatLanguageModel;

    private AnthropicProvider anthropicProvider;

    @BeforeEach
    void setUp() {
        when(properties.getAnthropic()).thenReturn(anthropicConfig);
        when(anthropicConfig.getApiKey()).thenReturn("sk-ant-test-key");
        when(anthropicConfig.getBaseUrl()).thenReturn("https://api.anthropic.com/v1");
        when(anthropicConfig.getTimeoutSeconds()).thenReturn(60);
        when(anthropicConfig.getMaxRetries()).thenReturn(3);

        anthropicProvider = new AnthropicProvider(properties);
    }

    @Test
    void getProviderNameShouldReturnAnthropic() {
        assertEquals("ANTHROPIC", anthropicProvider.getProviderName());
    }

    @Test
    void isHealthyShouldReturnFalseWhenApiKeyNotConfigured() {
        when(anthropicConfig.getApiKey()).thenReturn("");
        assertFalse(anthropicProvider.isHealthy());
    }

    @Test
    void isHealthyShouldReturnFalseWhenApiKeyIsNull() {
        when(anthropicConfig.getApiKey()).thenReturn(null);
        assertFalse(anthropicProvider.isHealthy());
    }

    @Test
    void generateShouldThrowWhenApiKeyNotConfigured() {
        when(anthropicConfig.getApiKey()).thenReturn("");
        BaseException ex = assertThrows(BaseException.class,
                () -> anthropicProvider.generate("prompt", "claude-3-sonnet", 0.7));
        assertTrue(ex.getMessage().contains("API key is not configured"));
    }

    @Test
    void generateWithMessagesShouldThrowWhenMessagesNull() {
        BaseException ex = assertThrows(BaseException.class,
                () -> anthropicProvider.generateWithMessages(null, "claude-3-sonnet", 0.7));
        assertTrue(ex.getMessage().contains("cannot be null or empty"));
    }

    @Test
    void generateWithMessagesShouldThrowWhenMessagesEmpty() {
        BaseException ex = assertThrows(BaseException.class,
                () -> anthropicProvider.generateWithMessages(List.of(), "claude-3-sonnet", 0.7));
        assertTrue(ex.getMessage().contains("cannot be null or empty"));
    }

    @Test
    void generateShouldUseInjectedMockModel() throws Exception {
        injectMockModel("claude-3-sonnet@0.7", chatLanguageModel);
        when(chatLanguageModel.generate(anyString())).thenReturn("Hello from Claude");

        String result = anthropicProvider.generate("prompt", "claude-3-sonnet", 0.7);

        assertEquals("Hello from Claude", result);
        verify(chatLanguageModel).generate("prompt");
    }

    @Test
    void generateWithMessagesShouldUseInjectedMockModel() throws Exception {
        injectMockModel("claude-3-sonnet@0.7", chatLanguageModel);
        Response<AiMessage> mockResponse = Response.from(new AiMessage("Chat response"));
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);

        List<LlmMessage> messages = List.of(
                new LlmMessage("system", "You are helpful"),
                new LlmMessage("user", "Hello")
        );
        String result = anthropicProvider.generateWithMessages(messages, "claude-3-sonnet", 0.7);

        assertEquals("Chat response", result);
        verify(chatLanguageModel).generate(anyList());
    }

    @Test
    void generateShouldHandleModelFailure() throws Exception {
        injectMockModel("claude-3-sonnet@0.7", chatLanguageModel);
        when(chatLanguageModel.generate(anyString()))
                .thenThrow(new RuntimeException("API rate limit exceeded"));

        BaseException ex = assertThrows(BaseException.class,
                () -> anthropicProvider.generate("prompt", "claude-3-sonnet", 0.7));
        assertTrue(ex.getMessage().contains("ANTHROPIC generation failed"));
    }

    @Test
    void generateWithMessagesShouldHandleModelFailure() throws Exception {
        injectMockModel("claude-3-sonnet@0.7", chatLanguageModel);
        when(chatLanguageModel.generate(anyList()))
                .thenThrow(new RuntimeException("Connection timeout"));

        List<LlmMessage> messages = List.of(new LlmMessage("user", "Hello"));
        BaseException ex = assertThrows(BaseException.class,
                () -> anthropicProvider.generateWithMessages(messages, "claude-3-sonnet", 0.7));
        assertTrue(ex.getMessage().contains("ANTHROPIC chat completion failed"));
    }

    @Test
    void generateShouldReturnEmptyStringWhenModelReturnsNull() throws Exception {
        injectMockModel("claude-3-sonnet@0.7", chatLanguageModel);
        when(chatLanguageModel.generate(anyString())).thenReturn(null);

        String result = anthropicProvider.generate("prompt", "claude-3-sonnet", 0.7);
        assertEquals("", result);
    }

    @Test
    void generateWithMessagesShouldReturnEmptyStringWhenResponseIsNull() throws Exception {
        injectMockModel("claude-3-sonnet@0.7", chatLanguageModel);
        when(chatLanguageModel.generate(anyList())).thenReturn(null);

        List<LlmMessage> messages = List.of(new LlmMessage("user", "Hello"));
        String result = anthropicProvider.generateWithMessages(messages, "claude-3-sonnet", 0.7);
        assertEquals("", result);
    }

    @Test
    void generateWithMessagesShouldReturnEmptyStringWhenAiMessageTextIsNull() throws Exception {
        injectMockModel("claude-3-sonnet@0.7", chatLanguageModel);
        // AiMessage constructor rejects null text, so we mock the response directly
        @SuppressWarnings("unchecked")
        Response<AiMessage> mockResponse = mock(Response.class);
        AiMessage aiMessage = mock(AiMessage.class);
        when(mockResponse.content()).thenReturn(aiMessage);
        when(aiMessage.text()).thenReturn(null);
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);

        List<LlmMessage> messages = List.of(new LlmMessage("user", "Hello"));
        String result = anthropicProvider.generateWithMessages(messages, "claude-3-sonnet", 0.7);
        assertEquals("", result);
    }

    @Test
    void generateShouldCacheModelForSameConfig() throws Exception {
        injectMockModel("claude-3-sonnet@0.7", chatLanguageModel);
        when(chatLanguageModel.generate(anyString())).thenReturn("response");

        anthropicProvider.generate("prompt1", "claude-3-sonnet", 0.7);
        anthropicProvider.generate("prompt2", "claude-3-sonnet", 0.7);

        verify(chatLanguageModel, times(2)).generate(anyString());
    }

    @Test
    void generateShouldUseDefaultModelWhenModelIdBlank() throws Exception {
        ChatLanguageModel defaultModel = mock(ChatLanguageModel.class);
        injectMockModel("claude-3-sonnet-20240229@0.7", defaultModel);
        when(defaultModel.generate(anyString())).thenReturn("default model response");

        String result = anthropicProvider.generate("prompt", "", 0.7);
        assertEquals("default model response", result);
    }

    @Test
    void generateShouldClampTemperatureAboveMax() throws Exception {
        // Anthropic temperature max is 1.0
        ChatLanguageModel hotModel = mock(ChatLanguageModel.class);
        injectMockModel("claude-3-sonnet@1.0", hotModel);
        when(hotModel.generate(anyString())).thenReturn("hot");

        anthropicProvider.generate("prompt", "claude-3-sonnet", 2.0);
        verify(hotModel).generate(anyString());
    }

    @Test
    void generateShouldClampTemperatureBelowMin() throws Exception {
        ChatLanguageModel coldModel = mock(ChatLanguageModel.class);
        injectMockModel("claude-3-sonnet@0.0", coldModel);
        when(coldModel.generate(anyString())).thenReturn("cold");

        anthropicProvider.generate("prompt", "claude-3-sonnet", -1.0);
        verify(coldModel).generate(anyString());
    }

    @Test
    void generateWithMessagesShouldConvertRolesCorrectly() throws Exception {
        injectMockModel("claude-3-sonnet@0.7", chatLanguageModel);
        Response<AiMessage> mockResponse = Response.from(new AiMessage("ok"));
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);

        List<LlmMessage> messages = List.of(
                new LlmMessage("system", "sys"),
                new LlmMessage("user", "usr"),
                new LlmMessage("assistant", "asst"),
                new LlmMessage("ai", "ai msg"),
                new LlmMessage("human", "human msg"),
                new LlmMessage("unknown", "fallback")
        );

        String result = anthropicProvider.generateWithMessages(messages, "claude-3-sonnet", 0.7);
        assertEquals("ok", result);
        verify(chatLanguageModel).generate(anyList());
    }

    @Test
    void generateWithMessagesShouldThrowWhenMessageRoleIsNull() {
        List<LlmMessage> messages = List.of(new LlmMessage(null, "content"));
        BaseException ex = assertThrows(BaseException.class,
                () -> anthropicProvider.generateWithMessages(messages, "claude-3-sonnet", 0.7));
        assertTrue(ex.getMessage().contains("role cannot be null"));
    }

    @SuppressWarnings("unchecked")
    private void injectMockModel(String cacheKey, ChatLanguageModel mockModel) throws Exception {
        // modelCache is in the parent class LlmProviderAdapter
        Field modelCacheField = LlmProviderAdapter.class.getDeclaredField("modelCache");
        modelCacheField.setAccessible(true);
        Map<String, ChatLanguageModel> cache = (Map<String, ChatLanguageModel>) modelCacheField.get(anthropicProvider);
        cache.put(cacheKey, mockModel);
    }
}
