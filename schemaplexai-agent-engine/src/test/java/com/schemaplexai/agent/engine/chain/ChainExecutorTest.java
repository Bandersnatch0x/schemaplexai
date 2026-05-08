package com.schemaplexai.agent.engine.chain;

import com.schemaplexai.agent.engine.model.AiModelRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChainExecutorTest {

    @Mock
    private AiModelRouter aiModelRouter;

    private ChainExecutor chainExecutor;

    private static final String MODEL_ID = "gpt-4";

    @BeforeEach
    void setUp() {
        chainExecutor = new ChainExecutor(aiModelRouter);
    }

    @Test
    void shouldExecuteSingleStepChainSuccessfully() {
        when(aiModelRouter.generateWithFallback(eq("Hello World"), eq(MODEL_ID), eq(0.7)))
                .thenReturn("Step 1 output");

        ChainStep step = new ChainStep("s1", "Step 1", "Hello World", null, "out1", 0, 0.7);
        ChainDefinition chain = new ChainDefinition("c1", "Test Chain", "desc", List.of(step), Map.of());

        ChainExecutionResult result = chainExecutor.execute(chain, MODEL_ID);

        assertThat(result.success()).isTrue();
        assertThat(result.chainId()).isEqualTo("c1");
        assertThat(result.allOutputs()).containsEntry("out1", "Step 1 output");
        assertThat(result.failedStepId()).isNull();
        assertThat(result.errorMessage()).isNull();
        assertThat(result.totalDuration()).isNotNull();
    }

    @Test
    void shouldExecuteMultiStepChainSequentially() {
        when(aiModelRouter.generateWithFallback(eq("Analyze: test data"), eq(MODEL_ID), eq(0.7)))
                .thenReturn("analysis result");
        when(aiModelRouter.generateWithFallback(contains("Summarize: analysis result"), eq(MODEL_ID), eq(0.5)))
                .thenReturn("summary result");

        ChainStep step1 = new ChainStep("s1", "Analyze", "Analyze: {input}", null, "analysis", 0, 0.7);
        ChainStep step2 = new ChainStep("s2", "Summarize", "Summarize: {analysis}", null, "summary", 0, 0.5);

        ChainDefinition chain = new ChainDefinition(
                "c1", "Analysis Chain", "desc",
                List.of(step1, step2),
                Map.of("input", "test data")
        );

        ChainExecutionResult result = chainExecutor.execute(chain, MODEL_ID);

        assertThat(result.success()).isTrue();
        assertThat(result.allOutputs())
                .containsEntry("analysis", "analysis result")
                .containsEntry("summary", "summary result");
    }

    @Test
    void shouldPassStepOutputToDownstreamViaInputMapping() {
        when(aiModelRouter.generateWithFallback(eq("Generate code"), eq(MODEL_ID), eq(0.7)))
                .thenReturn("function hello() {}");
        when(aiModelRouter.generateWithFallback(contains("Review: function hello() {}"), eq(MODEL_ID), eq(0.5)))
                .thenReturn("Looks good");

        ChainStep step1 = new ChainStep("s1", "Generate", "Generate code", null, "code", 0, 0.7);
        ChainStep step2 = new ChainStep("s2", "Review", "Review: {review_target}", "code -> {review_target}", "review", 0, 0.5);

        ChainDefinition chain = new ChainDefinition(
                "c1", "Code Chain", "desc",
                List.of(step1, step2),
                Map.of()
        );

        ChainExecutionResult result = chainExecutor.execute(chain, MODEL_ID);

        assertThat(result.success()).isTrue();
        assertThat(result.allOutputs()).containsEntry("review", "Looks good");
    }

    @Test
    void shouldRetryOnStepFailureAndSucceed() {
        when(aiModelRouter.generateWithFallback(eq("prompt"), eq(MODEL_ID), eq(0.7)))
                .thenThrow(new RuntimeException("Temporary failure"))
                .thenReturn("recovered output");

        ChainStep step = new ChainStep("s1", "Failing Step", "prompt", null, "out1", 2, 0.7);
        ChainDefinition chain = new ChainDefinition("c1", "Retry Chain", "desc", List.of(step), Map.of());

        ChainExecutionResult result = chainExecutor.execute(chain, MODEL_ID);

        assertThat(result.success()).isTrue();
        assertThat(result.allOutputs()).containsEntry("out1", "recovered output");
        verify(aiModelRouter, times(2)).generateWithFallback(eq("prompt"), eq(MODEL_ID), eq(0.7));
    }

    @Test
    void shouldFailChainWhenAllRetriesExhausted() {
        when(aiModelRouter.generateWithFallback(eq("prompt"), eq(MODEL_ID), eq(0.7)))
                .thenThrow(new RuntimeException("Persistent failure"));

        ChainStep step = new ChainStep("s1", "Failing Step", "prompt", null, "out1", 1, 0.7);
        ChainDefinition chain = new ChainDefinition("c1", "Fail Chain", "desc", List.of(step), Map.of());

        ChainExecutionResult result = chainExecutor.execute(chain, MODEL_ID);

        assertThat(result.success()).isFalse();
        assertThat(result.failedStepId()).isEqualTo("s1");
        assertThat(result.errorMessage()).contains("Failing Step");
        assertThat(result.errorMessage()).contains("1 retries");
        // 1 maxRetries + 1 initial = 2 attempts
        verify(aiModelRouter, times(2)).generateWithFallback(eq("prompt"), eq(MODEL_ID), eq(0.7));
    }

    @Test
    void shouldStopAtFailedStepAndPreservePreviousOutputs() {
        when(aiModelRouter.generateWithFallback(eq("Step 1 prompt"), eq(MODEL_ID), eq(0.7)))
                .thenReturn("step 1 output");
        when(aiModelRouter.generateWithFallback(eq("Step 2 prompt"), eq(MODEL_ID), eq(0.7)))
                .thenThrow(new RuntimeException("Step 2 fails"));

        ChainStep step1 = new ChainStep("s1", "Step 1", "Step 1 prompt", null, "out1", 0, 0.7);
        ChainStep step2 = new ChainStep("s2", "Step 2", "Step 2 prompt", null, "out2", 0, 0.7);

        ChainDefinition chain = new ChainDefinition(
                "c1", "Partial Chain", "desc",
                List.of(step1, step2),
                Map.of()
        );

        ChainExecutionResult result = chainExecutor.execute(chain, MODEL_ID);

        assertThat(result.success()).isFalse();
        assertThat(result.failedStepId()).isEqualTo("s2");
        // Step 1 output should still be present
        assertThat(result.allOutputs()).containsEntry("out1", "step 1 output");
        assertThat(result.allOutputs()).doesNotContainKey("out2");
    }

    @Test
    void shouldHandleChainWithZeroRetries() {
        when(aiModelRouter.generateWithFallback(eq("prompt"), eq(MODEL_ID), eq(0.7)))
                .thenThrow(new RuntimeException("Fail"));

        ChainStep step = new ChainStep("s1", "No Retry", "prompt", null, "out1", 0, 0.7);
        ChainDefinition chain = new ChainDefinition("c1", "No Retry Chain", "desc", List.of(step), Map.of());

        ChainExecutionResult result = chainExecutor.execute(chain, MODEL_ID);

        assertThat(result.success()).isFalse();
        assertThat(result.failedStepId()).isEqualTo("s1");
        // 0 maxRetries + 1 initial = 1 attempt
        verify(aiModelRouter, times(1)).generateWithFallback(eq("prompt"), eq(MODEL_ID), eq(0.7));
    }

    @Test
    void shouldResolveInitialInputsInTemplates() {
        when(aiModelRouter.generateWithFallback(eq("Analyze: product feedback"), eq(MODEL_ID), eq(0.7)))
                .thenReturn("analysis");

        ChainStep step = new ChainStep("s1", "Analyze", "Analyze: {topic}", null, "result", 0, 0.7);
        ChainDefinition chain = new ChainDefinition(
                "c1", "Input Chain", "desc",
                List.of(step),
                Map.of("topic", "product feedback")
        );

        ChainExecutionResult result = chainExecutor.execute(chain, MODEL_ID);

        assertThat(result.success()).isTrue();
        assertThat(result.allOutputs()).containsEntry("result", "analysis");
    }

    @Test
    void shouldPassCorrectTemperaturePerStep() {
        when(aiModelRouter.generateWithFallback(anyString(), eq(MODEL_ID), eq(0.1)))
                .thenReturn("precise");
        when(aiModelRouter.generateWithFallback(anyString(), eq(MODEL_ID), eq(0.9)))
                .thenReturn("creative");

        ChainStep preciseStep = new ChainStep("s1", "Precise", "prompt", null, "out1", 0, 0.1);
        ChainStep creativeStep = new ChainStep("s2", "Creative", "prompt", null, "out2", 0, 0.9);

        ChainDefinition chain = new ChainDefinition(
                "c1", "Temp Chain", "desc",
                List.of(preciseStep, creativeStep),
                Map.of()
        );

        ChainExecutionResult result = chainExecutor.execute(chain, MODEL_ID);

        assertThat(result.success()).isTrue();
        verify(aiModelRouter).generateWithFallback(anyString(), eq(MODEL_ID), eq(0.1));
        verify(aiModelRouter).generateWithFallback(anyString(), eq(MODEL_ID), eq(0.9));
    }

    @Test
    void shouldHandleEmptyChain() {
        ChainDefinition chain = new ChainDefinition("c1", "Empty Chain", "desc", List.of(), Map.of());

        ChainExecutionResult result = chainExecutor.execute(chain, MODEL_ID);

        assertThat(result.success()).isTrue();
        assertThat(result.allOutputs()).isEmpty();
        verifyNoInteractions(aiModelRouter);
    }

    @Test
    void shouldApplyInputMappingFromMultipleSources() {
        when(aiModelRouter.generateWithFallback(eq("Step A"), eq(MODEL_ID), eq(0.7)))
                .thenReturn("output A");
        when(aiModelRouter.generateWithFallback(eq("Step B"), eq(MODEL_ID), eq(0.7)))
                .thenReturn("output B");
        when(aiModelRouter.generateWithFallback(contains("output A"), anyString(), anyDouble()))
                .thenReturn("combined");

        ChainStep stepA = new ChainStep("s1", "A", "Step A", null, "stepA", 0, 0.7);
        ChainStep stepB = new ChainStep("s2", "B", "Step B", null, "stepB", 0, 0.7);
        ChainStep stepC = new ChainStep("s3", "C", "{mapped_a} and {mapped_b}",
                "stepA -> {mapped_a}; stepB -> {mapped_b}", "final", 0, 0.7);

        ChainDefinition chain = new ChainDefinition(
                "c1", "Multi Map Chain", "desc",
                List.of(stepA, stepB, stepC),
                Map.of()
        );

        ChainExecutionResult result = chainExecutor.execute(chain, MODEL_ID);

        assertThat(result.success()).isTrue();
        assertThat(result.allOutputs()).containsKey("final");
    }
}
