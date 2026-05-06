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
class OpenAiProviderTest {

    @Mock(lenient = true)
    private LlmProviderProperties properties;

    @Mock(lenient = true)
    private LlmProviderProperties.ProviderConfig openaiConfig;

    @Mock(lenient = true)
    private ChatLanguageModel chatLanguageModel;

    private OpenAiProvider openAiProvider;

    @BeforeEach
    void setUp() {
        when(properties.getOpenai()).thenReturn(openaiConfig);
        when(openaiConfig.getApiKey()).thenReturn("sk-test-key");
        when(openaiConfig.getBaseUrl()).thenReturn("https://api.openai.com/v1");
        when(openaiConfig.getTimeoutSeconds()).thenReturn(60);
        when(openaiConfig.getMaxRetries()).thenReturn(3);

        openAiProvider = new OpenAiProvider(properties);
    }

    @Test
    void getProviderNameShouldReturnOpenAi() {
        assertEquals("OPENAI", openAiProvider.getProviderName());
    }

    @Test
    void isHealthyShouldReturnFalseWhenApiKeyNotConfigured() {
        when(openaiConfig.getApiKey()).thenReturn("");
        assertFalse(openAiProvider.isHealthy());
    }

    @Test
    void isHealthyShouldReturnFalseWhenApiKeyIsNull() {
        when(openaiConfig.getApiKey()).thenReturn(null);
        assertFalse(openAiProvider.isHealthy());
    }

    @Test
    void generateShouldThrowWhenApiKeyNotConfigured() {
        when(openaiConfig.getApiKey()).thenReturn("");
        BaseException ex = assertThrows(BaseException.class,
                () -> openAiProvider.generate("prompt", "gpt-4", 0.7));
        assertTrue(ex.getMessage().contains("API key is not configured"));
    }

    @Test
    void generateWithMessagesShouldThrowWhenMessagesNull() {
        BaseException ex = assertThrows(BaseException.class,
                () -> openAiProvider.generateWithMessages(null, "gpt-4", 0.7));
        assertTrue(ex.getMessage().contains("cannot be null or empty"));
    }

    @Test
    void generateWithMessagesShouldThrowWhenMessagesEmpty() {
        BaseException ex = assertThrows(BaseException.class,
                () -> openAiProvider.generateWithMessages(List.of(), "gpt-4", 0.7));
        assertTrue(ex.getMessage().contains("cannot be null or empty"));
    }

    @Test
    void generateShouldUseInjectedMockModel() throws Exception {
        // Inject a mock model directly into the cache to avoid real API calls
        injectMockModel("gpt-4@0.7", chatLanguageModel);
        when(chatLanguageModel.generate(anyString())).thenReturn("Hello from OpenAI");

        String result = openAiProvider.generate("prompt", "gpt-4", 0.7);

        assertEquals("Hello from OpenAI", result);
        verify(chatLanguageModel).generate("prompt");
    }

    @Test
    void generateWithMessagesShouldUseInjectedMockModel() throws Exception {
        injectMockModel("gpt-4@0.7", chatLanguageModel);
        Response<AiMessage> mockResponse = Response.from(new AiMessage("Chat response"));
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);

        List<LlmMessage> messages = List.of(
                new LlmMessage("system", "You are helpful"),
                new LlmMessage("user", "Hello")
        );
        String result = openAiProvider.generateWithMessages(messages, "gpt-4", 0.7);

