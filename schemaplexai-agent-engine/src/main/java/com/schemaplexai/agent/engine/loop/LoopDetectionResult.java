package com.schemaplexai.agent.engine.loop;

public record LoopDetectionResult(boolean loopDetected, String reason) {

    public static final String HASH_LOOP = "HASH_LOOP";
    public static final String TOOL_SEQUENCE_LOOP = "TOOL_SEQUENCE_LOOP";

    public static LoopDetectionResult noLoop() {
        return new LoopDetectionResult(false, null);
    }

    public static LoopDetectionResult hashLoop() {
        return new LoopDetectionResult(true, HASH_LOOP);
    }

    public static LoopDetectionResult toolSequenceLoop() {
        return new LoopDetectionResult(true, TOOL_SEQUENCE_LOOP);
    }
}
