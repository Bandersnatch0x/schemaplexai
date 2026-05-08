package com.schemaplexai.agent.engine.learning;

import com.schemaplexai.agent.engine.tool.ToolErrorCategory;

import java.time.Instant;

/**
 * Record representing a detected tool failure pattern for a specific tool and error category.
 * Used by the learning/adaptation layer to identify recurring issues and guide recovery strategies.
 */
public record ToolFailurePattern(
        String toolName,
        ToolErrorCategory errorCategory,
        long failureCount,
        Instant lastFailureTime,
        String tenantId,
        Trend trend
) {

    /**
     * Trend direction for failure patterns over time.
     */
    public enum Trend {
        INCREASING,
        STABLE,
        DECREASING
    }
}
