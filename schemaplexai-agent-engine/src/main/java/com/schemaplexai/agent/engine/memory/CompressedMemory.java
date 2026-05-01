package com.schemaplexai.agent.engine.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Result of compressing a conversation history into a summary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompressedMemory {

    private String summary;
    private long originalMessageCount;
    private long compressedTokenCount;
    private Instant compressedAt;
    private List<ChatMessage> retainedMessages;

    public static CompressedMemory of(String summary, long originalCount,
                                       long compressedTokens, List<ChatMessage> retained) {
        return new CompressedMemory(summary, originalCount, compressedTokens, Instant.now(), retained);
    }

    public boolean hasSummary() {
        return summary != null && !summary.isBlank();
    }
}
