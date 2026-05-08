package com.schemaplexai.agent.engine.reasoning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReasoningStep Record Tests")
class ReasoningStepTest {

    @Test
    @DisplayName("valid construction succeeds")
    void validConstruction() {
        Instant now = Instant.now();
        ReasoningStep step = new ReasoningStep(1, "Analyze", "Detailed reasoning", 0.85, now);

        assertEquals(1, step.stepNumber());
        assertEquals("Analyze", step.description());
        assertEquals("Detailed reasoning", step.reasoning());
        assertEquals(0.85, step.confidenceScore());
        assertEquals(now, step.timestamp());
    }

    @Test
    @DisplayName("constructor without timestamp assigns current timestamp")
    void constructorWithoutTimestamp() {
        ReasoningStep step = new ReasoningStep(2, "Synthesize", "Combined results", 0.92);

        assertEquals(2, step.stepNumber());
        assertEquals("Synthesize", step.description());
        assertNotNull(step.timestamp());
    }

    @Test
    @DisplayName("null timestamp defaults to now")
    void nullTimestampDefaultsToNow() {
        ReasoningStep step = new ReasoningStep(1, "Step", "Reasoning", 0.5, null);
        assertNotNull(step.timestamp());
    }

    @Test
    @DisplayName("invalid stepNumber throws")
    void invalidStepNumber() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReasoningStep(0, "Step", "Reasoning", 0.5, Instant.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new ReasoningStep(-1, "Step", "Reasoning", 0.5, Instant.now()));
    }

    @Test
    @DisplayName("confidence out of range throws")
    void confidenceOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReasoningStep(1, "Step", "Reasoning", -0.1, Instant.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new ReasoningStep(1, "Step", "Reasoning", 1.01, Instant.now()));
    }

    @Test
    @DisplayName("boundary confidence values are accepted")
    void boundaryConfidence() {
        assertDoesNotThrow(() -> new ReasoningStep(1, "Step", "Reasoning", 0.0, Instant.now()));
        assertDoesNotThrow(() -> new ReasoningStep(1, "Step", "Reasoning", 1.0, Instant.now()));
    }
}
