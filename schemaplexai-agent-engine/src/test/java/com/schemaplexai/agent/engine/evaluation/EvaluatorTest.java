package com.schemaplexai.agent.engine.evaluation;

import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.tool.ToolExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EvaluatorTest {

    private RuleBasedEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new RuleBasedEvaluator();
    }

    @Test
    void shouldEvaluateSuccessfulExecution() {
        AgentExecutionTrace trace = new AgentExecutionTrace(
                "exec-1",
                AgentExecutionState.COMPLETED,
                2,
                1000,
                400,
                Duration.ofMillis(800),
                List.of(
                        new ToolCallTrace("calculator", ToolExecutionResult.success("calculator", "42", 100, 50)),
                        new ToolCallTrace("weather", ToolExecutionResult.success("weather", "sunny", 200, 30))
                )
        );

        EvaluationResult result = evaluator.evaluate(trace);

        assertTrue(result.overallScore() > 0.7, "Successful execution should score > 0.7, got " + result.overallScore());
        assertTrue(result.issues().isEmpty(), "Successful execution should have no issues");
        assertEquals("exec-1", result.executionId());
        assertNotNull(result.evaluatedAt());

        // Verify all five dimensions exist
        Map<String, Double> dims = result.dimensions();
        assertTrue(dims.containsKey("success_rate"));
        assertTrue(dims.containsKey("efficiency"));
        assertTrue(dims.containsKey("token_efficiency"));
        assertTrue(dims.containsKey("error_rate"));
        assertTrue(dims.containsKey("latency"));

        assertEquals(1.0, dims.get("success_rate"), 0.001);
        assertEquals(1.0, dims.get("latency"), 0.001); // < 1s = 1.0
    }

    @Test
    void shouldDetectHighIterationCount() {
        AgentExecutionTrace trace = new AgentExecutionTrace(
                "exec-2",
                AgentExecutionState.COMPLETED,
                8,
                5000,
                1000,
                Duration.ofSeconds(15),
                List.of()
        );

        EvaluationResult result = evaluator.evaluate(trace);

        assertTrue(result.issues().stream().anyMatch(i -> i.contains("iteration")),
                "Should flag high iteration count");
        assertTrue(result.dimensions().get("efficiency") < 0.5,
                "Efficiency should be low for 8 iterations");
    }

    @Test
    void shouldDetectFailedExecution() {
        AgentExecutionTrace trace = new AgentExecutionTrace(
                "exec-3",
                AgentExecutionState.FAILED,
                3,
                500,
                200,
                Duration.ofSeconds(2),
                List.of()
        );

        EvaluationResult result = evaluator.evaluate(trace);

        assertEquals(0.0, result.dimensions().get("success_rate"), 0.001,
                "Failed execution should have 0 success rate");
    }

    @Test
    void shouldDetectHighErrorRate() {
        ToolExecutionResult failedResult = ToolExecutionResult.failure(
                "tool1", com.schemaplexai.agent.engine.tool.ToolErrorCategory.INTERNAL_ERROR, "error", 100, 10);
        ToolExecutionResult successResult = ToolExecutionResult.success("tool2", "ok", 100, 10);

        AgentExecutionTrace trace = new AgentExecutionTrace(
                "exec-4",
                AgentExecutionState.COMPLETED,
                3,
                1000,
                300,
                Duration.ofSeconds(1),
                List.of(
                        new ToolCallTrace("tool1", failedResult),
                        new ToolCallTrace("tool2", successResult),
                        new ToolCallTrace("tool3", failedResult)
                )
        );

        EvaluationResult result = evaluator.evaluate(trace);

        // 2 failures out of 3 = 0.67 error rate -> error_rate dimension = 0.33
        assertTrue(result.dimensions().get("error_rate") < 0.5,
                "Error rate dimension should be low with 2/3 failures");
        assertTrue(result.issues().stream().anyMatch(i -> i.contains("error rate")),
                "Should flag high error rate");
    }

    @Test
    void shouldDetectLowTokenEfficiency() {
        AgentExecutionTrace trace = new AgentExecutionTrace(
                "exec-5",
                AgentExecutionState.COMPLETED,
                2,
                10000,
                100,
                Duration.ofSeconds(1),
                List.of()
        );

        EvaluationResult result = evaluator.evaluate(trace);

        // token efficiency = 100/10000 = 0.01, below 0.3 threshold
        assertTrue(result.issues().stream().anyMatch(i -> i.contains("token efficiency")),
                "Should flag low token efficiency");
    }

    @Test
    void shouldCalculateLatencyScore() {
        // Test different latency ranges
        AgentExecutionTrace fastTrace = createTraceWithDuration(Duration.ofMillis(500));
        AgentExecutionTrace mediumTrace = createTraceWithDuration(Duration.ofSeconds(3));
        AgentExecutionTrace slowTrace = createTraceWithDuration(Duration.ofSeconds(20));

        assertEquals(1.0, evaluator.evaluate(fastTrace).dimensions().get("latency"), 0.001);
        assertEquals(0.8, evaluator.evaluate(mediumTrace).dimensions().get("latency"), 0.001);
        assertEquals(0.4, evaluator.evaluate(slowTrace).dimensions().get("latency"), 0.001);
    }

    @Test
    void shouldCompareResultsWithImprovingTrend() {
        EvaluationResult baseline = new EvaluationResult(
                "exec-1", 0.5,
                Map.of("success_rate", 0.0, "efficiency", 0.5, "token_efficiency", 0.5, "error_rate", 0.5, "latency", 0.5),
                List.of("issue1"), java.time.Instant.now()
        );
        EvaluationResult current = new EvaluationResult(
                "exec-2", 0.8,
                Map.of("success_rate", 1.0, "efficiency", 0.8, "token_efficiency", 0.7, "error_rate", 0.8, "latency", 0.8),
                List.of(), java.time.Instant.now()
        );

        EvaluationDelta delta = evaluator.compare(baseline, current);

        assertEquals(EvaluationTrend.IMPROVING, delta.trend());
        assertTrue(delta.overallDelta() > 0);
        assertTrue(delta.dimensionDeltas().get("success_rate") > 0);
    }

    @Test
    void shouldCompareResultsWithDecliningTrend() {
        EvaluationResult baseline = new EvaluationResult(
                "exec-1", 0.8,
                Map.of("success_rate", 1.0, "efficiency", 0.8, "token_efficiency", 0.7, "error_rate", 0.8, "latency", 0.8),
                List.of(), java.time.Instant.now()
        );
        EvaluationResult current = new EvaluationResult(
                "exec-2", 0.4,
                Map.of("success_rate", 0.0, "efficiency", 0.4, "token_efficiency", 0.3, "error_rate", 0.5, "latency", 0.4),
                List.of("issue1"), java.time.Instant.now()
        );

        EvaluationDelta delta = evaluator.compare(baseline, current);

        assertEquals(EvaluationTrend.DECLINING, delta.trend());
        assertTrue(delta.overallDelta() < 0);
    }

    @Test
    void shouldCompareResultsWithStableTrend() {
        EvaluationResult baseline = new EvaluationResult(
                "exec-1", 0.7,
                Map.of("success_rate", 1.0, "efficiency", 0.7, "token_efficiency", 0.5, "error_rate", 0.7, "latency", 0.6),
                List.of(), java.time.Instant.now()
        );
        EvaluationResult current = new EvaluationResult(
                "exec-2", 0.72,
                Map.of("success_rate", 1.0, "efficiency", 0.7, "token_efficiency", 0.5, "error_rate", 0.7, "latency", 0.6),
                List.of(), java.time.Instant.now()
        );

        EvaluationDelta delta = evaluator.compare(baseline, current);

        assertEquals(EvaluationTrend.STABLE, delta.trend());
    }

    @Test
    void shouldReturnCorrectName() {
        assertEquals("RuleBasedEvaluator", evaluator.getName());
    }

    @Test
    void shouldHandleEmptyToolCalls() {
        AgentExecutionTrace trace = new AgentExecutionTrace(
                "exec-empty",
                AgentExecutionState.COMPLETED,
                1,
                500,
                200,
                Duration.ofMillis(500),
                List.of()
        );

        EvaluationResult result = evaluator.evaluate(trace);

        // No tool calls = 0 error rate = 1.0 error_rate dimension
        assertEquals(1.0, result.dimensions().get("error_rate"), 0.001);
    }

    @Test
    void shouldHandleZeroTokens() {
        AgentExecutionTrace trace = new AgentExecutionTrace(
                "exec-zero",
                AgentExecutionState.COMPLETED,
                1,
                0,
                0,
                Duration.ofMillis(500),
                List.of()
        );

        EvaluationResult result = evaluator.evaluate(trace);

        assertEquals(0.0, result.dimensions().get("token_efficiency"), 0.001);
        // Zero tokens should NOT flag low token efficiency (guard: totalTokens > 0)
        assertTrue(result.issues().stream().noneMatch(i -> i.contains("token efficiency")));
    }

    private AgentExecutionTrace createTraceWithDuration(Duration duration) {
        return new AgentExecutionTrace(
                "exec-latency",
                AgentExecutionState.COMPLETED,
                2,
                1000,
                400,
                duration,
                List.of()
        );
    }
}
