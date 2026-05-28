package com.schemaplexai.agent.engine.exploration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service that delegates agent strategy experiments to a configured runtime.
 * Runs experiments, compares results, and recommends the best strategy per task type.
 */
@Slf4j
@Service
public class AgentLab {

    private static final double LATENCY_WEIGHT = 0.25;
    private static final double SUCCESS_WEIGHT = 0.50;
    private static final double TOKEN_WEIGHT = 0.25;
    private static final double LATENCY_NORMALIZATION_MS = 5000.0;
    private static final double TOKEN_NORMALIZATION = 4000.0;

    private final Map<String, List<ExperimentResult>> experimentHistory = new ConcurrentHashMap<>();
    private final ExperimentRuntime experimentRuntime;

    public AgentLab() {
        this(null);
    }

    AgentLab(ExperimentRuntime experimentRuntime) {
        this.experimentRuntime = experimentRuntime;
    }

    /**
     * Runs an experiment for the given task type with a list of candidate strategies.
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

        String normalizedTask = taskType.trim().toLowerCase(Locale.ROOT);
        List<String> runnableStrategies = strategies.stream()
                .filter(strategy -> strategy != null && !strategy.isBlank())
                .toList();
        if (runnableStrategies.isEmpty()) {
            return Collections.emptyList();
        }

        if (experimentRuntime == null) {
            throw new UnsupportedOperationException("AgentLab experiment runtime is not configured.");
        }

        List<ExperimentResult> results = Optional.ofNullable(experimentRuntime.run(normalizedTask, runnableStrategies))
                .map(List::copyOf)
                .orElseGet(Collections::emptyList);
        experimentHistory.put(normalizedTask, results);
        log.info("Completed experiment for taskType={} with {} strategies", normalizedTask, results.size());
        return results;
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

    double calculateScore(double successRate, double latencyMs, double tokenUsage) {
        double normalizedLatency = Math.max(0.0, 1.0 - (latencyMs / LATENCY_NORMALIZATION_MS));
        double normalizedTokens = Math.max(0.0, 1.0 - (tokenUsage / TOKEN_NORMALIZATION));
        return (successRate * SUCCESS_WEIGHT)
                + (normalizedLatency * LATENCY_WEIGHT)
                + (normalizedTokens * TOKEN_WEIGHT);
    }

    interface ExperimentRuntime {
        List<ExperimentResult> run(String taskType, List<String> strategies);
    }
}
