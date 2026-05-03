package com.schemaplexai.agent.engine.evaluation;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record EvaluationResult(
    String executionId,
    double overallScore,
    Map<String, Double> dimensions,
    List<String> issues,
    Instant evaluatedAt
) {
}
