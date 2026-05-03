package com.schemaplexai.agent.engine.evaluation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule-based evaluator that scores agent execution across five dimensions:
 * success_rate, efficiency, token_efficiency, error_rate, and latency.
 */
public class RuleBasedEvaluator implements Evaluator {

    private static final double HIGH_ITERATION_THRESHOLD = 5;
    private static final int MAX_ITERATIONS = 10;
    private static final double LOW_TOKEN_EFFICIENCY_THRESHOLD = 0.3;
    private static final double HIGH_ERROR_RATE_THRESHOLD = 0.1;
    private static final double IMPROVEMENT_THRESHOLD = 0.1;
    private static final double DECLINE_THRESHOLD = -0.1;

    @Override
    public EvaluationResult evaluate(AgentExecutionTrace trace) {
        Map<String, Double> dimensions = new HashMap<>();
        List<String> issues = new ArrayList<>();

        // 1. Success rate
        boolean success = trace.finalState() == AgentExecutionState.COMPLETED;
        dimensions.put("success_rate", success ? 1.0 : 0.0);

        // 2. Efficiency (iteration count)
        double efficiency = Math.max(0, 1.0 - ((double) trace.iterationCount() / MAX_ITERATIONS));
        dimensions.put("efficiency", efficiency);
        if (trace.iterationCount() > HIGH_ITERATION_THRESHOLD) {
            issues.add("High iteration count: " + trace.iterationCount());
        }

        // 3. Token efficiency
        double tokenEfficiency = calculateTokenEfficiency(trace);
        dimensions.put("token_efficiency", tokenEfficiency);
        if (tokenEfficiency < LOW_TOKEN_EFFICIENCY_THRESHOLD && trace.totalTokens() > 0) {
            issues.add("Low token efficiency: " + String.format("%.2f", tokenEfficiency));
        }

        // 4. Error rate
        double errorRate = calculateErrorRate(trace);
        dimensions.put("error_rate", 1.0 - errorRate);
        if (errorRate > HIGH_ERROR_RATE_THRESHOLD) {
            issues.add("High error rate: " + String.format("%.2f", errorRate));
        }

        // 5. Latency
        double latencyScore = calculateLatencyScore(trace);
        dimensions.put("latency", latencyScore);

        // Overall score = average of all dimensions
        double overallScore = dimensions.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return new EvaluationResult(
                trace.executionId(),
                overallScore,
                dimensions,
                issues,
                Instant.now()
        );
    }

    @Override
    public EvaluationDelta compare(EvaluationResult baseline, EvaluationResult current) {
        Map<String, Double> dimensionDeltas = new HashMap<>();

        for (String dim : baseline.dimensions().keySet()) {
            double baseValue = baseline.dimensions().getOrDefault(dim, 0.0);
            double currentValue = current.dimensions().getOrDefault(dim, 0.0);
            dimensionDeltas.put(dim, currentValue - baseValue);
        }

        double overallDelta = current.overallScore() - baseline.overallScore();

        return new EvaluationDelta(
                overallDelta,
                dimensionDeltas,
                determineTrend(overallDelta)
        );
    }

    @Override
    public String getName() {
        return "RuleBasedEvaluator";
    }

    private double calculateTokenEfficiency(AgentExecutionTrace trace) {
        if (trace.totalTokens() == 0) {
            return 0.0;
        }
        return (double) trace.outputTokens() / trace.totalTokens();
    }

    private double calculateErrorRate(AgentExecutionTrace trace) {
        if (trace.toolCalls() == null || trace.toolCalls().isEmpty()) {
            return 0.0;
        }
        long errorCount = trace.toolCalls().stream()
                .filter(tc -> tc.result() != null && !tc.result().success())
                .count();
        return (double) errorCount / trace.toolCalls().size();
    }

    private double calculateLatencyScore(AgentExecutionTrace trace) {
        long latencyMs = trace.duration().toMillis();
        if (latencyMs < 1000) return 1.0;
        if (latencyMs < 5000) return 0.8;
        if (latencyMs < 10000) return 0.6;
        if (latencyMs < 30000) return 0.4;
        return 0.2;
    }

    private EvaluationTrend determineTrend(double overallDelta) {
        if (overallDelta > IMPROVEMENT_THRESHOLD) return EvaluationTrend.IMPROVING;
        if (overallDelta < DECLINE_THRESHOLD) return EvaluationTrend.DECLINING;
        return EvaluationTrend.STABLE;
    }
}
