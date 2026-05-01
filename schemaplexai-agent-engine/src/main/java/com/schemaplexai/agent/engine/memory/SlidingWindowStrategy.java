package com.schemaplexai.agent.engine.memory;

import com.schemaplexai.agent.engine.admission.TokenBudget;

import java.util.ArrayList;
import java.util.List;

/**
 * Sliding window memory strategy.
 * Retains the most recent messages up to the token budget limit,
 * dropping older messages when the budget is exceeded.
 */
public class SlidingWindowStrategy implements MemoryStrategy {

    private static final long DEFAULT_TOKENS_PER_MESSAGE = 50;
    private final long tokensPerMessage;

    public SlidingWindowStrategy(long tokensPerMessage) {
        this.tokensPerMessage = tokensPerMessage;
    }

    public SlidingWindowStrategy() {
        this(DEFAULT_TOKENS_PER_MESSAGE);
    }

    @Override
    public List<ChatMessage> select(List<ChatMessage> messages, TokenBudget budget) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        long remaining = budget.remainingInput();
        List<ChatMessage> selected = new ArrayList<>();

        // Walk from the most recent backwards
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            long cost = estimateTokens(msg);
            if (cost <= remaining) {
                selected.add(0, msg);
                remaining -= cost;
            } else {
                break;
            }
        }

        return selected;
    }

    @Override
    public CompressedMemory compress(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return CompressedMemory.of("", 0, 0, List.of());
        }

        // Build a lightweight summary from dropped messages
        StringBuilder summary = new StringBuilder();
        long totalTokens = 0;
        for (ChatMessage msg : messages) {
            totalTokens += estimateTokens(msg);
            summary.append("[").append(msg.getRole()).append("] ");
            String content = msg.getContent();
            if (content != null && content.length() > 100) {
                summary.append(content, 0, 100).append("...");
            } else {
                summary.append(content);
            }
            summary.append("\n");
        }

        return CompressedMemory.of(
                summary.toString().trim(),
                messages.size(),
                totalTokens,
                List.of()
        );
    }

    @Override
    public String getName() {
        return "SlidingWindow";
    }

    private long estimateTokens(ChatMessage message) {
        if (message.getTokenCount() != null) {
            return message.getTokenCount();
        }
        if (message.getContent() == null) {
            return 0;
        }
        // Rough estimate: 1 token per 4 characters
        return Math.max(1, message.getContent().length() / 4);
    }
}
