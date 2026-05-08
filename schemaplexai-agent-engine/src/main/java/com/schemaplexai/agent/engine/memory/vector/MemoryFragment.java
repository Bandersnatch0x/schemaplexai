package com.schemaplexai.agent.engine.memory.vector;

import java.time.Instant;
import java.util.Map;

/**
 * A single stored memory fragment with metadata for long-term vector memory.
 *
 * @param id         unique identifier for this fragment
 * @param agentId    the agent this memory belongs to
 * @param tenantId   the tenant this memory belongs to
 * @param content    the memory text content
 * @param source     origin of the memory (e.g. "conversation", "consolidation", "manual")
 * @param importance importance score from 0.0 to 1.0
 * @param createdAt  when this memory was created
 * @param metadata   additional key-value metadata
 */
public record MemoryFragment(
        String id,
        String agentId,
        String tenantId,
        String content,
        String source,
        double importance,
        Instant createdAt,
        Map<String, Object> metadata
) {
}
