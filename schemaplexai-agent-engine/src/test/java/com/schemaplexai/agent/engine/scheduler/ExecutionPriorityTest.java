package com.schemaplexai.agent.engine.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExecutionPriority")
class ExecutionPriorityTest {

    @Test
    @DisplayName("CRITICAL should have weight 1")
    void criticalWeight() {
        assertThat(ExecutionPriority.CRITICAL.getWeight()).isEqualTo(1);
    }

    @Test
    @DisplayName("HIGH should have weight 2")
    void highWeight() {
        assertThat(ExecutionPriority.HIGH.getWeight()).isEqualTo(2);
    }

    @Test
    @DisplayName("NORMAL should have weight 3")
    void normalWeight() {
        assertThat(ExecutionPriority.NORMAL.getWeight()).isEqualTo(3);
    }

    @Test
    @DisplayName("LOW should have weight 4")
    void lowWeight() {
        assertThat(ExecutionPriority.LOW.getWeight()).isEqualTo(4);
    }

    @Test
    @DisplayName("BACKGROUND should have weight 5")
    void backgroundWeight() {
        assertThat(ExecutionPriority.BACKGROUND.getWeight()).isEqualTo(5);
    }

    @Test
    @DisplayName("weights should increase with decreasing urgency")
    void weightsIncreaseWithDecreasingUrgency() {
        assertThat(ExecutionPriority.CRITICAL.getWeight())
                .isLessThan(ExecutionPriority.HIGH.getWeight())
                .isLessThan(ExecutionPriority.NORMAL.getWeight())
                .isLessThan(ExecutionPriority.LOW.getWeight())
                .isLessThan(ExecutionPriority.BACKGROUND.getWeight());
    }
}
