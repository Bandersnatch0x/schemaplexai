package com.schemaplexai.agent.engine.evaluation;

import com.schemaplexai.agent.engine.shadow.AgentLoopShadowReviewService;
import com.schemaplexai.agent.engine.shadow.FeedbackAction;
import com.schemaplexai.agent.engine.shadow.FeedbackActionType;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ShadowReviewEvaluatorTest {

    @Mock
    private AgentLoopShadowReviewService shadowReviewService;

    private ShadowReviewEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ShadowReviewEvaluator(shadowReviewService);
    }

    @Test
    void shouldReturnCorrectName() {
        assertThat(evaluator.getName()).isEqualTo("ShadowReviewEvaluator");
    }

    @Test
    void shouldScoreSuccessfulExecutionHigh() {
        AgentExecutionTrace trace = new AgentExecutionTrace(
                "exec-1",
                AgentExecutionState.COMPLETED,
                2, 1000, 400,
                Duration.ofMillis(500),
                List.of(
                        new ToolCallTrace("calc", ToolExecutionResult.success("calc", "42", 100, 50))
                )
        );

        EvaluationResult result = evaluator.evaluate(trace);

        assertThat(result.overallScore()).isGreaterThan(0.7);
        assertThat(result.dimensions()).containsKey("success_rate");
        assertThat(result.dimensions()).containsKey("shadow_acceptance");
        assertThat(result.dimensions().get("success_rate")).isEqualTo(1.0);
        assertThat(result.dimensions().get("shadow_acceptance")).isEqualTo(1.0);
    }

    @Test
    void shouldScoreFailedExecutionLow() {
        AgentExecutionTrace trace = new AgentExecutionTrace(
                "exec-2",
                AgentExecutionState.FAILED,
                3, 500, 200,
                Duration.ofSeconds(2),
                List.of()
        );

        EvaluationResult result = evaluator.evaluate(trace);

        assertThat(result.dimensions().get("success_rate")).isEqualTo(0.0);
    }

    @Test
    void shouldDetectHighIterationCount() {
        AgentExecutionTrace trace = new AgentExecutionTrace(
                "exec-3",
                AgentExecutionState.COMPLETED,
                8, 5000, 1000,
                Duration.ofSeconds(10),
                List.of()
        );

        EvaluationResult result = evaluator.evaluate(trace);

        assertThat(result.issues()).anyMatch(i -> i.contains("iteration"));
    }

    @Test
    void shouldCalculateToolReliability() {
        AgentExecutionTrace trace = new AgentExecutionTrace(
                "exec-4",
                AgentExecutionState.COMPLETED,
                2, 1000, 400,
                Duration.ofSeconds(1),
                List.of(
                        new ToolCallTrace("tool1", ToolExecutionResult.success("tool1", "ok", 100, 10)),
                        new ToolCallTrace("tool2", ToolExecutionResult.failure("tool2",
                                ToolErrorCategory.INTERNAL_ERROR, "fail", 100, 0))
                )
        );

        EvaluationResult result = evaluator.evaluate(trace);

        // 1 success out of 2 = 0.5 reliability
        assertThat(result.dimensions().get("tool_reliability")).isEqualTo(0.5);
        assertThat(result.issues()).anyMatch(i -> i.contains("reliability"));
    }

    @Test
    void shouldReturnPerfectReliabilityWhenNoToolCalls() {
        AgentExecutionTrace trace = new AgentExecutionTrace(
                "exec-5",
                AgentExecutionState.COMPLETED,
                1, 500, 200,
                Duration.ofMillis(300),
                List.of()
        );

        EvaluationResult result = evaluator.evaluate(trace);

        assertThat(result.dimensions().get("tool_reliability")).isEqualTo(1.0);
    }

    @Test
    void shouldEvaluateWithShadowConfigAccept() {
        AgentExecutionTrace trace = new AgentExecutionTrace(
                "exec-6",
                AgentExecutionState.COMPLETED,
                2, 1000, 400,
                Duration.ofMillis(500),
                List.of()
        );
        List<FeedbackAction> actions = List.of(
                FeedbackAction.builder().type(FeedbackActionType.ACCEPT).description("Looks good").build()
        );

        EvaluationResult result = evaluator.evaluateWithShadowConfig(trace, actions);

        assertThat(result.dimensions().get("shadow_acceptance")).isEqualTo(1.0);
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void shouldPenalizeShadowEscalation() {
        AgentExecutionTrace trace = new AgentExecutionTrace(
                "exec-7",
                AgentExecutionState.COMPLETED,
                2, 1000, 400,
                Duration.ofMillis(500),
                List.of()
        );
        List<FeedbackAction> actions = List.of(
                FeedbackAction.builder().type(FeedbackActionType.ESCALATE).description("Needs human review").build(),
                FeedbackAction.builder().type(FeedbackActionType.ACCEPT).description("OK").build()
        );

        EvaluationResult result = evaluator.evaluateWithShadowConfig(trace, actions);

        assertThat(result.dimensions().get("shadow_acceptance")).isEqualTo(0.5);
        assertThat(result.issues()).anyMatch(i -> i.contains("escalation"));
    }

    @Test
    void shouldReportShadowRetry() {
        AgentExecutionTrace trace = new AgentExecutionTrace(
                "exec-8",
                AgentExecutionState.COMPLETED,
                2, 1000, 400,
                Duration.ofMillis(500),
                List.of()
        );
        List<FeedbackAction> actions = List.of(
                FeedbackAction.builder().type(FeedbackActionType.RETRY).description("Try again").build()
        );

        EvaluationResult result = evaluator.evaluateWithShadowConfig(trace, actions);

        assertThat(result.dimensions().get("shadow_acceptance")).isEqualTo(0.0);
        assertThat(result.issues()).anyMatch(i -> i.contains("retry"));
    }

    @Test
    void shouldCompareWithImprovingTrend() {
        EvaluationResult baseline = new EvaluationResult(
                "exec-1", 0.5,
                Map.of("success_rate", 0.0, "efficiency", 0.5, "tool_reliability", 0.5, "shadow_acceptance", 1.0),
                List.of(), java.time.Instant.now()
        );
        EvaluationResult current = new EvaluationResult(
                "exec-2", 0.8,
                Map.of("success_rate", 1.0, "efficiency", 0.8, "tool_reliability", 0.8, "shadow_acceptance", 1.0),
                List.of(), java.time.Instant.now()
        );

        EvaluationDelta delta = evaluator.compare(baseline, current);

        assertThat(delta.trend()).isEqualTo(EvaluationTrend.IMPROVING);
        assertThat(delta.overallDelta()).isGreaterThan(0.0);
    }

    @Test
    void shouldCompareWithDecliningTrend() {
        EvaluationResult baseline = new EvaluationResult(
                "exec-1", 0.8,
                Map.of("success_rate", 1.0, "efficiency", 0.8, "tool_reliability", 0.8, "shadow_acceptance", 1.0),
                List.of(), java.time.Instant.now()
        );
        EvaluationResult current = new EvaluationResult(
                "exec-2", 0.4,
                Map.of("success_rate", 0.0, "efficiency", 0.4, "tool_reliability", 0.4, "shadow_acceptance", 0.5),
                List.of("issue"), java.time.Instant.now()
        );

        EvaluationDelta delta = evaluator.compare(baseline, current);

        assertThat(delta.trend()).isEqualTo(EvaluationTrend.DECLINING);
    }
}
