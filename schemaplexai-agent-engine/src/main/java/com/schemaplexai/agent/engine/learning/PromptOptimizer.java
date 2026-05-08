package com.schemaplexai.agent.engine.learning;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Service that analyzes prompt performance patterns and suggests optimizations.
 * Calculates efficiency scores to identify underperforming prompt templates.
 */
@Slf4j
@Service
public class PromptOptimizer {

    private static final double LATENCY_THRESHOLD_MS = 2000.0;
    private static final double SUCCESS_RATE_THRESHOLD = 0.85;
    private static final double EFFICIENCY_THRESHOLD = 0.7;

    /**
     * Suggests an optimization strategy for the given prompt template based on its performance data.
     *
     * @param promptTemplateId the identifier of the prompt template
     * @return a human-readable optimization suggestion
     */
    public String suggestOptimization(String promptTemplateId) {
        if (promptTemplateId == null || promptTemplateId.isBlank()) {
            return "No optimization available: promptTemplateId is required.";
        }
        // Without historical data, return a generic suggestion.
        // In production this would look up the PromptPerformancePattern for the template.
        return String.format(Locale.ROOT,
                "Review prompt template '%s': consider reducing context length, adding examples, or refining instructions.",
                promptTemplateId);
    }

    /**
     * Suggests an optimization for a specific performance pattern.
     *
     * @param pattern the performance pattern to analyze
     * @return a targeted optimization suggestion
     */
    public String suggestOptimization(PromptPerformancePattern pattern) {
        if (pattern == null) {
            return "No optimization available: performance pattern is null.";
        }

        StringBuilder suggestion = new StringBuilder();
        suggestion.append(String.format(Locale.ROOT, "Prompt template '%s' analysis:", pattern.promptTemplateId()));

        if (pattern.avgLatencyMs() > LATENCY_THRESHOLD_MS) {
            suggestion.append(String.format(Locale.ROOT,
                    " High latency (%.1f ms) — consider simplifying the prompt or splitting into smaller steps.",
                    pattern.avgLatencyMs()));
        }
        if (pattern.successRate() < SUCCESS_RATE_THRESHOLD) {
            suggestion.append(String.format(Locale.ROOT,
                    " Low success rate (%.1f%%) — add more examples, clarify instructions, or tighten output schema.",
                    pattern.successRate() * 100));
        }
        if (pattern.tokenEfficiencyScore() < EFFICIENCY_THRESHOLD) {
            suggestion.append(String.format(Locale.ROOT,
                    " Poor token efficiency (%.2f) — reduce redundant context and use concise formatting.",
                    pattern.tokenEfficiencyScore()));
        }

        String expectedPrefix = String.format(Locale.ROOT, "Prompt template '%s' analysis:", pattern.promptTemplateId());
        if (suggestion.toString().trim().equals(expectedPrefix)) {
            suggestion.append(" Performance is within acceptable thresholds.");
        }

        return suggestion.toString().trim();
    }

    /**
     * Calculates a composite efficiency score for the given performance pattern.
     * Score is in range [0.0, 1.0] where higher is better.
     *
     * @param pattern the performance pattern
     * @return efficiency score between 0.0 and 1.0
     */
    public double calculateEfficiencyScore(PromptPerformancePattern pattern) {
        if (pattern == null) {
            return 0.0;
        }

        double latencyScore = normalizeLatency(pattern.avgLatencyMs());
        double successScore = pattern.successRate();
        double tokenScore = pattern.tokenEfficiencyScore();

        // Weighted composite: success matters most, then token efficiency, then latency.
        double composite = (successScore * 0.5) + (tokenScore * 0.3) + (latencyScore * 0.2);
        double clamped = Math.max(0.0, Math.min(1.0, composite));
        log.debug("Calculated efficiency score={:.3f} for promptTemplateId={}", clamped, pattern.promptTemplateId());
        return clamped;
    }

    private double normalizeLatency(double avgLatencyMs) {
        if (avgLatencyMs <= 0) {
            return 1.0;
        }
        if (avgLatencyMs >= LATENCY_THRESHOLD_MS) {
            return 0.0;
        }
        return 1.0 - (avgLatencyMs / LATENCY_THRESHOLD_MS);
    }
}
