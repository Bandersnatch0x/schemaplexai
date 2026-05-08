package com.schemaplexai.agent.engine.learning;

import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionRecorder;
import com.schemaplexai.agent.engine.tool.ToolExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service that analyzes tool execution failure trends from recorded data.
 * Detects anomalies such as increasing failure rates for specific tools or error categories.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackTrendAnalyzer {

    private final ToolExecutionRecorder toolExecutionRecorder;

    private static final long ANOMALY_FAILURE_THRESHOLD = 5;
    private static final double ANOMALY_RATE_THRESHOLD = 0.5;

    /**
     * Analyzes tool failure trends for the given tenant.
     *
     * @param tenantId the tenant identifier
     * @return list of detected tool failure patterns, sorted by failure count descending
     */
    public List<ToolFailurePattern> analyzeTrends(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return Collections.emptyList();
        }

        List<ToolExecutionResult> recentFailures = fetchRecentFailures(tenantId);
        if (recentFailures.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<ToolExecutionResult>> grouped = recentFailures.stream()
                .collect(Collectors.groupingBy(r -> r.toolName() + "#" + r.errorCategory()));

        List<ToolFailurePattern> patterns = new ArrayList<>();
        for (List<ToolExecutionResult> group : grouped.values()) {
            if (group.isEmpty()) {
                continue;
            }
            ToolExecutionResult latest = group.get(0);
            long count = group.size();
            Instant lastFailure = group.stream()
                    .map(ToolExecutionResult::latencyMs)
                    .max(Long::compare)
                    .map(Instant::ofEpochMilli)
                    .orElse(Instant.now());

            ToolFailurePattern.Trend trend = computeTrend(group);
            patterns.add(new ToolFailurePattern(
                    latest.toolName(),
                    latest.errorCategory(),
                    count,
                    lastFailure,
                    tenantId,
                    trend
            ));
        }

        patterns.sort(Comparator.comparingLong(ToolFailurePattern::failureCount).reversed());
        log.info("Analyzed {} failure patterns for tenant={}", patterns.size(), tenantId);
        return patterns;
    }

    /**
     * Detects anomalous failure patterns for the given tenant.
     * An anomaly is defined as a pattern with high failure count or high failure rate.
     *
     * @param tenantId the tenant identifier
     * @return list of anomalous patterns
     */
    public List<ToolFailurePattern> detectAnomalies(String tenantId) {
        List<ToolFailurePattern> allPatterns = analyzeTrends(tenantId);
        if (allPatterns.isEmpty()) {
            return Collections.emptyList();
        }

        long totalFailures = allPatterns.stream()
                .mapToLong(ToolFailurePattern::failureCount)
                .sum();

        List<ToolFailurePattern> anomalies = new ArrayList<>();
        for (ToolFailurePattern pattern : allPatterns) {
            double rate = totalFailures > 0 ? (double) pattern.failureCount() / totalFailures : 0.0;
            if (pattern.failureCount() >= ANOMALY_FAILURE_THRESHOLD || rate >= ANOMALY_RATE_THRESHOLD) {
                anomalies.add(pattern);
                log.warn("Anomaly detected: tool={}, category={}, count={}, rate={:.2f}, tenant={}",
                        pattern.toolName(), pattern.errorCategory(), pattern.failureCount(), rate, tenantId);
            }
        }

        return anomalies;
    }

    /**
     * Computes trend direction based on latency values as a proxy for chronological ordering.
     * In a real implementation this would use actual timestamps from persisted records.
     */
    private ToolFailurePattern.Trend computeTrend(List<ToolExecutionResult> results) {
        if (results.size() < 3) {
            return ToolFailurePattern.Trend.STABLE;
        }
        List<Long> latencies = results.stream()
                .map(ToolExecutionResult::latencyMs)
                .sorted()
                .toList();
        int mid = latencies.size() / 2;
        double firstHalfAvg = latencies.subList(0, mid).stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        double secondHalfAvg = latencies.subList(mid, latencies.size()).stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        double delta = secondHalfAvg - firstHalfAvg;
        if (delta > 100) {
            return ToolFailurePattern.Trend.INCREASING;
        }
        if (delta < -100) {
            return ToolFailurePattern.Trend.DECREASING;
        }
        return ToolFailurePattern.Trend.STABLE;
    }

    /**
     * Fetches recent tool execution failures for the tenant.
     * This is a placeholder integration point; in production this would query
     * the mapper or a time-series store for historical data.
     */
    private List<ToolExecutionResult> fetchRecentFailures(String tenantId) {
        // Placeholder: in a real system this would query the database via the recorder or a dedicated DAO.
        // Returning empty list here because ToolExecutionRecorder does not expose historical queries.
        log.debug("Fetching recent failures for tenant={}", tenantId);
        return Collections.emptyList();
    }
}
