package com.schemaplexai.agent.engine.exploration;

/**
 * Record representing the outcome of a single strategy experiment.
 *
 * @param strategyName   the name of the strategy tested
 * @param successRate    proportion of successful runs, in range [0.0, 1.0]
 * @param avgLatencyMs   average latency in milliseconds
 * @param avgTokenUsage  average number of tokens consumed per run
 * @param score          composite score calculated from the metrics (higher is better)
 */
public record ExperimentResult(
        String strategyName,
        double successRate,
        double avgLatencyMs,
        double avgTokenUsage,
        double score
) {
}
