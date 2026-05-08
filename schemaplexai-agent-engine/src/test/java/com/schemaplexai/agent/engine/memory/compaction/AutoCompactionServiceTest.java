package com.schemaplexai.agent.engine.memory.compaction;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AutoCompactionService")
class AutoCompactionServiceTest {

    private AutoCompactionService service;
    private CompositeChatMemoryStore chatMemoryStore;
    private ToolResultCompactionStrategy toolResultStrategy;
    private SlidingWindowCompactionStrategy slidingWindowStrategy;
    private SummarizationCompactionStrategy summarizationStrategy;

    @BeforeEach
    void setUp() {
        chatMemoryStore = mock(CompositeChatMemoryStore.class);
        toolResultStrategy = mock(ToolResultCompactionStrategy.class);
        slidingWindowStrategy = mock(SlidingWindowCompactionStrategy.class);
        summarizationStrategy = mock(SummarizationCompactionStrategy.class);
        service = new AutoCompactionService(chatMemoryStore, toolResultStrategy, slidingWindowStrategy, summarizationStrategy);
    }

    @Test
    @DisplayName("should return noop when messages fit within budget")
    void testWithinBudgetReturnsNoop() {
        String conversationId = "conv-1";
        TokenBudget budget = new TokenBudget(100, 100);
        // Each message ~2 tokens (11 chars / 4), 3 messages = ~6 tokens, well under 100
        List<LlmMessage> messages = List.of(
            new LlmMessage("user", "message_one"),
            new LlmMessage("assistant", "message_two"),
            new LlmMessage("user", "message_three")
        );
        when(chatMemoryStore.loadMessages(conversationId)).thenReturn(messages);

        CompactionResult result = service.compactIfNeeded(conversationId, budget);

        assertThat(result.success()).isTrue();
        assertThat(result.noOp()).isTrue();
        verify(toolResultStrategy, never()).compact(any(), any(), any());
        verify(slidingWindowStrategy, never()).compact(any(), any(), any());
        verify(summarizationStrategy, never()).compact(any(), any(), any());
        verify(chatMemoryStore, never()).replaceMessages(any(), any());
    }

    @Test
    @DisplayName("should apply Layer 0 when it makes messages fit budget")
    void testLayer0Success() {
        String conversationId = "conv-1";
        TokenBudget budget = new TokenBudget(5, 100);
        // 5 messages with 40-char content = 50 tokens total (40/4=10 each), over budget of 5
        List<LlmMessage> originalMessages = buildMessages(5, 40, "orig");
        // 2 messages with 8-char content = 4 tokens total, fits budget of 5
        List<LlmMessage> compactedMessages = buildMessages(2, 8, "compact");
        when(chatMemoryStore.loadMessages(conversationId)).thenReturn(originalMessages);
        when(toolResultStrategy.compact(eq(conversationId), anyList(), eq(budget)))
            .thenReturn(CompactionResult.success(compactedMessages, "tool_result_cleanup"));

        CompactionResult result = service.compactIfNeeded(conversationId, budget);

        assertThat(result.success()).isTrue();
        assertThat(result.noOp()).isFalse();
        assertThat(result.strategyName()).isEqualTo("tool_result_cleanup");
        verify(chatMemoryStore).replaceMessages(conversationId, compactedMessages);
        verify(slidingWindowStrategy, never()).compact(any(), any(), any());
        verify(summarizationStrategy, never()).compact(any(), any(), any());
    }

    @Test
    @DisplayName("should apply Layer 1 when Layer 0 is noop")
    void testLayer1WhenLayer0NoOp() {
        String conversationId = "conv-1";
        TokenBudget budget = new TokenBudget(5, 100);
        List<LlmMessage> originalMessages = buildMessages(5, 40, "orig");
        List<LlmMessage> compactedMessages = buildMessages(2, 8, "compact");
        when(chatMemoryStore.loadMessages(conversationId)).thenReturn(originalMessages);
        when(toolResultStrategy.compact(eq(conversationId), anyList(), eq(budget)))
            .thenReturn(CompactionResult.empty());
        when(slidingWindowStrategy.compact(eq(conversationId), anyList(), eq(budget)))
            .thenReturn(CompactionResult.success(compactedMessages, "sliding_window"));

        CompactionResult result = service.compactIfNeeded(conversationId, budget);

        assertThat(result.success()).isTrue();
        assertThat(result.noOp()).isFalse();
        assertThat(result.strategyName()).isEqualTo("sliding_window");
        verify(chatMemoryStore).replaceMessages(conversationId, compactedMessages);
        verify(summarizationStrategy, never()).compact(any(), any(), any());
    }

