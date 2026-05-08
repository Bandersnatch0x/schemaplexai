package com.schemaplexai.agent.engine.learning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("ModelSelector")
class ModelSelectorTest {

    private ModelSelector modelSelector;

    @BeforeEach
    void setUp() {
        modelSelector = new ModelSelector();
    }

    @Nested
    @DisplayName("selectModelForTask")
    class SelectModelForTaskTests {

        @Test
        @DisplayName("should select a model for reasoning task with high priority")
        void shouldSelectModelForReasoningHighPriority() {
            String model = modelSelector.selectModelForTask("reasoning", 4096, ModelSelector.ExecutionPriority.HIGH);
            assertNotNull(model);
            assertThat(model).isNotBlank();
        }

        @Test
        @DisplayName("should select a model for code task with critical priority")
        void shouldSelectModelForCodeCriticalPriority() {
            String model = modelSelector.selectModelForTask("code", 2048, ModelSelector.ExecutionPriority.CRITICAL);
            assertNotNull(model);
            assertThat(model).isNotBlank();
        }

        @Test
        @DisplayName("should select a cheaper model for low priority small tasks")
        void shouldSelectCheaperModelForLowPriority() {
            String model = modelSelector.selectModelForTask("summarization", 1024, ModelSelector.ExecutionPriority.LOW);
            assertNotNull(model);
            assertThat(model).isNotBlank();
        }

        @Test
        @DisplayName("should handle null taskType gracefully")
        void shouldHandleNullTaskType() {
            String model = modelSelector.selectModelForTask(null, 2048, ModelSelector.ExecutionPriority.MEDIUM);
            assertNotNull(model);
            assertThat(model).isNotBlank();
        }

        @Test
        @DisplayName("should handle negative estimated tokens")
        void shouldHandleNegativeTokens() {
            String model = modelSelector.selectModelForTask("general", -100, ModelSelector.ExecutionPriority.MEDIUM);
            assertNotNull(model);
            assertThat(model).isNotBlank();
        }

        @Test
        @DisplayName("should handle very large token estimates")
        void shouldHandleLargeTokens() {
            String model = modelSelector.selectModelForTask("general", 50000, ModelSelector.ExecutionPriority.HIGH);
            assertNotNull(model);
            assertThat(model).isNotBlank();
        }

        @Test
        @DisplayName("should be consistent for same inputs")
        void shouldBeConsistent() {
            String model1 = modelSelector.selectModelForTask("reasoning", 4096, ModelSelector.ExecutionPriority.HIGH);
            String model2 = modelSelector.selectModelForTask("reasoning", 4096, ModelSelector.ExecutionPriority.HIGH);
            assertEquals(model1, model2);
        }
    }

    @Nested
    @DisplayName("calculateModelScore")
    class CalculateModelScoreTests {

        @Test
        @DisplayName("should score higher for quality on critical priority")
        void shouldWeightQualityForCritical() {
            ModelSelector.ModelProfile highQuality = new ModelSelector.ModelProfile(8, 5, 10);
            ModelSelector.ModelProfile lowQuality = new ModelSelector.ModelProfile(2, 9, 4);

            double highScore = modelSelector.calculateModelScore(highQuality, "reasoning", 4096, ModelSelector.ExecutionPriority.CRITICAL);
            double lowScore = modelSelector.calculateModelScore(lowQuality, "reasoning", 4096, ModelSelector.ExecutionPriority.CRITICAL);

            assertThat(highScore).isGreaterThan(lowScore);
        }

        @Test
        @DisplayName("should score higher for latency on low priority")
        void shouldWeightLatencyForLowPriority() {
            ModelSelector.ModelProfile fast = new ModelSelector.ModelProfile(5, 9, 6);
            ModelSelector.ModelProfile slow = new ModelSelector.ModelProfile(5, 3, 9);

            double fastScore = modelSelector.calculateModelScore(fast, "general", 1024, ModelSelector.ExecutionPriority.LOW);
            double slowScore = modelSelector.calculateModelScore(slow, "general", 1024, ModelSelector.ExecutionPriority.LOW);

            assertThat(fastScore).isGreaterThan(slowScore);
        }

        @Test
        @DisplayName("should penalize large token estimates")
        void shouldPenalizeLargeTokens() {
            ModelSelector.ModelProfile profile = new ModelSelector.ModelProfile(5, 7, 8);

            double smallScore = modelSelector.calculateModelScore(profile, "general", 1024, ModelSelector.ExecutionPriority.MEDIUM);
            double largeScore = modelSelector.calculateModelScore(profile, "general", 10000, ModelSelector.ExecutionPriority.MEDIUM);

            assertThat(smallScore).isGreaterThan(largeScore);
        }

        @Test
        @DisplayName("should reward small token estimates")
        void shouldRewardSmallTokens() {
            ModelSelector.ModelProfile profile = new ModelSelector.ModelProfile(5, 7, 8);

            double smallScore = modelSelector.calculateModelScore(profile, "general", 512, ModelSelector.ExecutionPriority.MEDIUM);
            double mediumScore = modelSelector.calculateModelScore(profile, "general", 4096, ModelSelector.ExecutionPriority.MEDIUM);

            assertThat(smallScore).isGreaterThan(mediumScore);
        }
    }

    @Nested
    @DisplayName("ExecutionPriority")
    class ExecutionPriorityTests {

        @Test
        @DisplayName("should contain all expected priority levels")
        void shouldContainAllLevels() {
            assertThat(ModelSelector.ExecutionPriority.values()).containsExactly(
                    ModelSelector.ExecutionPriority.LOW,
                    ModelSelector.ExecutionPriority.MEDIUM,
                    ModelSelector.ExecutionPriority.HIGH,
                    ModelSelector.ExecutionPriority.CRITICAL
            );
        }
    }
}
