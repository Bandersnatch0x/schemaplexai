package com.schemaplexai.agent.engine.memory.compaction;

import com.schemaplexai.agent.engine.model.LlmMessage;

import java.util.List;

public record CompactionResult(
    List<LlmMessage> messages,
    String strategy,
    boolean noop
) {

    public static CompactionResult noop() {
        return new CompactionResult(null, null, true);
    }

    public static CompactionResult success(List<LlmMessage> messages, String strategy) {
        return new CompactionResult(messages, strategy, false);
    }
}
