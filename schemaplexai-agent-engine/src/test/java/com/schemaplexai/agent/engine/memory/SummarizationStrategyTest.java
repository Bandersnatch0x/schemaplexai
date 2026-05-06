package com.schemaplexai.agent.engine.memory;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SummarizationStrategy")
class SummarizationStrategyTest {

    private SummarizationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SummarizationStrategy();
    }

    @Nested
    @DisplayName("select")
    class SelectTests {

        @Test
        @DisplayName("should return empty list for null messages")
        void shouldReturnEmptyForNull() {
            TokenBudget budget = new TokenBudget(1000, 1000);
            List<ChatMessage> result = strategy.select(null, budget);

            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("should return empty list for empty messages")
        void shouldReturnEmptyForEmpty() {
            TokenBudget budget = new TokenBudget(1000, 1000);
            List<ChatMessage> result = strategy.select(List.of(), budget);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should preserve system messages")
        void shouldPreserveSystemMessages() {
            TokenBudget budget = new TokenBudget(500, 500);
            List<ChatMessage> messages = List.of(
                    ChatMessage.system("You are a helpful assistant."),
                    ChatMessage.user("Hello"),
                    ChatMessage.assistant("Hi!")
            );

            List<ChatMessage> selected = strategy.select(messages, budget);
            assertThat(selected.stream().anyMatch(m -> "system".equals(m.getRole()))).isTrue();
        }

        @Test
        @DisplayName("should preserve multiple system messages")
        void shouldPreserveMultipleSystemMessages() {
            TokenBudget budget = new TokenBudget(500, 500);
            List<ChatMessage> messages = List.of(
                    ChatMessage.system("System prompt 1"),
                    ChatMessage.system("System prompt 2"),
                    ChatMessage.user("Hello")
            );

            List<ChatMessage> selected = strategy.select(messages, budget);
            long systemCount = selected.stream().filter(m -> "system".equals(m.getRole())).count();
            assertThat(systemCount).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("should create summary placeholder when dropping messages")
        void shouldCreateSummaryPlaceholder() {
            TokenBudget budget = new TokenBudget(100, 100);
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.system("System prompt"));
            for (int i = 0; i < 20; i++) {
                messages.add(ChatMessage.user("User message number " + i + " with some content"));
                messages.add(ChatMessage.assistant("Assistant response number " + i + " with details"));
            }

            List<ChatMessage> selected = strategy.select(messages, budget);
            boolean hasSummary = selected.stream()
                    .anyMatch(m -> m.getContent() != null && m.getContent().contains("[Conversation summary"));
            assertThat(hasSummary).isTrue();
        }

        @Test
        @DisplayName("should not create summary when all messages fit")
        void shouldNotCreateSummaryWhenAllFit() {
            TokenBudget budget = new TokenBudget(10000, 10000);
            List<ChatMessage> messages = List.of(
                    ChatMessage.system("System"),
                    ChatMessage.user("Hello"),
                    ChatMessage.assistant("Hi!")
            );

            List<ChatMessage> selected = strategy.select(messages, budget);
            boolean hasSummary = selected.stream()
                    .anyMatch(m -> m.getContent() != null && m.getContent().contains("[Conversation summary"));
            assertThat(hasSummary).isFalse();
        }

        @Test
        @DisplayName("should keep most recent conversation messages")
        void shouldKeepMostRecentMessages() {
            TokenBudget budget = new TokenBudget(200, 200);
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.system("System"));
            for (int i = 0; i < 10; i++) {
                messages.add(ChatMessage.user("msg " + i));
            }

            List<ChatMessage> selected = strategy.select(messages, budget);
            // Should have system + some recent messages (possibly + summary)
            assertThat(selected).isNotEmpty();
        }

        @Test
        @DisplayName("should handle zero budget gracefully")
        void shouldHandleZeroBudget() {
            TokenBudget budget = new TokenBudget(0, 0);
            List<ChatMessage> messages = List.of(
                    ChatMessage.system("System"),
                    ChatMessage.user("Hello")
            );

            List<ChatMessage> selected = strategy.select(messages, budget);
            // System messages still included even with zero budget
            assertThat(selected.stream().anyMatch(m -> "system".equals(m.getRole()))).isTrue();
        }

        @Test
        @DisplayName("should handle messages with explicit token counts")
        void shouldHandleExplicitTokenCounts() {
            TokenBudget budget = new TokenBudget(500, 500);
            List<ChatMessage> messages = List.of(
                    ChatMessage.system("System"),
                    new ChatMessage("user", "small", 10L),
                    new ChatMessage("assistant", "medium", 20L),
                    new ChatMessage("user", "big", 200L)
            );

            List<ChatMessage> selected = strategy.select(messages, budget);
            assertThat(selected).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("compress")
    class CompressTests {

        @Test
        @DisplayName("should return empty summary for null messages")
        void shouldReturnEmptyForNull() {
            CompressedMemory compressed = strategy.compress(null);

            assertThat(compressed).isNotNull();
            assertThat(compressed.hasSummary()).isFalse();
            assertThat(compressed.getOriginalMessageCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return empty summary for empty messages")
        void shouldReturnEmptyForEmpty() {
            CompressedMemory compressed = strategy.compress(List.of());

            assertThat(compressed.hasSummary()).isFalse();
            assertThat(compressed.getOriginalMessageCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should produce readable summary with roles")
        void shouldProduceReadableSummary() {
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("What is Spring Boot?"),
                    ChatMessage.assistant("Spring Boot is a Java framework.")
            );

            CompressedMemory compressed = strategy.compress(messages);
            assertThat(compressed.hasSummary()).isTrue();
            assertThat(compressed.getSummary()).contains("Spring Boot");
            assertThat(compressed.getSummary()).contains("[user]");
            assertThat(compressed.getSummary()).contains("[assistant]");
        }

        @Test
        @DisplayName("should truncate long content at 120 characters")
        void shouldTruncateLongContent() {
            String longContent = "B".repeat(200);
            List<ChatMessage> messages = List.of(ChatMessage.user(longContent));

            CompressedMemory compressed = strategy.compress(messages);
            assertThat(compressed.getSummary().length()).isLessThan(longContent.length() + 20);
            assertThat(compressed.getSummary()).contains("...");
        }

        @Test
        @DisplayName("should not truncate short content")
        void shouldNotTruncateShortContent() {
            String shortContent = "Short message here";
            List<ChatMessage> messages = List.of(ChatMessage.user(shortContent));

            CompressedMemory compressed = strategy.compress(messages);
            assertThat(compressed.getSummary()).contains(shortContent);
            assertThat(compressed.getSummary()).doesNotContain("...");
        }

        @Test
        @DisplayName("should calculate total token count")
        void shouldCalculateTokenCount() {
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("Hello"),
                    ChatMessage.assistant("World")
            );

            CompressedMemory compressed = strategy.compress(messages);
            assertThat(compressed.getCompressedTokenCount()).isGreaterThan(0);
            assertThat(compressed.getOriginalMessageCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle null content")
        void shouldHandleNullContent() {
            List<ChatMessage> messages = List.of(new ChatMessage("user", null));

            CompressedMemory compressed = strategy.compress(messages);
            assertThat(compressed.hasSummary()).isTrue();
            assertThat(compressed.getSummary()).contains("[user]");
        }

        @Test
        @DisplayName("should use explicit token count when available")
        void shouldUseExplicitTokenCount() {
            List<ChatMessage> messages = List.of(
                    new ChatMessage("user", "content", 555L)
            );

            CompressedMemory compressed = strategy.compress(messages);
            assertThat(compressed.getCompressedTokenCount()).isEqualTo(555L);
        }

        @Test
        @DisplayName("should separate entries with semicolons")
        void shouldSeparateWithSemicolons() {
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("First"),
                    ChatMessage.assistant("Second")
            );

            CompressedMemory compressed = strategy.compress(messages);
            assertThat(compressed.getSummary()).contains(";");
        }
    }

    @Test
    @DisplayName("getName() should return 'Summarization'")
    void getNameShouldReturnSummarization() {
        assertThat(strategy.getName()).isEqualTo("Summarization");
    }
}
