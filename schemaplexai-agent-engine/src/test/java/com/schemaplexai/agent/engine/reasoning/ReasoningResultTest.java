package com.schemaplexai.agent.engine.reasoning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReasoningResult Record Tests")
class ReasoningResultTest {

    @Test
    @DisplayName("valid construction succeeds")
    void validConstruction() {
        List<ReasoningStep> steps = List.of(
                new ReasoningStep(1, "A", "Reason A", 0.8),
                new ReasoningStep(2, "B", "Reason B", 0.9)
        );
        ReasoningResult result = new ReasoningResult("Answer", steps, 2, 0.85);

        assertEquals("Answer", result.finalAnswer());
        assertEquals(2, result.totalSteps());
        assertEquals(0.85, result.averageConfidence());
        assertEquals(2, result.steps().size());
    }

    @Test
    @DisplayName("empty factory creates result with no steps")
    void emptyFactory() {
        ReasoningResult result = ReasoningResult.empty("No reasoning needed");

        assertEquals("No reasoning needed", result.finalAnswer());
        assertEquals(0, result.totalSteps());
        assertEquals(0.0, result.averageConfidence());
        assertTrue(result.steps().isEmpty());
    }

    @Test
    @DisplayName("null steps defaults to empty list")
    void nullStepsDefault() {
        ReasoningResult result = new ReasoningResult("Answer", null, 0, 0.0);
        assertTrue(result.steps().isEmpty());
    }

    @Test
    @DisplayName("steps list is immutable from outside")
    void stepsImmutable() {
        ReasoningResult result = new ReasoningResult("Answer",
                List.of(new ReasoningStep(1, "A", "R", 0.5)), 1, 0.5);
        assertThrows(UnsupportedOperationException.class,
                () -> result.steps().add(new ReasoningStep(2, "B", "R", 0.5)));
    }

    @Test
    @DisplayName("negative totalSteps throws")
    void negativeTotalSteps() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReasoningResult("Answer", List.of(), -1, 0.5));
    }

    @Test
    @DisplayName("averageConfidence out of range throws")
    void confidenceOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReasoningResult("Answer", List.of(), 0, -0.1));
        assertThrows(IllegalArgumentException.class,
                () -> new ReasoningResult("Answer", List.of(), 0, 1.01));
    }
}
