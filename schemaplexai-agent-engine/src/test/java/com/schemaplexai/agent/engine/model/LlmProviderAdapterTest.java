package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.config.LlmProviderProperties;
import com.schemaplexai.agent.engine.tool.ToolDefinition;
import com.schemaplexai.agent.engine.tool.ToolParameter;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the LlmProviderAdapter template method pattern using a concrete test implementation.
 */
class LlmProviderAdapterTest {

    private TestProvider provider;
    private ChatLanguageModel mockModel;

    @BeforeEach
    void setUp() {
        mockModel = mock(ChatLanguageModel.class);
        provider = new TestProvider(mockModel);
    }

    @Test
    void generateShouldDelegateToModel() {
        when(mockModel.generate("test prompt")).thenReturn("test response");

        String result = provider.generate("test prompt", "test-model", 0.7);

        assertEquals("test response", result);
        verify(mockModel).generate("test prompt");
    }

    @Test
    void generateWithMessagesShouldConvertAndDelegate() {
        when(mockModel.generate(anyList())).thenReturn(
                dev.langchain4j.model.output.Response.from(
                        new dev.langchain4j.data.message.AiMessage("response")));

        List<LlmMessage> messages = List.of(
                new LlmMessage("user", "Hello")
        );

        String result = provider.generateWithMessages(messages, "test-model", 0.7);

        assertEquals("response", result);
    }

    @Test
    void generateWithToolsShouldEnrichAndDelegate() {
        when(mockModel.generate(anyList())).thenReturn(
                dev.langchain4j.model.output.Response.from(
                        new dev.langchain4j.data.message.AiMessage("tool response")));

        List<LlmMessage> messages = List.of(new LlmMessage("user", "Hello"));
        List<ToolDefinition> tools = List.of(
                new ToolDefinition("search", "Search", List.of(), "string")
        );

        String result = provider.generateWithTools(messages, tools, "test-model", 0.7);

        assertEquals("tool response", result);
    }

    @Test
    void shouldUseDefaultModelWhenNull() {
        when(mockModel.generate(anyString())).thenReturn("ok");

        provider.generate("test", null, 0.7);

        assertEquals("test-default", provider.getLastModelId());
    }

    @Test
    void shouldClampTemperature() {
        when(mockModel.generate(anyString())).thenReturn("ok");

        provider.generate("test", "model", 5.0);

        assertEquals(2.0, provider.getLastTemperature());
    }

    @Test
    void shouldCacheModelInstances() {
        when(mockModel.generate(anyString())).thenReturn("ok");

        provider.generate("test", "model1", 0.7);
        provider.generate("test", "model1", 0.7);

        // Model should be created only once (cached)
        assertEquals(1, provider.getCreateCount());
    }

    @Test
    void getProviderNameShouldReturnTestProvider() {
        assertEquals("TEST", provider.getProviderName());
    }

    @Test
    void isHealthyShouldReturnTrue() {
        assertTrue(provider.isHealthy());
    }

    // --- Test implementation of LlmProviderAdapter ---

    private static class TestProvider extends LlmProviderAdapter {

        private final ChatLanguageModel mockModel;
        private String lastModelId;
        private double lastTemperature;
        private int createCount = 0;

        TestProvider(ChatLanguageModel mockModel) {
            this.mockModel = mockModel;
        }

        @Override
        public String getProviderName() {
            return "TEST";
        }

        @Override
        protected String getDefaultModelId() {
            return "test-default";
        }

        @Override
        protected LlmProviderProperties.ProviderConfig getProviderConfig() {
            return new LlmProviderProperties.ProviderConfig();
        }

        @Override
        protected ChatLanguageModel createChatModel(String modelId, double temperature) {
            createCount++;
            lastModelId = modelId;
            lastTemperature = temperature;
            return mockModel;
        }

        @Override
        protected void validateConfiguration() {
            // no-op for test
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        @Override
        protected String getDefaultBaseUrl() {
            return "https://test.api.com/v1";
        }

        String getLastModelId() { return lastModelId; }
        double getLastTemperature() { return lastTemperature; }
        int getCreateCount() { return createCount; }
    }
}
