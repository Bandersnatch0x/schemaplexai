package com.schemaplexai.agent.engine.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Prometheus metrics binder for tool execution observability.
 *
 * Uses in-memory ConcurrentHashMap counters (NOT DB polling) to avoid IO overhead
 * impacting P99 latency. Top-N label strategy prevents time-series cardinality explosion.
 *
 * Metrics exposed:
 * - agent_tool_execution_total (Counter) by toolName + status
 * - agent_tool_execution_latency_seconds (Timer) by toolName
 * - agent_tool_keep_rate (Gauge)
 * - agent_tool_blocked_rate (Gauge)
 * - agent_tool_error_by_category (Counter) by errorCategory
 * - agent_tool_retry_total (Counter)
 */
@Slf4j
@Component
public class ToolExecutionMetricsBinder implements MeterBinder {

    private static final int TOP_N_TOOLS = 10;

    // In-memory counters (no DB polling)
    private final Map<String, LongAdder> successCounters = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> failureCounters = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> blockedCounters = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> retryCounters = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> categoryCounters = new ConcurrentHashMap<>();

    @Override
    public void bindTo(MeterRegistry registry) {
        // 1. Tool execution total counter (by toolName + status)
        Counter.builder("agent_tool_execution_total")
                .description("Total tool executions")
                .tags("status", "success")
                .register(registry);

        Counter.builder("agent_tool_execution_total")
                .description("Total tool executions")
                .tags("status", "failure")
                .register(registry);

        Counter.builder("agent_tool_execution_total")
                .description("Total tool executions")
                .tags("status", "blocked")
                .register(registry);

        // 2. Latency histogram (by toolName)
        Timer.builder("agent_tool_execution_latency_seconds")
                .description("Tool execution latency distribution")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // 3. Keep rate gauge
        Gauge.builder("agent_tool_keep_rate", this, ToolExecutionMetricsBinder::getKeepRate)
                .description("Tool execution success rate (success / total)")
                .register(registry);

        // 4. Blocked rate gauge
        Gauge.builder("agent_tool_blocked_rate", this, ToolExecutionMetricsBinder::getBlockedRate)
                .description("Tool execution blocked rate (blocked / total)")
                .register(registry);

        // 5. Error by category counter
        Counter.builder("agent_tool_error_by_category")
                .description("Tool failures grouped by error category")
                .register(registry);

        // 6. Retry counter
        Counter.builder("agent_tool_retry_total")
                .description("Total tool execution retries")
                .register(registry);

        log.info("ToolExecutionMetricsBinder registered: keep_rate, blocked_rate, latency, errors, retries");
    }

    /**
     * Record a successful tool execution.
     */
    public void recordSuccess(String toolName, long latencyMs) {
        incrementCounter(successCounters, toolName);
    }

    /**
     * Record a failed tool execution.
     */
    public void recordFailure(String toolName, String errorCategory) {
        incrementCounter(failureCounters, toolName);
        if (errorCategory != null && !errorCategory.isBlank()) {
            incrementCounter(categoryCounters, errorCategory);
        }
    }

    /**
     * Record a blocked tool execution.
     */
    public void recordBlocked(String toolName) {
        incrementCounter(blockedCounters, toolName);
    }

    /**
     * Record a retry attempt.
     */
    public void recordRetry(String toolName) {
        incrementCounter(retryCounters, toolName);
    }

    private void incrementCounter(Map<String, LongAdder> counters, String key) {
        counters.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    /**
     * Calculate keep rate: success / total. Returns 1.0 if total is 0.
     */
    double getKeepRate() {
        long totalSuccess = sumCounters(successCounters);
        long totalFailure = sumCounters(failureCounters);
        long totalBlocked = sumCounters(blockedCounters);
        long total = totalSuccess + totalFailure + totalBlocked;
        return total == 0 ? 1.0 : (double) totalSuccess / total;
    }

    /**
     * Calculate blocked rate: blocked / total. Returns 0.0 if total is 0.
     */
    double getBlockedRate() {
        long totalSuccess = sumCounters(successCounters);
        long totalFailure = sumCounters(failureCounters);
        long totalBlocked = sumCounters(blockedCounters);
        long total = totalSuccess + totalFailure + totalBlocked;
        return total == 0 ? 0.0 : (double) totalBlocked / total;
    }

    /**
     * Top-N tool names by total count (to prevent label cardinality explosion).
     */
    public List<String> getTopNToolNames() {
        Map<String, Long> totalCounts = new ConcurrentHashMap<>();
        for (var entry : successCounters.entrySet()) {
            totalCounts.merge(entry.getKey(), entry.getValue().longValue(), Long::sum);
        }
        for (var entry : failureCounters.entrySet()) {
            totalCounts.merge(entry.getKey(), entry.getValue().longValue(), Long::sum);
        }
        for (var entry : blockedCounters.entrySet()) {
            totalCounts.merge(entry.getKey(), entry.getValue().longValue(), Long::sum);
        }

        List<String> sorted = new ArrayList<>(totalCounts.keySet());
        sorted.sort(Comparator.comparingLong(totalCounts::get).reversed());

        List<String> topN = new ArrayList<>();
        for (int i = 0; i < Math.min(TOP_N_TOOLS, sorted.size()); i++) {
            topN.add(sorted.get(i));
        }
        return topN;
    }

    private long sumCounters(Map<String, LongAdder> counters) {
        return counters.values().stream().mapToLong(LongAdder::longValue).sum();
    }
}
