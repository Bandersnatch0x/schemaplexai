package com.schemaplexai.agent.engine.learning;

import java.time.Instant;

/**
 * Record representing performance metrics for a prompt template.
 * Used to identify underperforming prompts and drive optimization decisions.
 */
public record PromptPerformancePattern(
        String promptTemplateId,
        double avgLatencyMs,
        double successRate,
        double tokenEfficiencyScore,
        Instant lastEvaluatedAt,
        String tenantId
) {
}
