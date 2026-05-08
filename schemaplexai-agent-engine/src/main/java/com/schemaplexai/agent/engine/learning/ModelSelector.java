package com.schemaplexai.agent.engine.learning;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

/**
 * Service that selects the optimal LLM model for a given task based on
 * task characteristics, estimated token usage, and execution priority.
 * Uses a simple scoring function balancing cost, latency, and quality.
 */
@Slf4j
@Service
public class ModelSelector {

    /**
     * Execution priority levels influencing model selection.
     */
    public enum ExecutionPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    private static final String MODEL_GPT_4O = "gpt-4o";
    private static final String MODEL_GPT_4O_MINI = "gpt-4o-mini";
    private static final String MODEL_GPT_4_TURBO = "gpt-4-turbo";
    private static final String MODEL_CLAUDE_3_5_SONNET = "claude-3-5-sonnet";
    private static final String MODEL_CLAUDE_3_HAIKU = "claude-3-haiku";

    // Model metadata: costScore (lower = cheaper), latencyScore (higher = faster), qualityScore (higher = better)
    // Scores are normalized roughly on a 0-10 scale.
    private static final Map<String, ModelProfile> MODEL_PROFILES = Map.of(
            MODEL_GPT_4O, new ModelProfile(5, 7, 9),
            MODEL_GPT_4O_MINI, new ModelProfile(2, 9, 6),
            MODEL_GPT_4_TURBO, new ModelProfile(7, 5, 9),
            MODEL_CLAUDE_3_5_SONNET, new ModelProfile(6, 6, 9),
            MODEL_CLAUDE_3_HAIKU, new ModelProfile(2, 9, 5)
    );

    private static final int TOKEN_THRESHOLD_SMALL = 2048;
    private static final int TOKEN_THRESHOLD_LARGE = 8192;

    /**
     * Selects the best model for the given task characteristics.
     *
     * @param taskType       the type of task (e.g., "reasoning", "summarization", "code")
     * @param estimatedTokens estimated token count for the task
     * @param priority       execution priority
     * @return the selected model identifier
     */
    public String selectModelForTask(String taskType, int estimatedTokens, ExecutionPriority priority) {
        String normalizedTask = taskType == null ? "general" : taskType.toLowerCase(Locale.ROOT);
        int clampedTokens = Math.max(0, estimatedTokens);

        String bestModel = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Map.Entry<String, ModelProfile> entry : MODEL_PROFILES.entrySet()) {
            double score = calculateModelScore(entry.getValue(), normalizedTask, clampedTokens, priority);
            if (score > bestScore) {
                bestScore = score;
                bestModel = entry.getKey();
            }
        }

        if (bestModel == null) {
            bestModel = MODEL_GPT_4O;
        }

        log.info("Selected model={} for taskType={}, tokens={}, priority={}, score={:.2f}",
                bestModel, normalizedTask, clampedTokens, priority, bestScore);
        return bestModel;
    }

    /**
     * Calculates a composite score for a model profile given task constraints.
     * Higher score = better fit.
     */
    double calculateModelScore(ModelProfile profile, String taskType, int estimatedTokens, ExecutionPriority priority) {
        double qualityWeight = getQualityWeight(priority, taskType);
        double latencyWeight = getLatencyWeight(priority, estimatedTokens);
        double costWeight = getCostWeight(priority, estimatedTokens);

        double quality = profile.qualityScore() / 10.0;
        double latency = profile.latencyScore() / 10.0;
        double cost = (10 - profile.costScore()) / 10.0; // invert so higher = cheaper/better

        double tokenFactor = getTokenSizeFactor(estimatedTokens);

        return (quality * qualityWeight) + (latency * latencyWeight) + (cost * costWeight) + tokenFactor;
    }

    private double getQualityWeight(ExecutionPriority priority, String taskType) {
        double base = switch (priority) {
            case CRITICAL -> 0.6;
            case HIGH -> 0.5;
            case MEDIUM -> 0.35;
            case LOW -> 0.2;
        };
        if (taskType.contains("reasoning") || taskType.contains("code")) {
            base += 0.15;
        }
        return Math.min(1.0, base);
    }

    private double getLatencyWeight(ExecutionPriority priority, int estimatedTokens) {
        double base = switch (priority) {
            case CRITICAL -> 0.2;
            case HIGH -> 0.25;
            case MEDIUM -> 0.35;
            case LOW -> 0.4;
        };
        if (estimatedTokens < TOKEN_THRESHOLD_SMALL) {
            base += 0.1;
        }
        return Math.min(1.0, base);
    }

    private double getCostWeight(ExecutionPriority priority, int estimatedTokens) {
        double base = switch (priority) {
            case CRITICAL -> 0.05;
            case HIGH -> 0.1;
            case MEDIUM -> 0.2;
            case LOW -> 0.3;
        };
        if (estimatedTokens > TOKEN_THRESHOLD_LARGE) {
            base += 0.15;
        }
        return Math.min(1.0, base);
    }

    private double getTokenSizeFactor(int estimatedTokens) {
        if (estimatedTokens > TOKEN_THRESHOLD_LARGE) {
            return -0.1; // penalty for very large tasks on smaller models
        }
        if (estimatedTokens < TOKEN_THRESHOLD_SMALL) {
            return 0.05; // small bonus for small tasks
        }
        return 0.0;
    }

    record ModelProfile(int costScore, int latencyScore, int qualityScore) {
    }
}
