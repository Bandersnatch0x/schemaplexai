package com.schemaplexai.agent.engine.memory;

import com.schemaplexai.agent.engine.admission.TokenBudget;

import java.util.ArrayList;
import java.util.List;

/**
 * Summarization memory strategy.
 * Keeps the system message and the most recent messages,
 * and summarizes older messages into a single compressed entry
 * to preserve long-range context within the token budget.
 */
public class SummarizationStrategy implements MemoryStrategy {

    private static final int MIN_RECENT_MESSAGES = 2;
    private static final long SUMMARY_TOKEN_COST = 200;

    @Override
    public List<ChatMessage> select(List<ChatMessage> messages, TokenBudget budget) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        long remaining = budget.remainingInput();

        // Separate system messages from conversation messages
        List<ChatMessage> systemMessages = new ArrayList<>();
        List<ChatMessage> conversationMessages = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if ("system".equals(msg.getRole())) {
                systemMessages.add(msg);
            } else {
                conversationMessages.add(msg);
            }
        }

        // Reserve budget for system messages
        for (ChatMessage sys : systemMessages) {
            remaining -= estimateTokens(sys);
        }

        // Reserve budget for summary placeholder
        remaining -= SUMMARY_TOKEN_COST;

        // Keep the most recent messages that fit
        List<ChatMessage> recentMessages = new ArrayList<>();
        for (int i = conversationMessages.size() - 1; i >= 0; i--) {
            ChatMessage msg = conversationMessages.get(i);
            long cost = estimateTokens(msg);
            if (cost <= remaining && recentMessages.size() < conversationMessages.size()) {
                recentMessages.add(0, msg);
                remaining -= cost;
            } else {
                break;
            }
        }

        // If we dropped messages, add a summary placeholder
        int droppedCount = conversationMessages.size() - recentMessages.size();
        List<ChatMessage> result = new ArrayList<>(systemMessages);
        if (droppedCount > 0) {
            CompressedMemory compressed = compress(
                    conversationMessages.subList(0, droppedCount)
            );
            result.add(ChatMessage.system(
                    "[Conversation summary of " + droppedCount + " messages]: " + compressed.getSummary()
            ));
        }
        result.addAll(recentMessages);

        return result;
    }

    @Override
    public CompressedMemory compress(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return CompressedMemory.of("", 0, 0, List.of());
        }

        StringBuilder summary = new StringBuilder();
        long totalTokens = 0;

        for (ChatMessage msg : messages) {
            totalTokens += estimateTokens(msg);
            summary.append("[").append(msg.getRole()).append("] ");
            String content = msg.getContent();
            if (content != null && content.length() > 120) {
                summary.append(content, 0, 120).append("...");
            } else {
                summary.append(content);
            }
            summary.append("; ");
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
        return "Summarization";
    }

    private long estimateTokens(ChatMessage message) {
        if (message.getTokenCount() != null) {
            return message.getTokenCount();
        }
        if (message.getContent() == null) {
            return 0;
        }
        return Math.max(1, message.getContent().length() / 4);
    }
}
