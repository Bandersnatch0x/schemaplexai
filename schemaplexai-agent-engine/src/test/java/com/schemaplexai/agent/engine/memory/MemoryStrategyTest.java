package com.schemaplexai.agent.engine.memory;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemoryStrategyTest {

    private SlidingWindowStrategy slidingWindow;

    @BeforeEach
    void setUp() {
        slidingWindow = new SlidingWindowStrategy(25);
    }

    // ── SlidingWindowStrategy.select() ──────────────────────────

    @Nested
    @DisplayName("SlidingWindowStrategy.select()")
    class SlidingWindowSelect {

        @Test
        @DisplayName("returns empty list when messages are null")
        void returnsEmptyWhenNull() {
            TokenBudget budget = new TokenBudget(1000, 1000);
            List<ChatMessage> result = slidingWindow.select(null, budget);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty list when messages are empty")
        void returnsEmptyWhenEmpty() {
            TokenBudget budget = new TokenBudget(1000, 1000);
            List<ChatMessage> result = slidingWindow.select(List.of(), budget);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns all messages when budget is sufficient")
        void returnsAllWhenBudgetSufficient() {
            TokenBudget budget = new TokenBudget(10000, 10000);
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("Hello"),
                    ChatMessage.assistant("Hi there"),
                    ChatMessage.user("How are you?")
            );

            List<ChatMessage> selected = slidingWindow.select(messages, budget);
            assertEquals(3, selected.size());
        }

        @Test
        @DisplayName("drops oldest messages when budget is tight")
        void dropsOldestWhenBudgetTight() {
            // Budget allows roughly 1 message (content-length/4 estimation)
            TokenBudget budget = new TokenBudget(15, 15);
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("First message that is quite long and costs tokens"),
                    ChatMessage.assistant("Second message that is also fairly long"),
                    ChatMessage.user("Third and final question")
            );

            List<ChatMessage> selected = slidingWindow.select(messages, budget);
            assertTrue(selected.size() < messages.size(),
                    "Should have dropped some messages due to budget");
            // Most recent should be kept
            assertEquals("Third and final question",
                    selected.get(selected.size() - 1).getContent());
        }

        @Test
        @DisplayName("keeps most recent messages in order")
        void keepsMostRecentInOrder() {
            TokenBudget budget = new TokenBudget(200, 200);
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("msg1"),
                    ChatMessage.assistant("msg2"),
                    ChatMessage.user("msg3"),
                    ChatMessage.assistant("msg4")
            );

            List<ChatMessage> selected = slidingWindow.select(messages, budget);
            assertEquals(4, selected.size());
            assertEquals("msg1", selected.get(0).getContent());
            assertEquals("msg4", selected.get(3).getContent());
        }

        @Test
        @DisplayName("respects explicit token count on messages")
        void respectsExplicitTokenCount() {
            TokenBudget budget = new TokenBudget(100, 100);
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("user", "small", 10L));
            messages.add(new ChatMessage("assistant", "medium", 40L));
            messages.add(new ChatMessage("user", "big", 80L));

            List<ChatMessage> selected = slidingWindow.select(messages, budget);
            // 80 + 40 = 120 > 100, so at most 2 messages (40 + 10 = 50) or just the last one
            assertTrue(selected.size() >= 1);
            assertTrue(selected.size() <= 2);
        }
    }

    // ── SlidingWindowStrategy.compress() ────────────────────────

    @Nested
    @DisplayName("SlidingWindowStrategy.compress()")
    class SlidingWindowCompress {

        @Test
        @DisplayName("returns empty summary for empty input")
        void returnsEmptyForEmptyInput() {
            CompressedMemory compressed = slidingWindow.compress(List.of());
            assertNotNull(compressed);
            assertFalse(compressed.hasSummary());
            assertEquals(0, compressed.getOriginalMessageCount());
        }

        @Test
        @DisplayName("summarizes messages with roles")
        void summarizesWithRoles() {
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("What is Java?"),
                    ChatMessage.assistant("Java is a programming language.")
            );

            CompressedMemory compressed = slidingWindow.compress(messages);
            assertTrue(compressed.hasSummary());
            assertEquals(2, compressed.getOriginalMessageCount());
            assertTrue(compressed.getSummary().contains("[user]"));
            assertTrue(compressed.getSummary().contains("[assistant]"));
        }

        @Test
        @DisplayName("truncates long content in summary")
        void truncatesLongContent() {
            String longContent = "A".repeat(200);
            List<ChatMessage> messages = List.of(
                    ChatMessage.user(longContent)
            );

            CompressedMemory compressed = slidingWindow.compress(messages);
            assertTrue(compressed.getSummary().length() < longContent.length(),
                    "Summary should be shorter than original");
            assertTrue(compressed.getSummary().contains("..."));
        }

        @Test
        @DisplayName("calculates compressed token count")
        void calculatesTokenCount() {
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("Hello world"),
                    ChatMessage.assistant("Hi!")
            );

            CompressedMemory compressed = slidingWindow.compress(messages);
            assertTrue(compressed.getCompressedTokenCount() > 0);
        }
    }

    // ── SlidingWindowStrategy metadata ──────────────────────────

    @Test
    @DisplayName("getName() returns 'SlidingWindow'")
    void nameIsCorrect() {
        assertEquals("SlidingWindow", slidingWindow.getName());
    }

    // ── SummarizationStrategy basics ────────────────────────────

    @Nested
    @DisplayName("SummarizationStrategy")
    class SummarizationTests {

        private SummarizationStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new SummarizationStrategy();
        }

        @Test
        @DisplayName("preserves system messages")
        void preservesSystemMessages() {
            TokenBudget budget = new TokenBudget(200, 200);
            List<ChatMessage> messages = List.of(
                    ChatMessage.system("You are a helpful assistant."),
                    ChatMessage.user("Hello"),
                    ChatMessage.assistant("Hi!")
            );

            List<ChatMessage> selected = strategy.select(messages, budget);
            assertTrue(selected.stream().anyMatch(m -> "system".equals(m.getRole())),
                    "System message should be preserved");
        }

        @Test
        @DisplayName("creates summary placeholder when dropping messages")
        void createsSummaryPlaceholder() {
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
            assertTrue(hasSummary, "Should have a summary placeholder for dropped messages");
        }

        @Test
        @DisplayName("getName() returns 'Summarization'")
        void nameIsCorrect() {
            assertEquals("Summarization", strategy.getName());
        }

        @Test
        @DisplayName("compress() produces readable summary")
        void compressProducesReadableSummary() {
            List<ChatMessage> messages = List.of(
                    ChatMessage.user("What is Spring Boot?"),
                    ChatMessage.assistant("Spring Boot is a Java framework.")
            );

            CompressedMemory compressed = strategy.compress(messages);
            assertTrue(compressed.hasSummary());
            assertTrue(compressed.getSummary().contains("Spring Boot"));
        }
    }

}
