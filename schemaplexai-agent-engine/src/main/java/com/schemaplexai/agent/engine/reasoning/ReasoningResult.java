package com.schemaplexai.agent.engine.reasoning;

import java.util.Collections;
import java.util.List;

/**
 * The final result of a reasoning process, including the answer and all recorded steps.
 *
 * @param finalAnswer       the final output produced by the reasoning process
 * @param steps             ordered list of reasoning steps taken
 * @param totalSteps        total number of steps recorded
 * @param averageConfidence average confidence across all steps (0.0 if no steps)
 */
public record ReasoningResult(
        String finalAnswer,
        List<ReasoningStep> steps,
        int totalSteps,
        double averageConfidence
) {

    public ReasoningResult {
        steps = steps == null ? List.of() : List.copyOf(steps);
        if (totalSteps < 0) {
            throw new IllegalArgumentException("totalSteps must be >= 0");
        }
        if (averageConfidence < 0.0 || averageConfidence > 1.0) {
            throw new IllegalArgumentException("averageConfidence must be between 0.0 and 1.0");
        }
    }

    /**
     * Convenience factory for an empty result with no steps.
     */
    public static ReasoningResult empty(String finalAnswer) {
        return new ReasoningResult(finalAnswer, List.of(), 0, 0.0);
    }

    /**
     * Returns an unmodifiable view of the steps.
     */
    public List<ReasoningStep> steps() {
        return Collections.unmodifiableList(steps);
    }
}
