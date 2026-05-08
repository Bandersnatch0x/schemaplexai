package com.schemaplexai.agent.engine.learning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PromptOptimizer")
class PromptOptimizerTest {

    private PromptOptimizer optimizer;

    @BeforeEach
    void setUp() {
        optimizer = new PromptOptimizer();
    }

    @Nested
    @DisplayName("suggestOptimization(String)")
    class SuggestOptimizationByIdTests {

        @Test
        @DisplayName("should return generic suggestion for valid promptTemplateId")
        void shouldReturnGenericSuggestion() {
            String suggestion = optimizer.suggestOptimization("prompt-123");
            assertThat(suggestion).contains("prompt-123");
            assertThat(suggestion).containsIgnoringCase("review");
        }

        @Test
        @DisplayName("should return error message for null promptTemplateId")
        void shouldReturnErrorForNullId() {
            String suggestion = optimizer.suggestOptimization((String) null);
            assertThat(suggestion).containsIgnoringCase("required");
        }

        @Test
        @DisplayName("should return error message for blank promptTemplateId")
        void shouldReturnErrorForBlankId() {
            String suggestion = optimizer.suggestOptimization("   ");
            assertThat(suggestion).containsIgnoringCase("required");
        }
    }

    @Nested
    @DisplayName("suggestOptimization(PromptPerformancePattern)")
    class SuggestOptimizationByPatternTests {

        @Test
        @DisplayName("should suggest latency optimization for high latency")
        void shouldSuggestLatencyOptimization() {
            PromptPerformancePattern pattern = new PromptPerformancePattern(
                    "prompt-latency", 5000.0, 0.95, 0.8, Instant.now(), "tenant-1"
            );
            String suggestion = optimizer.suggestOptimization(pattern);
            assertThat(suggestion).containsIgnoringCase("latency");
            assertThat(suggestion).contains("5000.0 ms");
        }

        @Test
        @DisplayName("should suggest success rate optimization for low success rate")
        void shouldSuggestSuccessRateOptimization() {
            PromptPerformancePattern pattern = new PromptPerformancePattern(
                    "prompt-success", 500.0, 0.5, 0.8, Instant.now(), "tenant-1"
            );
            String suggestion = optimizer.suggestOptimization(pattern);
            assertThat(suggestion).containsIgnoringCase("success rate");
            assertThat(suggestion).contains("50.0%");
        }

        @Test
        @DisplayName("should suggest token efficiency optimization for poor efficiency")
        void shouldSuggestEfficiencyOptimization() {
            PromptPerformancePattern pattern = new PromptPerformancePattern(
                    "prompt-efficiency", 500.0, 0.95, 0.3, Instant.now(), "tenant-1"
            );
            String suggestion = optimizer.suggestOptimization(pattern);
            assertThat(suggestion).containsIgnoringCase("token efficiency");
            assertThat(suggestion).contains("0.30");
        }

        @Test
        @DisplayName("should return within-threshold message for good performance")
        void shouldReturnWithinThresholdMessage() {
            PromptPerformancePattern pattern = new PromptPerformancePattern(
                    "prompt-good", 500.0, 0.95, 0.9, Instant.now(), "tenant-1"
            );
            String suggestion = optimizer.suggestOptimization(pattern);
            assertThat(suggestion).containsIgnoringCase("acceptable");
        }

        @Test
        @DisplayName("should return error message for null pattern")
        void shouldReturnErrorForNullPattern() {
            String suggestion = optimizer.suggestOptimization((PromptPerformancePattern) null);
            assertThat(suggestion).containsIgnoringCase("null");
        }
    }

    @Nested
    @DisplayName("calculateEfficiencyScore")
    class CalculateEfficiencyScoreTests {

        @Test
        @DisplayName("should return 0.0 for null pattern")
        void shouldReturnZeroForNull() {
            double score = optimizer.calculateEfficiencyScore(null);
            assertEquals(0.0, score, 0.001);
        }

        @Test
        @DisplayName("should return high score for excellent performance")
        void shouldReturnHighScoreForExcellent() {
            PromptPerformancePattern pattern = new PromptPerformancePattern(
                    "prompt-excellent", 100.0, 1.0, 1.0, Instant.now(), "tenant-1"
            );
            double score = optimizer.calculateEfficiencyScore(pattern);
            assertThat(score).isGreaterThan(0.9);
            assertThat(score).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("should return low score for poor performance")
        void shouldReturnLowScoreForPoor() {
            PromptPerformancePattern pattern = new PromptPerformancePattern(
                    "prompt-poor", 10000.0, 0.0, 0.0, Instant.now(), "tenant-1"
            );
            double score = optimizer.calculateEfficiencyScore(pattern);
            assertThat(score).isLessThan(0.3);
            assertThat(score).isGreaterThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("should return mid-range score for average performance")
        void shouldReturnMidRangeForAverage() {
            PromptPerformancePattern pattern = new PromptPerformancePattern(
                    "prompt-average", 1000.0, 0.85, 0.7, Instant.now(), "tenant-1"
            );
            double score = optimizer.calculateEfficiencyScore(pattern);
            assertThat(score).isGreaterThan(0.4);
            assertThat(score).isLessThan(0.9);
        }

        @Test
        @DisplayName("should clamp score to 1.0 maximum")
        void shouldClampToOne() {
            PromptPerformancePattern pattern = new PromptPerformancePattern(
                    "prompt-max", 0.0, 1.0, 1.0, Instant.now(), "tenant-1"
            );
            double score = optimizer.calculateEfficiencyScore(pattern);
            assertEquals(1.0, score, 0.001);
        }

        @Test
        @DisplayName("should clamp score to 0.0 minimum")
        void shouldClampToZero() {
            PromptPerformancePattern pattern = new PromptPerformancePattern(
                    "prompt-min", 10000.0, 0.0, 0.0, Instant.now(), "tenant-1"
            );
            double score = optimizer.calculateEfficiencyScore(pattern);
            assertEquals(0.0, score, 0.001);
        }
    }
}
