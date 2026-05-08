package com.schemaplexai.agent.engine.exploration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ExperimentResult")
class ExperimentResultTest {

    @Test
    @DisplayName("should store all fields correctly")
    void shouldStoreAllFields() {
        ExperimentResult result = new ExperimentResult("strategy-a", 0.85, 1200.0, 900.0, 0.92);

        assertEquals("strategy-a", result.strategyName());
        assertEquals(0.85, result.successRate(), 0.001);
        assertEquals(1200.0, result.avgLatencyMs(), 0.001);
        assertEquals(900.0, result.avgTokenUsage(), 0.001);
        assertEquals(0.92, result.score(), 0.001);
    }

    @Test
    @DisplayName("should support equality based on field values")
    void shouldSupportEquality() {
        ExperimentResult r1 = new ExperimentResult("s1", 0.5, 100.0, 200.0, 0.6);
        ExperimentResult r2 = new ExperimentResult("s1", 0.5, 100.0, 200.0, 0.6);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
}
