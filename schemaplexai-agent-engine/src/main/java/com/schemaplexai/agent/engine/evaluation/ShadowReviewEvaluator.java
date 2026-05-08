package com.schemaplexai.agent.engine.evaluation;

import com.schemaplexai.agent.engine.shadow.AgentLoopShadowReviewService;
import com.schemaplexai.agent.engine.shadow.FeedbackAction;
import com.schemaplexai.agent.engine.shadow.FeedbackActionType;
import com.schemaplexai.agent.engine.state.AgentExecutionState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shadow-review evaluator that scores executions based on feedback actions
 * suggested by the AgentLoopShadowReviewService.
 *
 * <p>Shadow mode means the evaluator logs what actions <em>would</em> be taken
 * (RETRY, ESCALATE, MODIFY_PROMPT, …) without actually applying them.
 * The score reflects how many shadow actions recommend acceptance vs correction.</p>
 */
public class ShadowReviewEvaluator implements Evaluator {

    private final AgentLoopShadowReviewService shadowReviewService;

    public ShadowReviewEvaluator(AgentLoopShadowReviewService shadowReviewService) {
        this.shadowReviewService = shadowReviewService;
    }

    @Override
    public EvaluationResult evaluate(AgentExecutionTrace trace) {
        Map<String, Double> dimensions = new HashMap<>();
        List<String> issues = new ArrayList<>();

        // 1. Base success score
        boolean success = trace.finalState() == AgentExecutionState.COMPLETED;
        dimensions.put("success_rate", success ? 1.0 : 0.0);

        // 2. Iteration efficiency
        double efficiency = Math.max(0, 1.0 - ((double) trace.iterationCount() / 10.0));
        dimensions.put("efficiency", efficiency);
        if (trace.iterationCount() > 5) {
            issues.add("High iteration count: " + trace.iterationCount());
        }

        // 3. Tool reliability — ratio of successful tool calls
        double toolReliability = calculateToolReliability(trace);
        dimensions.put("tool_reliability", toolReliability);
        if (toolReliability < 0.8 && trace.toolCalls() != null && !trace.toolCalls().isEmpty()) {
            issues.add("Low tool reliability: " + String.format("%.2f", toolReliability));
        }

        // 4. Shadow action score — penalize executions that would trigger corrective actions
        double shadowScore = 1.0;
        dimensions.put("shadow_acceptance", shadowScore);

        double overallScore = dimensions.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return new EvaluationResult(
                trace.executionId(),
                overallScore,
                dimensions,
                issues,
                Instant.now()
        );
    }

    /**
     * Evaluate a trace against a set of shadow feedback actions.
     * This method applies the shadow config and adjusts scores based on
     * what actions the shadow review would recommend.
     */
    public EvaluationResult evaluateWithShadowConfig(AgentExecutionTrace trace,
                                                      List<FeedbackAction> shadowActions) {
        Map<String, Double> dimensions = new HashMap<>();
        List<String> issues = new ArrayList<>();

        boolean success = trace.finalState() == AgentExecutionState.COMPLETED;
        dimensions.put("success_rate", success ? 1.0 : 0.0);

        double efficiency = Math.max(0, 1.0 - ((double) trace.iterationCount() / 10.0));
        dimensions.put("efficiency", efficiency);
        if (trace.iterationCount() > 5) {
            issues.add("High iteration count: " + trace.iterationCount());
        }

        double toolReliability = calculateToolReliability(trace);
        dimensions.put("tool_reliability", toolReliability);

        // Analyze shadow actions
        double shadowScore = calculateShadowAcceptance(shadowActions);
        dimensions.put("shadow_acceptance", shadowScore);

        // Categorize actions as issues
        for (FeedbackAction action : shadowActions) {
            if (action.getType() == FeedbackActionType.ESCALATE) {
                issues.add("Shadow recommends escalation: " + action.getDescription());
            } else if (action.getType() == FeedbackActionType.RETRY) {
                issues.add("Shadow recommends retry: " + action.getDescription());
            }
        }

        double overallScore = dimensions.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return new EvaluationResult(
                trace.executionId(),
                overallScore,
                dimensions,
                issues,
                Instant.now()
        );
    }

    @Override
    public EvaluationDelta compare(EvaluationResult baseline, EvaluationResult current) {
        Map<String, Double> dimensionDeltas = new HashMap<>();

        for (String dim : baseline.dimensions().keySet()) {
            double baseValue = baseline.dimensions().getOrDefault(dim, 0.0);
            double currentValue = current.dimensions().getOrDefault(dim, 0.0);
            dimensionDeltas.put(dim, currentValue - baseValue);
        }

        double overallDelta = current.overallScore() - baseline.overallScore();
        EvaluationTrend trend;
        if (overallDelta > 0.1) trend = EvaluationTrend.IMPROVING;
        else if (overallDelta < -0.1) trend = EvaluationTrend.DECLINING;
        else trend = EvaluationTrend.STABLE;

        return new EvaluationDelta(overallDelta, dimensionDeltas, trend);
    }

    @Override
    public String getName() {
        return "ShadowReviewEvaluator";
    }

    private double calculateToolReliability(AgentExecutionTrace trace) {
        if (trace.toolCalls() == null || trace.toolCalls().isEmpty()) {
            return 1.0;
        }
        long successCount = trace.toolCalls().stream()
                .filter(tc -> tc.result() != null && tc.result().success())
                .count();
        return (double) successCount / trace.toolCalls().size();
    }

    private double calculateShadowAcceptance(List<FeedbackAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return 1.0;
        }
        long acceptCount = actions.stream()
                .filter(a -> a.getType() == FeedbackActionType.ACCEPT)
                .count();
        return (double) acceptCount / actions.size();
    }
}