        assertEquals("Chat response", result);
        verify(chatLanguageModel).generate(anyList());
    }

    @Test
    void generateShouldHandleModelFailure() throws Exception {
        injectMockModel("gpt-4@0.7", chatLanguageModel);
        when(chatLanguageModel.generate(anyString()))
                .thenThrow(new RuntimeException("API rate limit exceeded"));

        BaseException ex = assertThrows(BaseException.class,
                () -> openAiProvider.generate("prompt", "gpt-4", 0.7));
        assertTrue(ex.getMessage().contains("OpenAI generation failed"));
    }

    @Test
    void generateWithMessagesShouldHandleModelFailure() throws Exception {
        injectMockModel("gpt-4@0.7", chatLanguageModel);
        when(chatLanguageModel.generate(anyList()))
                .thenThrow(new RuntimeException("Connection timeout"));

        List<LlmMessage> messages = List.of(new LlmMessage("user", "Hello"));
        BaseException ex = assertThrows(BaseException.class,
                () -> openAiProvider.generateWithMessages(messages, "gpt-4", 0.7));
        assertTrue(ex.getMessage().contains("OpenAI chat completion failed"));
    }

    @Test
    void generateWithMessagesShouldReturnEmptyStringWhenResponseIsNull() throws Exception {
        injectMockModel("gpt-4@0.7", chatLanguageModel);
        when(chatLanguageModel.generate(anyList())).thenReturn(null);

        List<LlmMessage> messages = List.of(new LlmMessage("user", "Hello"));
        String result = openAiProvider.generateWithMessages(messages, "gpt-4", 0.7);
        assertEquals("", result);
    }

    @Test
    void generateShouldReturnEmptyStringWhenModelReturnsNull() throws Exception {
        injectMockModel("gpt-4@0.7", chatLanguageModel);
        when(chatLanguageModel.generate(anyString())).thenReturn(null);

        String result = openAiProvider.generate("prompt", "gpt-4", 0.7);
        assertEquals("", result);
    }

    @Test
    void generateWithMessagesShouldReturnEmptyStringWhenAiMessageTextIsNull() throws Exception {
        injectMockModel("gpt-4@0.7", chatLanguageModel);
        // AiMessage constructor rejects null text, so we mock the response directly
        @SuppressWarnings("unchecked")
        Response<AiMessage> mockResponse = mock(Response.class);
        AiMessage aiMessage = mock(AiMessage.class);
        when(mockResponse.content()).thenReturn(aiMessage);
        when(aiMessage.text()).thenReturn(null);
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);

        List<LlmMessage> messages = List.of(new LlmMessage("user", "Hello"));
        String result = openAiProvider.generateWithMessages(messages, "gpt-4", 0.7);
        assertEquals("", result);
    }

    @Test
    void generateShouldCacheModelForSameConfig() throws Exception {
        injectMockModel("gpt-4@0.7", chatLanguageModel);
        when(chatLanguageModel.generate(anyString())).thenReturn("response");

        openAiProvider.generate("prompt1", "gpt-4", 0.7);
        openAiProvider.generate("prompt2", "gpt-4", 0.7);

        // Should reuse the same cached model, so only one generate call on the mock
        // Wait - the provider calls generate on the model each time, so 2 calls is correct
        verify(chatLanguageModel, times(2)).generate(anyString());
    }

    @Test
    void generateShouldUseDefaultModelWhenModelIdBlank() throws Exception {
        ChatLanguageModel defaultModel = mock(ChatLanguageModel.class);
        injectMockModel("gpt-4o@0.7", defaultModel);
        when(defaultModel.generate(anyString())).thenReturn("default model response");

        String result = openAiProvider.generate("prompt", "", 0.7);
        assertEquals("default model response", result);
    }

    @Test
    void generateShouldClampTemperatureAboveMax() throws Exception {
        ChatLanguageModel hotModel = mock(ChatLanguageModel.class);
        injectMockModel("gpt-4@2.0", hotModel);
        when(hotModel.generate(anyString())).thenReturn("hot");

        openAiProvider.generate("prompt", "gpt-4", 5.0);
        // Temperature should be clamped to 2.0, so cache key is gpt-4@2.0
        verify(hotModel).generate(anyString());
    }

    @Test
    void generateShouldClampTemperatureBelowMin() throws Exception {
        ChatLanguageModel coldModel = mock(ChatLanguageModel.class);
        injectMockModel("gpt-4@0.0", coldModel);
        when(coldModel.generate(anyString())).thenReturn("cold");

        openAiProvider.generate("prompt", "gpt-4", -1.0);
        verify(coldModel).generate(anyString());
    }

    @Test
    void generateWithMessagesShouldConvertRolesCorrectly() throws Exception {
        injectMockModel("gpt-4@0.7", chatLanguageModel);
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

        String result = openAiProvider.generateWithMessages(messages, "gpt-4", 0.7);
        assertEquals("ok", result);
        verify(chatLanguageModel).generate(anyList());
    }

    @Test
    void generateWithMessagesShouldThrowWhenMessageRoleIsNull() throws Exception {
        List<LlmMessage> messages = List.of(new LlmMessage(null, "content"));
        BaseException ex = assertThrows(BaseException.class,
                () -> openAiProvider.generateWithMessages(messages, "gpt-4", 0.7));
        assertTrue(ex.getMessage().contains("role cannot be null"));
    }

    @SuppressWarnings("unchecked")
    private void injectMockModel(String cacheKey, ChatLanguageModel mockModel) throws Exception {
        Field modelCacheField = OpenAiProvider.class.getDeclaredField("modelCache");
        modelCacheField.setAccessible(true);
        Map<String, ChatLanguageModel> cache = (Map<String, ChatLanguageModel>) modelCacheField.get(openAiProvider);
        cache.put(cacheKey, mockModel);
    }
}
