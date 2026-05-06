package com.schemaplexai.agent.engine.memory;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SlidingWindowStrategy")
class SlidingWindowStrategyTest {

    private SlidingWindowStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SlidingWindowStrategy(25);
    }

    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should create with custom tokens per message")
        void shouldCreateWithCustomTokens() {
            SlidingWindowStrategy custom = new SlidingWindowStrategy(100);
            assertThat(custom.getName()).isEqualTo("SlidingWindow");
        }

        @Test
        @DisplayName("should create with default tokens per message")
        void shouldCreateWithDefaultTokens() {
            SlidingWindowStrategy defaultStrategy = new SlidingWindowStrategy();
            assertThat(defaultStrategy.getName()).isEqualTo("SlidingWindow");
        }
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
        @DisplayName("should return all messages when budget is sufficient")
        void shouldReturnAllWhenBudgetSufficient() {
            TokenBudget budget = new TokenBudget(10000, 10000);
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("Hello"),
                    ChatMessage.assistant("Hi there"),
                    ChatMessage.user("How are you?")
            );

            List<ChatMessage> selected = strategy.select(messages, budget);
            assertThat(selected).hasSize(3);
        }

        @Test
        @DisplayName("should drop oldest messages when budget is tight")
        void shouldDropOldestWhenBudgetTight() {
            TokenBudget budget = new TokenBudget(15, 15);
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("First message that is quite long and costs tokens"),
                    ChatMessage.assistant("Second message that is also fairly long"),
                    ChatMessage.user("Third and final question")
            );

            List<ChatMessage> selected = strategy.select(messages, budget);
            assertThat(selected).hasSizeLessThan(messages.size());
            assertThat(selected.get(selected.size() - 1).getContent()).isEqualTo("Third and final question");
        }

        @Test
        @DisplayName("should keep most recent messages in order")
        void shouldKeepMostRecentInOrder() {
            TokenBudget budget = new TokenBudget(200, 200);
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("msg1"),
                    ChatMessage.assistant("msg2"),
                    ChatMessage.user("msg3"),
                    ChatMessage.assistant("msg4")
            );

            List<ChatMessage> selected = strategy.select(messages, budget);
            assertThat(selected).hasSize(4);
            assertThat(selected.get(0).getContent()).isEqualTo("msg1");
            assertThat(selected.get(3).getContent()).isEqualTo("msg4");
        }

        @Test
        @DisplayName("should respect explicit token count on messages")
        void shouldRespectExplicitTokenCount() {
            TokenBudget budget = new TokenBudget(100, 100);
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("user", "small", 10L));
            messages.add(new ChatMessage("assistant", "medium", 40L));
            messages.add(new ChatMessage("user", "big", 80L));

            List<ChatMessage> selected = strategy.select(messages, budget);
            assertThat(selected.size()).isBetween(1, 2);
        }

        @Test
        @DisplayName("should return only most recent when budget fits exactly one message")
        void shouldReturnOneWhenBudgetFitsExactlyOne() {
            TokenBudget budget = new TokenBudget(5, 5);
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("first long message content here"),
                    ChatMessage.assistant("second long message content here"),
                    ChatMessage.user("third")
            );

            List<ChatMessage> selected = strategy.select(messages, budget);
            assertThat(selected).hasSize(1);
            assertThat(selected.get(0).getContent()).isEqualTo("third");
        }

        @Test
        @DisplayName("should handle messages with null content")
        void shouldHandleNullContent() {
            TokenBudget budget = new TokenBudget(100, 100);
            List<ChatMessage> messages = List.of(
                    new ChatMessage("user", null),
                    ChatMessage.assistant("valid")
            );

            List<ChatMessage> selected = strategy.select(messages, budget);
            assertThat(selected).isNotEmpty();
        }

        @Test
        @DisplayName("should stop when first message exceeds budget")
        void shouldStopWhenFirstExceedsBudget() {
            TokenBudget budget = new TokenBudget(1, 1);
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("way too long for this tiny budget")
            );

            List<ChatMessage> selected = strategy.select(messages, budget);
            assertThat(selected).isEmpty();
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
            assertThat(compressed.getCompressedTokenCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return empty summary for empty messages")
        void shouldReturnEmptyForEmpty() {
            CompressedMemory compressed = strategy.compress(List.of());

            assertThat(compressed.hasSummary()).isFalse();
            assertThat(compressed.getOriginalMessageCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should summarize messages with roles")
        void shouldSummarizeWithRoles() {
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("What is Java?"),
                    ChatMessage.assistant("Java is a programming language.")
            );

            CompressedMemory compressed = strategy.compress(messages);
            assertThat(compressed.hasSummary()).isTrue();
            assertThat(compressed.getOriginalMessageCount()).isEqualTo(2);
            assertThat(compressed.getSummary()).contains("[user]");
            assertThat(compressed.getSummary()).contains("[assistant]");
        }

        @Test
        @DisplayName("should truncate long content at 100 characters")
        void shouldTruncateLongContent() {
            String longContent = "A".repeat(200);
            List<ChatMessage> messages = List.of(ChatMessage.user(longContent));

            CompressedMemory compressed = strategy.compress(messages);
            assertThat(compressed.getSummary().length()).isLessThan(longContent.length() + 20);
            assertThat(compressed.getSummary()).contains("...");
        }

        @Test
        @DisplayName("should not truncate short content")
        void shouldNotTruncateShortContent() {
            String shortContent = "Short message";
            List<ChatMessage> messages = List.of(ChatMessage.user(shortContent));

            CompressedMemory compressed = strategy.compress(messages);
            assertThat(compressed.getSummary()).contains(shortContent);
            assertThat(compressed.getSummary()).doesNotContain("...");
        }

        @Test
        @DisplayName("should calculate compressed token count")
        void shouldCalculateTokenCount() {
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("Hello world"),
                    ChatMessage.assistant("Hi!")
            );

            CompressedMemory compressed = strategy.compress(messages);
            assertThat(compressed.getCompressedTokenCount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should handle null content in message")
        void shouldHandleNullContent() {
            List<ChatMessage> messages = List.of(new ChatMessage("user", null));

            CompressedMemory compressed = strategy.compress(messages);
            assertThat(compressed.hasSummary()).isTrue();
            assertThat(compressed.getSummary()).contains("[user]");
            assertThat(compressed.getSummary()).contains("null");
        }

        @Test
        @DisplayName("should use explicit token count when available")
        void shouldUseExplicitTokenCount() {
            List<ChatMessage> messages = List.of(
                    new ChatMessage("user", "content", 999L)
            );

            CompressedMemory compressed = strategy.compress(messages);
            assertThat(compressed.getCompressedTokenCount()).isEqualTo(999L);
        }
    }

    @Test
    @DisplayName("getName() should return 'SlidingWindow'")
    void getNameShouldReturnSlidingWindow() {
        assertThat(strategy.getName()).isEqualTo("SlidingWindow");
    }
}
