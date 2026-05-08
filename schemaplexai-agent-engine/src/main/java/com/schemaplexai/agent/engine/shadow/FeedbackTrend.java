package com.schemaplexai.agent.engine.shadow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Aggregated feedback trend for an agent over a time window.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackTrend {

    private Long agentId;
    private long totalCount;
    private long acceptCount;
    private long retryCount;
    private long escalateCount;
    private long modifyPromptCount;
    private long skipCount;
    private double acceptanceRate;
    private double escalationRate;

    /**
     * Breakdown of action type counts.
     */
    private Map<FeedbackActionType, Long> actionCounts;
}
