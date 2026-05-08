package com.schemaplexai.agent.engine.state.middleware.impl;

import com.schemaplexai.agent.engine.state.middleware.MiddlewareContext;
import com.schemaplexai.agent.engine.state.middleware.StateHandlerMiddleware;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Middleware that tracks execution timing metrics for each state transition.
 *
 * <p>Stores per-state duration statistics in memory (for now).
 * Future: export to Micrometer/Prometheus.
 */
@Slf4j
@Component
public class MetricsMiddleware implements StateHandlerMiddleware {

    private static final int ORDER = 200;

    private final Map<String, AtomicLong> stateTransitionCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> stateTransitionTotalMs = new ConcurrentHashMap<>();

    @Override
    public boolean before(MiddlewareContext context) {
        context.setAttribute("metrics.startTime", Instant.now());
        return true;
    }

    @Override
    public void after(MiddlewareContext context, Throwable error) {
        Instant startTime = context.getAttribute("metrics.startTime");
        if (startTime == null) return;

        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        String stateKey = context.getTargetState().name();

        stateTransitionCounts.computeIfAbsent(stateKey, k -> new AtomicLong(0)).incrementAndGet();
        stateTransitionTotalMs.computeIfAbsent(stateKey, k -> new AtomicLong(0)).addAndGet(durationMs);

        context.setAttribute("metrics.durationMs", durationMs);
    }

    /**
     * Returns the total number of transitions to the given state.
     */
    public long getTransitionCount(String state) {
        AtomicLong count = stateTransitionCounts.get(state);
        return count != null ? count.get() : 0;
    }

    /**
     * Returns the average duration (ms) for transitions to the given state.
     */
    public double getAverageDurationMs(String state) {
        AtomicLong count = stateTransitionCounts.get(state);
        AtomicLong total = stateTransitionTotalMs.get(state);
        if (count == null || total == null || count.get() == 0) return 0;
        return (double) total.get() / count.get();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
