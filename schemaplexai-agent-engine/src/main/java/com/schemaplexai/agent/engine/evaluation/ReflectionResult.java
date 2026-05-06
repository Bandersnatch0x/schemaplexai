package com.schemaplexai.agent.engine.evaluation;

/**
 * Result of a self-reflection round by the agent.
 */
public record ReflectionResult(
    boolean accepted,
    String suggestions
) {
    public static ReflectionResult ofAccepted() {
        return new ReflectionResult(true, null);
    }

    public static ReflectionResult ofNeedsRevision(String suggestions) {
        return new ReflectionResult(false, suggestions);
    }

    public boolean needsRevision() {
        return !accepted;
    }
}
