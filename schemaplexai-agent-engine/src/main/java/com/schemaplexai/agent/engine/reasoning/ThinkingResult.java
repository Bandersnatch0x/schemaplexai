package com.schemaplexai.agent.engine.reasoning;

import com.schemaplexai.agent.engine.tool.ToolCall;

/**
 * Result of a reasoning step — captures the LLM's decision after thinking.
 * Types: COMPLETED (final answer ready), TOOL_CALL (needs tool invocation),
 * EXHAUSTED (token budget exceeded), ERROR (reasoning failure).
 */
public record ThinkingResult(
    Type type,
    String finalAnswer,
    ToolCall toolCall,
    String errorMessage
) {

    public enum Type {
        /** Reasoning produced a final answer. */
        COMPLETED,
        /** Reasoning determined a tool call is needed. */
        TOOL_CALL,
        /** Token budget exhausted, reasoning cannot continue. */
        EXHAUSTED,
        /** An error occurred during reasoning. */
        ERROR
    }

    public static ThinkingResult completed(String answer) {
        return new ThinkingResult(Type.COMPLETED, answer, null, null);
    }

    public static ThinkingResult toolCall(ToolCall call) {
        return new ThinkingResult(Type.TOOL_CALL, null, call, null);
    }

    public static ThinkingResult exhausted(String msg) {
        return new ThinkingResult(Type.EXHAUSTED, null, null, msg);
    }

    public static ThinkingResult error(String msg) {
        return new ThinkingResult(Type.ERROR, null, null, msg);
    }
}
