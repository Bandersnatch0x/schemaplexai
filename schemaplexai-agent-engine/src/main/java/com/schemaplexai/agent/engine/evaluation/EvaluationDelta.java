package com.schemaplexai.agent.engine.evaluation;

import java.util.Map;

public record EvaluationDelta(
    double overallDelta,
    Map<String, Double> dimensionDeltas,
    EvaluationTrend trend
) {
}
