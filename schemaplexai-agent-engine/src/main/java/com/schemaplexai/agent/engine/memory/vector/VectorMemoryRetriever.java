package com.schemaplexai.agent.engine.memory.vector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Retrieves long-term vector memories and formats them as context strings
 * suitable for injection into LLM prompts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorMemoryRetriever {

    private static final int DEFAULT_MAX_RESULTS = 5;
    private static final int CHARS_PER_TOKEN = 4;

    private final VectorMemoryStore vectorMemoryStore;

    /**
     * Retrieve relevant memories and format them as a context string.
     * The context string can be injected into prompts by {@link com.schemaplexai.agent.engine.context.ContextInjector}.
     *
     * @param agentId   the agent to retrieve memories for
     * @param tenantId  the tenant context
     * @param query     the search query for semantic matching
     * @param maxTokens maximum tokens to include in the context (approximate)
     * @return formatted context string, or empty string if no memories found
     */
    public String retrieveContext(String agentId, String tenantId, String query, int maxTokens) {
        if (agentId == null || agentId.isBlank() || tenantId == null || tenantId.isBlank()) {
            log.debug("Missing agentId or tenantId, returning empty context");
            return "";
        }
        if (query == null || query.isBlank()) {
            log.debug("Blank query, returning empty context");
            return "";
        }
        if (maxTokens <= 0) {
            maxTokens = 1000;
        }

        int maxChars = maxTokens * CHARS_PER_TOKEN;

        List<MemoryFragment> fragments = vectorMemoryStore.retrieve(agentId, tenantId, query, DEFAULT_MAX_RESULTS);
        if (fragments == null || fragments.isEmpty()) {
            log.debug("No memories found for agent={}, tenant={}, query='{}'", agentId, tenantId, truncate(query, 50));
            return "";
        }

        return formatAsContext(fragments, maxChars);
    }

    /**
     * Format memory fragments as a context block for prompt injection.
     */
    String formatAsContext(List<MemoryFragment> fragments, int maxChars) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Long-term Memory\n\n");

        for (MemoryFragment fragment : fragments) {
            String entry = formatFragment(fragment);
            if (sb.length() + entry.length() > maxChars) {
                log.debug("Context truncation: reached maxChars={} with {} fragments", maxChars, fragments.size());
                break;
            }
            sb.append(entry);
        }

        String result = sb.toString().stripTrailing();
        if (result.equals("## Long-term Memory")) {
            return "";
        }
        return result;
    }

    private String formatFragment(MemoryFragment fragment) {
        StringBuilder sb = new StringBuilder();
        sb.append("- ");
        if (fragment.source() != null) {
            sb.append("[").append(fragment.source()).append("] ");
        }
        sb.append(fragment.content());

        if (fragment.importance() > 0) {
            sb.append(" (importance: ").append(String.format("%.1f", fragment.importance())).append(")");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
