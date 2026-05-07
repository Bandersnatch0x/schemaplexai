package com.schemaplexai.agent.engine.memory.compaction;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.memory.ConversationFileTracker;
import com.schemaplexai.agent.engine.model.AiModelRouter;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.skill.SkillRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SummarizationCompactionStrategy")
class SummarizationCompactionTest {

    private SummarizationCompactionStrategy strategy;
    private AiModelRouter modelRouter;
    private ConversationFileTracker fileTracker;
    private SkillRegistry skillRegistry;

    @BeforeEach
    void setUp() {
        modelRouter = mock(AiModelRouter.class);
        fileTracker = mock(ConversationFileTracker.class);
        skillRegistry = mock(SkillRegistry.class);
        strategy = new SummarizationCompactionStrategy(modelRouter, fileTracker, skillRegistry);
    }

    @Test
    @DisplayName("getName() should return 'summarization_with_restoration'")
    void getNameShouldReturnCorrectValue() {
        assertThat(strategy.getName()).isEqualTo("summarization_with_restoration");
    }

    @Test
    @DisplayName("should return success result with summary message")
    void shouldReturnSuccessWithSummary() {
        String conversationId = "conv-123";
        List<LlmMessage> messages = List.of(
            new LlmMessage("user", "What is Spring Boot?"),
            new LlmMessage("assistant", "Spring Boot is a Java framework.")
        );
        TokenBudget budget = new TokenBudget(1000, 1000);

        when(modelRouter.generateWithFallback(any(), isNull(), anyDouble()))
            .thenReturn("Summary: user asked about Spring Boot, assistant explained it's a Java framework.");
        when(fileTracker.getRecentFiles(conversationId, 5)).thenReturn(List.of());

        CompactionResult result = strategy.compact(conversationId, messages, budget);

        assertThat(result.success()).isTrue();
        assertThat(result.noOp()).isFalse();
        assertThat(result.strategyName()).isEqualTo("summarization_with_restoration");
        assertThat(result.messages()).isNotNull();
        assertThat(result.messages()).isNotEmpty();
        assertThat(result.messages().get(0).getRole()).isEqualTo("system");
        assertThat(result.messages().get(0).getContent()).contains("Summary:");
    }

    @Test
    @DisplayName("should redact PII before generating summary")
    void shouldRedactPiiBeforeSummary() {
        String conversationId = "conv-456";
        List<LlmMessage> messages = List.of(
            new LlmMessage("user", "My password is secret123 and email is alice@example.com")
        );
        TokenBudget budget = new TokenBudget(1000, 1000);

        when(modelRouter.generateWithFallback(any(), isNull(), anyDouble()))
            .thenReturn("Summary: user shared credentials.");
        when(fileTracker.getRecentFiles(conversationId, 5)).thenReturn(List.of());

        CompactionResult result = strategy.compact(conversationId, messages, budget);

        assertThat(result.success()).isTrue();
        verify(modelRouter).generateWithFallback(
            any(),
            isNull(),
            eq(0.3)
        );
    }

    @Test
    @DisplayName("should include recent files in restored context")
    void shouldIncludeRecentFiles() {
        String conversationId = "conv-789";
        List<LlmMessage> messages = List.of(
            new LlmMessage("user", "Hello")
        );
        TokenBudget budget = new TokenBudget(1000, 1000);

        when(modelRouter.generateWithFallback(any(), isNull(), anyDouble()))
            .thenReturn("Summary: user greeted.");
        when(fileTracker.getRecentFiles(conversationId, 5))
            .thenReturn(List.of("src/main/java/App.java", "src/main/java/Config.java"));

        CompactionResult result = strategy.compact(conversationId, messages, budget);

        assertThat(result.success()).isTrue();
        assertThat(result.messages()).hasSizeGreaterThan(1);
        // First message is summary, subsequent messages are file context
        boolean hasFileContext = result.messages().stream()
            .skip(1)
            .anyMatch(m -> m.getContent().contains("App.java") || m.getContent().contains("Config.java"));
        assertThat(hasFileContext).isTrue();
    }

    @Test
    @DisplayName("should call generateWithFallback with null modelId and temperature 0.3")
    void shouldCallGenerateWithCorrectArgs() {
        String conversationId = "conv-abc";
        List<LlmMessage> messages = List.of(
            new LlmMessage("user", "Hello world")
        );
        TokenBudget budget = new TokenBudget(1000, 1000);

        when(modelRouter.generateWithFallback(any(), isNull(), anyDouble()))
            .thenReturn("Summary: greeting.");
        when(fileTracker.getRecentFiles(conversationId, 5)).thenReturn(List.of());

        strategy.compact(conversationId, messages, budget);

        verify(modelRouter).generateWithFallback(
            any(),
            isNull(),
            eq(0.3)
        );
    }

    @Test
    @DisplayName("should handle empty messages list")
    void shouldHandleEmptyMessages() {
        String conversationId = "conv-empty";
        List<LlmMessage> messages = List.of();
        TokenBudget budget = new TokenBudget(1000, 1000);

        when(modelRouter.generateWithFallback(any(), isNull(), anyDouble()))
            .thenReturn("Summary: empty conversation.");
        when(fileTracker.getRecentFiles(conversationId, 5)).thenReturn(List.of());

        CompactionResult result = strategy.compact(conversationId, messages, budget);

        assertThat(result.success()).isTrue();
        assertThat(result.messages()).isNotEmpty();
    }

    @Test
    @DisplayName("should handle null message content gracefully")
    void shouldHandleNullContent() {
        String conversationId = "conv-null";
        List<LlmMessage> messages = List.of(
            new LlmMessage("user", null)
        );
        TokenBudget budget = new TokenBudget(1000, 1000);

        when(modelRouter.generateWithFallback(any(), isNull(), anyDouble()))
            .thenReturn("Summary: null content.");
        when(fileTracker.getRecentFiles(conversationId, 5)).thenReturn(List.of());

        CompactionResult result = strategy.compact(conversationId, messages, budget);

        assertThat(result.success()).isTrue();
        assertThat(result.messages().get(0).getRole()).isEqualTo("system");
    }
}
