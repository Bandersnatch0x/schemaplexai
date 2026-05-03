package com.schemaplexai.agent.engine.evaluation;

/**
 * Context passed to the reflecting state handler for self-evaluation.
 */
public record ReflectionContext(
    String userMessage,
    String finalAnswer,
    int reflectionCount
) {
}
