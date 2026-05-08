package com.schemaplexai.agent.engine.reasoning;

import java.time.Instant;

/**
 * A single step in a chain-of-thought reasoning trace.
 *
 * @param stepNumber      the sequential step index (1-based)
 * @param description     short human-readable label for the step
 * @param reasoning       the detailed reasoning content produced at this step
 * @param confidenceScore confidence in this step, range 0.0 to 1.0
 * @param timestamp       when this step was recorded
 */
public record ReasoningStep(
        int stepNumber,
        String description,
        String reasoning,
        double confidenceScore,
        Instant timestamp
) {

    public ReasoningStep {
        if (stepNumber < 1) {
            throw new IllegalArgumentException("stepNumber must be >= 1");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (reasoning == null || reasoning.isBlank()) {
            throw new IllegalArgumentException("reasoning must not be blank");
        }
        if (confidenceScore < 0.0 || confidenceScore > 1.0) {
            throw new IllegalArgumentException("confidenceScore must be between 0.0 and 1.0");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * Creates a new step with an auto-generated timestamp.
     */
    public ReasoningStep(int stepNumber, String description, String reasoning, double confidenceScore) {
        this(stepNumber, description, reasoning, confidenceScore, Instant.now());
    }

    /**
     * Factory method: creates a new step with an auto-generated timestamp.
     */
    public static ReasoningStep of(int stepNumber, String description, String reasoning, double confidenceScore) {
        return new ReasoningStep(stepNumber, description, reasoning, confidenceScore, Instant.now());
    }
}
