package com.schemaplexai.agent.engine.memory.compaction;

import com.schemaplexai.agent.engine.model.LlmMessage;

import java.util.List;

public record CompactionResult(
    List<LlmMessage> messages,
    String strategy,
    boolean success,
    boolean noOp,
    String failureReason
) {

    public static CompactionResult empty() {
        return new CompactionResult(null, null, true, true, null);
    }

    public static CompactionResult success(List<LlmMessage> messages, String strategy) {
        return new CompactionResult(messages, strategy, true, false, null);
    }

    public static CompactionResult failed(String failureReason) {
        return new CompactionResult(null, null, false, false, failureReason);
    }

    public String strategyName() {
        return strategy;
    }
}
