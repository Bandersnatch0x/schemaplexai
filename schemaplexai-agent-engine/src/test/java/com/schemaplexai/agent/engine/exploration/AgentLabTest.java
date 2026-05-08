package com.schemaplexai.agent.engine.exploration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("AgentLab")
class AgentLabTest {

    private AgentLab agentLab;

    @BeforeEach
    void setUp() {
        agentLab = new AgentLab();
    }

    @Nested
    @DisplayName("runExperiment")
    class RunExperimentTests {

        @Test
        @DisplayName("should return results for each strategy")
        void shouldReturnResultsForEachStrategy() {
            List<ExperimentResult> results = agentLab.runExperiment("summarization",
                    List.of("strategy-a", "strategy-b"));

            assertEquals(2, results.size());
            assertThat(results.stream().map(ExperimentResult::strategyName))
                    .containsExactlyInAnyOrder("strategy-a", "strategy-b");
        }

        @Test
        @DisplayName("should return empty list for empty strategies")
        void shouldReturnEmptyListForEmptyStrategies() {
            List<ExperimentResult> results = agentLab.runExperiment("code", Collections.emptyList());
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for null strategies")
        void shouldReturnEmptyListForNullStrategies() {
            List<ExperimentResult> results = agentLab.runExperiment("code", null);
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should ignore blank strategy names")
        void shouldIgnoreBlankStrategyNames() {
            List<ExperimentResult> results = agentLab.runExperiment("task", java.util.Arrays.asList("valid", "", "   ", null));
            assertEquals(1, results.size());
            assertEquals("valid", results.get(0).strategyName());
        }

        @Test
        @DisplayName("should throw for null taskType")
        void shouldThrowForNullTaskType() {
            assertThrows(IllegalArgumentException.class, () ->
                    agentLab.runExperiment(null, List.of("s1")));
        }

        @Test
        @DisplayName("should throw for blank taskType")
        void shouldThrowForBlankTaskType() {
            assertThrows(IllegalArgumentException.class, () ->
                    agentLab.runExperiment("   ", List.of("s1")));
        }

        @Test
        @DisplayName("should produce deterministic results for same inputs")
        void shouldBeDeterministic() {
            List<ExperimentResult> first = agentLab.runExperiment("reasoning", List.of("s1", "s2"));
            List<ExperimentResult> second = agentLab.runExperiment("reasoning", List.of("s1", "s2"));

            assertEquals(first.size(), second.size());
            for (int i = 0; i < first.size(); i++) {
                assertEquals(first.get(i).strategyName(), second.get(i).strategyName());
                assertEquals(first.get(i).successRate(), second.get(i).successRate(), 0.001);
                assertEquals(first.get(i).avgLatencyMs(), second.get(i).avgLatencyMs(), 0.001);
                assertEquals(first.get(i).avgTokenUsage(), second.get(i).avgTokenUsage(), 0.001);
                assertEquals(first.get(i).score(), second.get(i).score(), 0.001);
            }
        }
    }

    @Nested
    @DisplayName("compareResults")
    class CompareResultsTests {

        @Test
        @DisplayName("should sort results by score descending")
        void shouldSortByScoreDescending() {
            ExperimentResult r1 = new ExperimentResult("low", 0.5, 1000.0, 500.0, 0.3);
            ExperimentResult r2 = new ExperimentResult("high", 0.9, 800.0, 400.0, 0.9);
            ExperimentResult r3 = new ExperimentResult("mid", 0.7, 900.0, 450.0, 0.6);

            List<ExperimentResult> sorted = agentLab.compareResults(List.of(r1, r2, r3));

            assertEquals("high", sorted.get(0).strategyName());
            assertEquals("mid", sorted.get(1).strategyName());
            assertEquals("low", sorted.get(2).strategyName());
        }

        @Test
        @DisplayName("should return empty list for null input")
        void shouldReturnEmptyForNull() {
            assertThat(agentLab.compareResults(null)).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for empty input")
        void shouldReturnEmptyForEmpty() {
            assertThat(agentLab.compareResults(Collections.emptyList())).isEmpty();
        }
    }

    @Nested
    @DisplayName("recommendStrategy")
    class RecommendStrategyTests {

        @Test
        @DisplayName("should recommend best strategy after experiment")
        void shouldRecommendBestStrategy() {
            agentLab.runExperiment("generation", List.of("s1", "s2", "s3"));
            String recommended = agentLab.recommendStrategy("generation");

            assertThat(recommended).isNotBlank();
            assertThat(List.of("s1", "s2", "s3")).contains(recommended);
        }

        @Test
        @DisplayName("should return empty string when no experiments exist")
        void shouldReturnEmptyWhenNoExperiments() {
            assertEquals("", agentLab.recommendStrategy("unknown"));
        }

        @Test
        @DisplayName("should return empty string for null taskType")
        void shouldReturnEmptyForNullTaskType() {
            assertEquals("", agentLab.recommendStrategy(null));
        }

        @Test
        @DisplayName("should return empty string for blank taskType")
        void shouldReturnEmptyForBlankTaskType() {
            assertEquals("", agentLab.recommendStrategy("   "));
        }
    }
}
