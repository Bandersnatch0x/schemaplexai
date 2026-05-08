package com.schemaplexai.agent.engine.exploration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service that simulates multiple agent strategies in a sandbox to find optimal approaches.
 * Runs experiments, compares results, and recommends the best strategy per task type.
 */
@Slf4j
@Service
public class AgentLab {

    private static final double DEFAULT_SUCCESS_RATE = 0.75;
    private static final double DEFAULT_LATENCY_MS = 1200.0;
    private static final double DEFAULT_TOKEN_USAGE = 800.0;
    private static final double LATENCY_WEIGHT = 0.25;
    private static final double SUCCESS_WEIGHT = 0.50;
    private static final double TOKEN_WEIGHT = 0.25;
    private static final double LATENCY_NORMALIZATION_MS = 5000.0;
    private static final double TOKEN_NORMALIZATION = 4000.0;

    private final Map<String, List<ExperimentResult>> experimentHistory = new ConcurrentHashMap<>();

    /**
     * Simulates running an experiment for the given task type with a list of candidate strategies.
     *
     * @param taskType   the type of task being evaluated (e.g., "summarization", "code-generation")
     * @param strategies list of strategy names to test
     * @return list of experiment results, one per strategy
     */
    public List<ExperimentResult> runExperiment(String taskType, List<String> strategies) {
        if (taskType == null || taskType.isBlank()) {
            throw new IllegalArgumentException("taskType must not be null or blank");
        }
        if (strategies == null || strategies.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedTask = taskType.toLowerCase(Locale.ROOT);
        List<ExperimentResult> results = new ArrayList<>();

        for (String strategy : strategies) {
            if (strategy == null || strategy.isBlank()) {
                continue;
            }
            ExperimentResult result = simulateStrategy(normalizedTask, strategy);
            results.add(result);
        }

        experimentHistory.put(normalizedTask, results);
        log.info("Completed experiment for taskType={} with {} strategies", normalizedTask, results.size());
        return Collections.unmodifiableList(results);
    }

    /**
     * Compares a list of experiment results and returns them sorted by score descending.
     *
     * @param results the experiment results to compare
     * @return sorted list from best to worst score
     */
    public List<ExperimentResult> compareResults(List<ExperimentResult> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }
        return results.stream()
                .sorted(Comparator.comparingDouble(ExperimentResult::score).reversed())
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Recommends the best strategy for the given task type based on historical experiments.
     *
     * @param taskType the type of task
     * @return the name of the recommended strategy, or empty string if no data exists
     */
    public String recommendStrategy(String taskType) {
        if (taskType == null || taskType.isBlank()) {
            return "";
        }
        String normalizedTask = taskType.toLowerCase(Locale.ROOT);
        List<ExperimentResult> history = experimentHistory.get(normalizedTask);
        if (history == null || history.isEmpty()) {
            return "";
        }
        return history.stream()
                .max(Comparator.comparingDouble(ExperimentResult::score))
                .map(ExperimentResult::strategyName)
                .orElse("");
    }

    private ExperimentResult simulateStrategy(String taskType, String strategy) {
        // Deterministic pseudo-random values based on hash codes for stable tests
        int seed = Objects.hash(taskType, strategy);
        Random random = new Random(seed);

        double successRate = clamp(random.nextDouble(0.4, 1.0), 0.0, 1.0);
        double latencyMs = random.nextDouble(200.0, 4000.0);
        double tokenUsage = random.nextDouble(200.0, 3000.0);

        double score = calculateScore(successRate, latencyMs, tokenUsage);
        return new ExperimentResult(strategy, successRate, latencyMs, tokenUsage, score);
    }

    double calculateScore(double successRate, double latencyMs, double tokenUsage) {
        double normalizedLatency = Math.max(0.0, 1.0 - (latencyMs / LATENCY_NORMALIZATION_MS));
        double normalizedTokens = Math.max(0.0, 1.0 - (tokenUsage / TOKEN_NORMALIZATION));
        return (successRate * SUCCESS_WEIGHT)
                + (normalizedLatency * LATENCY_WEIGHT)
                + (normalizedTokens * TOKEN_WEIGHT);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
