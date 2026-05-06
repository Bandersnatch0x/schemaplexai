package com.schemaplexai.agent.engine.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ToolExecutionMetricsBinder")
class ToolExecutionMetricsBinderTest {

    private ToolExecutionMetricsBinder binder;

    @BeforeEach
    void setUp() {
        binder = new ToolExecutionMetricsBinder();
    }

    @Nested
    @DisplayName("recordSuccess")
    class RecordSuccessTests {

        @Test
        @DisplayName("should increment success counter")
        void shouldIncrementSuccess() {
            binder.recordSuccess("fileRead", 100);
            binder.recordSuccess("fileRead", 200);

            // keepRate should be 1.0 (all success, no failures)
            assertEquals(1.0, binder.getKeepRate(), 0.001);
        }

        @Test
        @DisplayName("should track different tools independently")
        void shouldTrackDifferentTools() {
            binder.recordSuccess("fileRead", 100);
            binder.recordSuccess("httpCall", 200);

            List<String> topN = binder.getTopNToolNames();
            assertThat(topN).hasSize(2);
            assertThat(topN).containsExactlyInAnyOrder("fileRead", "httpCall");
        }
    }

    @Nested
    @DisplayName("recordFailure")
    class RecordFailureTests {

        @Test
        @DisplayName("should increment failure counter")
        void shouldIncrementFailure() {
            binder.recordSuccess("fileRead", 100);
            binder.recordFailure("fileRead", "TIMEOUT");

            // 1 success + 1 failure = keepRate 0.5
            assertEquals(0.5, binder.getKeepRate(), 0.001);
        }

        @Test
        @DisplayName("should handle null error category")
        void shouldHandleNullCategory() {
            assertDoesNotThrow(() -> binder.recordFailure("fileRead", null));
        }

        @Test
        @DisplayName("should handle blank error category")
        void shouldHandleBlankCategory() {
            assertDoesNotThrow(() -> binder.recordFailure("fileRead", "  "));
        }
    }

    @Nested
    @DisplayName("recordBlocked")
    class RecordBlockedTests {

        @Test
        @DisplayName("should increment blocked counter")
        void shouldIncrementBlocked() {
            binder.recordSuccess("fileRead", 100);
            binder.recordBlocked("volumeDelete");

            // 1 success + 1 blocked
            assertEquals(0.5, binder.getKeepRate(), 0.001);
            assertEquals(0.5, binder.getBlockedRate(), 0.001);
        }
    }

    @Nested
    @DisplayName("getKeepRate")
    class KeepRateTests {

        @Test
        @DisplayName("should return 1.0 when no executions recorded")
        void shouldReturnOneWhenEmpty() {
            assertEquals(1.0, binder.getKeepRate(), 0.001);
        }

        @Test
        @DisplayName("should return 1.0 when all executions succeed")
        void shouldReturnOneWhenAllSuccess() {
            binder.recordSuccess("tool1", 100);
            binder.recordSuccess("tool2", 200);

            assertEquals(1.0, binder.getKeepRate(), 0.001);
        }

        @Test
        @DisplayName("should return 0.0 when all executions fail or blocked")
        void shouldReturnZeroWhenAllFail() {
            binder.recordFailure("tool1", "ERROR");
            binder.recordBlocked("tool2");

            assertEquals(0.0, binder.getKeepRate(), 0.001);
        }

        @Test
        @DisplayName("should calculate correct rate with mixed results")
        void shouldCalculateMixedRate() {
            binder.recordSuccess("tool1", 100);
            binder.recordSuccess("tool2", 200);
            binder.recordSuccess("tool3", 300);
            binder.recordFailure("tool4", "TIMEOUT");

            // 3 success / 4 total = 0.75
            assertEquals(0.75, binder.getKeepRate(), 0.001);
        }
    }

    @Nested
    @DisplayName("getBlockedRate")
    class BlockedRateTests {

        @Test
        @DisplayName("should return 0.0 when no executions recorded")
        void shouldReturnZeroWhenEmpty() {
            assertEquals(0.0, binder.getBlockedRate(), 0.001);
        }

        @Test
        @DisplayName("should return 0.0 when no executions blocked")
        void shouldReturnZeroWhenNoneBlocked() {
            binder.recordSuccess("tool1", 100);
            binder.recordFailure("tool2", "ERROR");

            assertEquals(0.0, binder.getBlockedRate(), 0.001);
        }

        @Test
        @DisplayName("should calculate correct blocked rate")
        void shouldCalculateCorrectRate() {
            binder.recordSuccess("tool1", 100);
            binder.recordBlocked("tool2");
            binder.recordBlocked("tool3");

            // 2 blocked / 3 total = 0.666...
            assertEquals(2.0 / 3.0, binder.getBlockedRate(), 0.01);
        }
    }

    @Nested
    @DisplayName("recordRetry")
    class RecordRetryTests {

        @Test
        @DisplayName("should track retry count independently")
        void shouldTrackRetry() {
            // recordRetry increments retryCounters but does NOT contribute to
            // success/failure/blocked counters, so it doesn't affect getTopNToolNames()
            // which only considers success+failure+blocked.
            assertDoesNotThrow(() -> {
                binder.recordRetry("fileRead");
                binder.recordRetry("fileRead");
                binder.recordRetry("httpCall");
            });

            // TopN should be empty since retries are tracked separately
            assertThat(binder.getTopNToolNames()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTopNToolNames")
    class TopNTests {

        @Test
        @DisplayName("should return empty list when no data")
        void shouldReturnEmptyWhenNoData() {
            assertThat(binder.getTopNToolNames()).isEmpty();
        }

        @Test
        @DisplayName("should return tools sorted by total count descending")
        void shouldReturnSortedByCount() {
            // tool1: 3 successes
            binder.recordSuccess("tool1", 100);
            binder.recordSuccess("tool1", 100);
            binder.recordSuccess("tool1", 100);

            // tool2: 1 success + 1 failure = 2
            binder.recordSuccess("tool2", 100);
            binder.recordFailure("tool2", "ERR");

            // tool3: 1 blocked
            binder.recordBlocked("tool3");

            List<String> topN = binder.getTopNToolNames();
            assertThat(topN).hasSize(3);
            assertThat(topN.get(0)).isEqualTo("tool1");
        }

        @Test
        @DisplayName("should limit to top 10 tools")
        void shouldLimitToTen() {
            for (int i = 0; i < 15; i++) {
                binder.recordSuccess("tool" + i, 100);
            }

            List<String> topN = binder.getTopNToolNames();
            assertThat(topN).hasSize(10);
        }
    }

    @Nested
    @DisplayName("mixed scenario")
    class MixedScenarioTests {

        @Test
        @DisplayName("should correctly track complex multi-tool scenario")
        void shouldTrackComplexScenario() {
            // Simulate realistic usage
            for (int i = 0; i < 10; i++) {
                binder.recordSuccess("fileRead", 50 + i * 10);
            }
            for (int i = 0; i < 3; i++) {
                binder.recordSuccess("httpCall", 200 + i * 50);
            }
            binder.recordFailure("httpCall", "TIMEOUT");
            binder.recordFailure("httpCall", "TIMEOUT");
            binder.recordBlocked("volumeDelete");
            binder.recordBlocked("databaseDrop");

            // 13 success / 17 total = 0.764...
            assertEquals(13.0 / 17.0, binder.getKeepRate(), 0.01);
            // 2 blocked / 17 total = 0.117...
            assertEquals(2.0 / 17.0, binder.getBlockedRate(), 0.01);
        }
    }
}
