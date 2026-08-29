package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.config.LlmProviderProperties;
import com.schemaplexai.agent.engine.cost.LlmUsageDispatch;
import com.schemaplexai.agent.engine.tool.ToolDefinition;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @AfterEach
    void tearDown() {
        // Usage reporting uses a static dispatch; never leak listeners between tests.
        LlmUsageDispatch.setListener(null);
    }

    @Test
    void generateShouldDelegateToModel() {
        when(mockModel.generate(any(ChatMessage.class))).thenReturn(
                Response.from(new AiMessage("test response")));

        String result = provider.generate("test prompt", "test-model", 0.7);

        assertEquals("test response", result);
        verify(mockModel).generate(any(ChatMessage.class));
    }

    @Test
    void generateShouldReportTokenUsage() {
        TokenUsage usage = new TokenUsage(120, 30, 150);
        when(mockModel.generate(any(ChatMessage.class))).thenReturn(
                Response.from(new AiMessage("test response"), usage, null));

        AtomicReference<String> reportedProvider = new AtomicReference<>();
        AtomicReference<String> reportedModel = new AtomicReference<>();
        AtomicReference<TokenUsage> reportedUsage = new AtomicReference<>();
        LlmUsageDispatch.setListener((providerName, modelId, tokenUsage) -> {
            reportedProvider.set(providerName);
            reportedModel.set(modelId);
            reportedUsage.set(tokenUsage);
        });

        provider.generate("test prompt", "test-model", 0.7);

        assertEquals("TEST", reportedProvider.get());
        assertEquals("test-model", reportedModel.get());
        assertSame(usage, reportedUsage.get());
    }

    @Test
    void generateShouldNotReportWhenResponseHasNoUsage() {
        when(mockModel.generate(any(ChatMessage.class))).thenReturn(
                Response.from(new AiMessage("test response")));

        AtomicInteger reportCount = new AtomicInteger();
        LlmUsageDispatch.setListener((providerName, modelId, tokenUsage) -> reportCount.incrementAndGet());

        provider.generate("test prompt", "test-model", 0.7);

        assertEquals(0, reportCount.get());
    }

    @Test
    void generateWithMessagesShouldConvertAndDelegate() {
        when(mockModel.generate(anyList())).thenReturn(
                Response.from(new AiMessage("response")));

        List<LlmMessage> messages = List.of(
                new LlmMessage("user", "Hello")
        );

        String result = provider.generateWithMessages(messages, "test-model", 0.7);

        assertEquals("response", result);
    }

    @Test
    void generateWithMessagesShouldReportTokenUsage() {
        TokenUsage usage = new TokenUsage(500, 250, 750);
        when(mockModel.generate(anyList())).thenReturn(
                Response.from(new AiMessage("response"), usage, null));

        AtomicReference<TokenUsage> reportedUsage = new AtomicReference<>();
        LlmUsageDispatch.setListener((providerName, modelId, tokenUsage) -> reportedUsage.set(tokenUsage));

        provider.generateWithMessages(List.of(new LlmMessage("user", "Hello")), "test-model", 0.7);

        assertSame(usage, reportedUsage.get());
    }

    @Test
    void generateWithToolsShouldEnrichAndDelegate() {
        when(mockModel.generate(anyList())).thenReturn(
                Response.from(new AiMessage("tool response")));

        List<LlmMessage> messages = List.of(new LlmMessage("user", "Hello"));
        List<ToolDefinition> tools = List.of(
                new ToolDefinition("search", "Search", List.of(), "string")
        );

        String result = provider.generateWithTools(messages, tools, "test-model", 0.7);

        assertEquals("tool response", result);
    }

    @Test
    void shouldUseDefaultModelWhenNull() {
        when(mockModel.generate(any(ChatMessage.class))).thenReturn(
                Response.from(new AiMessage("ok")));

        provider.generate("test", null, 0.7);

        assertEquals("test-default", provider.getLastModelId());
    }

    @Test
    void generateShouldReportResolvedDefaultModelWhenModelIdNull() {
        TokenUsage usage = new TokenUsage(10, 5, 15);
        when(mockModel.generate(any(ChatMessage.class))).thenReturn(
                Response.from(new AiMessage("ok"), usage, null));

        AtomicReference<String> reportedModel = new AtomicReference<>();
        LlmUsageDispatch.setListener((providerName, modelId, tokenUsage) -> reportedModel.set(modelId));

        provider.generate("test", null, 0.7);

        // Usage must be attributed to the resolved (default) model, not the null input.
        assertEquals("test-default", reportedModel.get());
    }

    @Test
    void shouldClampTemperature() {
        when(mockModel.generate(any(ChatMessage.class))).thenReturn(
                Response.from(new AiMessage("ok")));

        provider.generate("test", "model", 5.0);

        assertEquals(2.0, provider.getLastTemperature());
    }

    @Test
    void shouldCacheModelInstances() {
        when(mockModel.generate(any(ChatMessage.class))).thenReturn(
                Response.from(new AiMessage("ok")));

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