    @Test
    @DisplayName("should apply Layer 2 when Layer 0 and 1 are noop")
    void testLayer2WhenLayer1NoOp() {
        String conversationId = "conv-1";
        TokenBudget budget = new TokenBudget(5, 100);
        List<LlmMessage> originalMessages = buildMessages(5, 40, "orig");
        List<LlmMessage> compactedMessages = buildMessages(2, 8, "compact");
        when(chatMemoryStore.loadMessages(conversationId)).thenReturn(originalMessages);
        when(toolResultStrategy.compact(eq(conversationId), anyList(), eq(budget)))
            .thenReturn(CompactionResult.empty());
        when(slidingWindowStrategy.compact(eq(conversationId), anyList(), eq(budget)))
            .thenReturn(CompactionResult.empty());
        when(summarizationStrategy.compact(eq(conversationId), anyList(), eq(budget)))
            .thenReturn(CompactionResult.success(compactedMessages, "summarization_with_restoration"));

        CompactionResult result = service.compactIfNeeded(conversationId, budget);

        assertThat(result.success()).isTrue();
        assertThat(result.noOp()).isFalse();
        assertThat(result.strategyName()).isEqualTo("summarization_with_restoration");
        verify(chatMemoryStore).replaceMessages(conversationId, compactedMessages);
    }

    @Test
    @DisplayName("should retry Layer 2 with truncated messages when first attempt is over budget")
    void testPTLRetrySuccessOnSecondAttempt() {
        String conversationId = "conv-1";
        TokenBudget budget = new TokenBudget(3, 100);
        // 4 messages with 40-char content = 40 tokens total, over budget of 3
        List<LlmMessage> originalMessages = buildMessages(4, 40, "orig");
        // First summarization returns 2 messages with 20-char content = 10 tokens, still over budget of 3
        List<LlmMessage> firstAttemptMessages = buildMessages(2, 20, "first");
        // Second summarization returns 1 message with 8-char content = 2 tokens, fits budget of 3
        List<LlmMessage> secondAttemptMessages = buildMessages(1, 8, "second");
        when(chatMemoryStore.loadMessages(conversationId)).thenReturn(originalMessages);
        when(toolResultStrategy.compact(eq(conversationId), anyList(), eq(budget)))
            .thenReturn(CompactionResult.empty());
        when(slidingWindowStrategy.compact(eq(conversationId), anyList(), eq(budget)))
            .thenReturn(CompactionResult.empty());
        when(summarizationStrategy.compact(eq(conversationId), anyList(), eq(budget)))
            .thenReturn(
                CompactionResult.success(firstAttemptMessages, "summarization_with_restoration"),
                CompactionResult.success(secondAttemptMessages, "summarization_with_restoration")
            );

        CompactionResult result = service.compactIfNeeded(conversationId, budget);

        assertThat(result.success()).isTrue();
        assertThat(result.noOp()).isFalse();
        assertThat(result.strategyName()).isEqualTo("summarization_with_restoration");
        verify(summarizationStrategy, times(2)).compact(eq(conversationId), anyList(), eq(budget));
        verify(chatMemoryStore).replaceMessages(conversationId, secondAttemptMessages);
    }

    @Test
    @DisplayName("should return failed result when all strategies are exhausted")
    void testAllStrategiesExhausted() {
        String conversationId = "conv-1";
        TokenBudget budget = new TokenBudget(1, 100);
        // 4 messages with 40-char content = 40 tokens, way over budget of 1
        List<LlmMessage> originalMessages = buildMessages(4, 40, "orig");
        when(chatMemoryStore.loadMessages(conversationId)).thenReturn(originalMessages);
        when(toolResultStrategy.compact(eq(conversationId), anyList(), eq(budget)))
            .thenReturn(CompactionResult.empty());
        when(slidingWindowStrategy.compact(eq(conversationId), anyList(), eq(budget)))
            .thenReturn(CompactionResult.empty());
        // Summarization always returns messages that are still over budget (or fails)
        when(summarizationStrategy.compact(eq(conversationId), anyList(), eq(budget)))
            .thenReturn(CompactionResult.success(buildMessages(2, 20, "still_over"), "summarization_with_restoration"));

        CompactionResult result = service.compactIfNeeded(conversationId, budget);

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo("All compaction strategies exhausted");
        verify(chatMemoryStore, never()).replaceMessages(any(), any());
    }

    @Test
    @DisplayName("should call replaceMessages exactly once when compaction succeeds")
    void testReplaceMessagesOnlyOnSuccess() {
        String conversationId = "conv-1";
        TokenBudget budget = new TokenBudget(5, 100);
        List<LlmMessage> originalMessages = buildMessages(5, 40, "orig");
        List<LlmMessage> compactedMessages = buildMessages(2, 8, "compact");
        when(chatMemoryStore.loadMessages(conversationId)).thenReturn(originalMessages);
        when(toolResultStrategy.compact(eq(conversationId), anyList(), eq(budget)))
            .thenReturn(CompactionResult.empty());
        when(slidingWindowStrategy.compact(eq(conversationId), anyList(), eq(budget)))
            .thenReturn(CompactionResult.success(compactedMessages, "sliding_window"));

        service.compactIfNeeded(conversationId, budget);

        verify(chatMemoryStore, times(1)).replaceMessages(conversationId, compactedMessages);
    }

    private List<LlmMessage> buildMessages(int count, int contentLength, String prefix) {
        List<LlmMessage> messages = new ArrayList<>(count);
        String content = prefix.repeat(Math.max(1, contentLength / prefix.length() + 1));
        content = content.substring(0, contentLength);
        for (int i = 0; i < count; i++) {
            messages.add(new LlmMessage("user", content));
        }
        return messages;
    }
}
