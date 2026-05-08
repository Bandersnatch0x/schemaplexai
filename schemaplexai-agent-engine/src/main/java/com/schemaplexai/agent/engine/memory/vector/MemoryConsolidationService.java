package com.schemaplexai.agent.engine.memory.vector;

import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service that consolidates recent conversation messages into long-term memory fragments.
 * Summarizes conversation history and stores it in the vector memory store
 * for future retrieval by the agent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryConsolidationService {

    private static final int DEFAULT_RECENT_COUNT = 20;

    private final VectorMemoryStore vectorMemoryStore;
    private final CompositeChatMemoryStore chatMemoryStore;

    /**
     * Consolidate recent conversation messages into a single memory fragment.
     * Takes the last N messages from chat memory, builds a summary, and stores it
     * as a memory fragment with importance scoring based on message role.
     *
     * @param agentId        the agent whose conversation to consolidate
     * @param tenantId       the tenant context
     * @param conversationId the conversation to consolidate
     */
    public void consolidate(String agentId, String tenantId, String conversationId) {
        if (agentId == null || agentId.isBlank()) {
            log.warn("Cannot consolidate: agentId is blank");
            return;
        }
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Cannot consolidate: tenantId is blank");
            return;
        }
        if (conversationId == null || conversationId.isBlank()) {
            log.warn("Cannot consolidate: conversationId is blank");
            return;
        }

        List<LlmMessage> messages = chatMemoryStore.loadMessages(conversationId);
        if (messages == null || messages.isEmpty()) {
            log.debug("No messages to consolidate for conversation={}", conversationId);
            return;
        }

        // Take the last N messages
        List<LlmMessage> recentMessages = messages.size() <= DEFAULT_RECENT_COUNT
                ? messages
                : messages.subList(messages.size() - DEFAULT_RECENT_COUNT, messages.size());

        double importance = calculateImportance(recentMessages);
        String summary = buildSummary(recentMessages);

        if (summary.isBlank()) {
            log.debug("Empty summary produced for conversation={}, skipping", conversationId);
            return;
        }

        MemoryFragment fragment = new MemoryFragment(
                UUID.randomUUID().toString(),
                agentId,
                tenantId,
                summary,
                "consolidation",
                importance,
                Instant.now(),
                Map.of("conversationId", conversationId, "messageCount", recentMessages.size())
        );

        vectorMemoryStore.store(fragment);
        log.info("Consolidated {} messages into memory fragment {} for agent={}, tenant={}",
                recentMessages.size(), fragment.id(), agentId, tenantId);
    }

    /**
     * Calculate importance score based on message composition.
     * Tool results and assistant reasoning are weighted higher than user messages.
     *
     * @param messages the messages to score
     * @return importance score between 0.0 and 1.0
     */
    double calculateImportance(List<LlmMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0.0;
        }

        int toolCount = 0;
        int assistantCount = 0;
        int userCount = 0;

        for (LlmMessage msg : messages) {
            if (msg.getRole() == null) {
                continue;
            }
            switch (msg.getRole()) {
                case "tool":
                case "function":
                    toolCount++;
                    break;
                case "assistant":
                    assistantCount++;
                    break;
                case "user":
                    userCount++;
                    break;
                default:
                    break;
            }
        }

        int total = toolCount + assistantCount + userCount;
        if (total == 0) {
            return 0.5;
        }

        // Weighted scoring: tool results > assistant reasoning > user messages
        double weightedScore = (toolCount * 1.0 + assistantCount * 0.7 + userCount * 0.4) / total;
        return Math.min(1.0, Math.max(0.0, weightedScore));
    }

    /**
     * Build a summary string from a list of messages.
     * Concatenates role-labeled messages into a readable summary.
     */
    String buildSummary(List<LlmMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Conversation summary (").append(messages.size()).append(" messages):\n");

        for (LlmMessage msg : messages) {
            String role = msg.getRole() != null ? msg.getRole() : "unknown";
            String content = msg.getContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            // Truncate very long messages to keep summaries manageable
            if (content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }
            sb.append("[").append(role).append("]: ").append(content).append("\n");
        }

        return sb.toString().stripTrailing();
    }
}
