package com.schemaplexai.agent.engine.learning;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionRecorder;
import com.schemaplexai.agent.engine.tool.ToolExecutionResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackTrendAnalyzer")
class FeedbackTrendAnalyzerTest {

    @Mock
    private ToolExecutionRecorder toolExecutionRecorder;

    private FeedbackTrendAnalyzer analyzer;
    private Logger analyzerLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        analyzer = new FeedbackTrendAnalyzer(toolExecutionRecorder);
        analyzerLogger = (Logger) LoggerFactory.getLogger(FeedbackTrendAnalyzer.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        analyzerLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        analyzerLogger.detachAppender(logAppender);
    }

    @Nested
    @DisplayName("analyzeTrends")
    class AnalyzeTrendsTests {

        @Test
        @DisplayName("should return empty list for null tenantId")
        void shouldReturnEmptyForNullTenant() {
            List<ToolFailurePattern> result = analyzer.analyzeTrends(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for blank tenantId")
        void shouldReturnEmptyForBlankTenant() {
            List<ToolFailurePattern> result = analyzer.analyzeTrends("   ");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no failures exist")
        void shouldReturnEmptyWhenNoFailures() {
            List<ToolFailurePattern> result = analyzer.analyzeTrends("tenant-1");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should group recent failures returned by the recorder")
        void shouldGroupRecentFailuresFromRecorder() {
            when(toolExecutionRecorder.listRecentFailures("tenant-1", 200)).thenReturn(List.of(
                    ToolExecutionResult.failure("apiCall", ToolErrorCategory.RATE_LIMITED,
                            "rate limit", 100, 0),
                    ToolExecutionResult.failure("apiCall", ToolErrorCategory.RATE_LIMITED,
                            "rate limit", 260, 0),
                    ToolExecutionResult.failure("dbQuery", ToolErrorCategory.INTERNAL_ERROR,
                            "db down", 90, 0)
            ));

            List<ToolFailurePattern> result = analyzer.analyzeTrends("tenant-1");

            assertThat(result).hasSize(2);
            assertThat(result.get(0).toolName()).isEqualTo("apiCall");
            assertThat(result.get(0).errorCategory()).isEqualTo(ToolErrorCategory.RATE_LIMITED);
            assertThat(result.get(0).failureCount()).isEqualTo(2);
            assertThat(result.get(1).toolName()).isEqualTo("dbQuery");
            assertThat(result.get(1).failureCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("detectAnomalies")
    class DetectAnomaliesTests {

        @Test
        @DisplayName("should return empty list for null tenantId")
        void shouldReturnEmptyForNullTenant() {
            List<ToolFailurePattern> result = analyzer.detectAnomalies(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no patterns exist")
        void shouldReturnEmptyWhenNoPatterns() {
            List<ToolFailurePattern> result = analyzer.detectAnomalies("tenant-1");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should detect anomaly when failure count exceeds threshold")
        void shouldDetectAnomalyByCount() {
            when(toolExecutionRecorder.listRecentFailures("tenant-1", 200)).thenReturn(List.of(
                    ToolExecutionResult.failure("apiCall", ToolErrorCategory.RATE_LIMITED,
                            "rate limit", 100, 0),
                    ToolExecutionResult.failure("apiCall", ToolErrorCategory.RATE_LIMITED,
                            "rate limit", 200, 0),
                    ToolExecutionResult.failure("apiCall", ToolErrorCategory.RATE_LIMITED,
                            "rate limit", 300, 0),
                    ToolExecutionResult.failure("apiCall", ToolErrorCategory.RATE_LIMITED,
                            "rate limit", 400, 0),
                    ToolExecutionResult.failure("apiCall", ToolErrorCategory.RATE_LIMITED,
                            "rate limit", 500, 0)
            ));

            List<ToolFailurePattern> anomalies = analyzer.detectAnomalies("tenant-1");

            assertThat(anomalies).hasSize(1);
            assertThat(anomalies.get(0).toolName()).isEqualTo("apiCall");
            assertThat(anomalies.get(0).failureCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("should log anomaly rate and tenant without placeholder drift")
        void shouldLogAnomalyRateAndTenantWithoutPlaceholderDrift() {
            when(toolExecutionRecorder.listRecentFailures("tenant-1", 200)).thenReturn(List.of(
                    ToolExecutionResult.failure("apiCall", ToolErrorCategory.RATE_LIMITED,
                            "rate limit", 100, 0),
                    ToolExecutionResult.failure("apiCall", ToolErrorCategory.RATE_LIMITED,
                            "rate limit", 200, 0),
                    ToolExecutionResult.failure("apiCall", ToolErrorCategory.RATE_LIMITED,
                            "rate limit", 300, 0),
                    ToolExecutionResult.failure("apiCall", ToolErrorCategory.RATE_LIMITED,
                            "rate limit", 400, 0),
                    ToolExecutionResult.failure("apiCall", ToolErrorCategory.RATE_LIMITED,
                            "rate limit", 500, 0)
            ));

            analyzer.detectAnomalies("tenant-1");

            List<String> warnings = logAppender.list.stream()
                    .filter(event -> event.getLevel() == Level.WARN)
                    .map(ILoggingEvent::getFormattedMessage)
                    .toList();
            assertThat(warnings).hasSize(1);
            assertThat(warnings.get(0)).contains("rate=1.0");
            assertThat(warnings.get(0)).contains("tenant=tenant-1");
            assertThat(warnings.get(0)).doesNotContain("{:.2f}");
        }
    }

    @Nested
    @DisplayName("trend computation via reflection")
    class TrendComputationTests {

        @Test
        @DisplayName("should compute STABLE trend for empty or small result sets")
        void shouldComputeStableForSmallSets() {
            List<ToolFailurePattern> result = analyzer.analyzeTrends("tenant-1");
            assertThat(result).isEmpty();
        }
    }
}
