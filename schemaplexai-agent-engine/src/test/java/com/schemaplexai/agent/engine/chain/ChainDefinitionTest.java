package com.schemaplexai.agent.engine.chain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChainDefinitionTest {

    @Test
    void shouldCreateChainDefinitionWithAllFields() {
        ChainStep step1 = new ChainStep("s1", "Analyze", "Analyze: {input}", null, "analysis", 2, 0.7);
        ChainStep step2 = new ChainStep("s2", "Summarize", "Summarize: {analysis}", "analysis -> {analysis}", "summary", 1, 0.5);

        ChainDefinition chain = new ChainDefinition(
                "chain-1",
                "Analysis Chain",
                "Analyzes and summarizes input",
                List.of(step1, step2),
                Map.of("input", "test data")
        );

        assertThat(chain.id()).isEqualTo("chain-1");
        assertThat(chain.name()).isEqualTo("Analysis Chain");
        assertThat(chain.description()).isEqualTo("Analyzes and summarizes input");
        assertThat(chain.steps()).hasSize(2);
        assertThat(chain.initialInputs()).containsEntry("input", "test data");
    }

    @Test
    void shouldPreserveStepOrdering() {
        ChainStep step1 = new ChainStep("s1", "First", "prompt1", null, "out1", 0, 0.7);
        ChainStep step2 = new ChainStep("s2", "Second", "prompt2", null, "out2", 0, 0.7);
        ChainStep step3 = new ChainStep("s3", "Third", "prompt3", null, "out3", 0, 0.7);

        ChainDefinition chain = new ChainDefinition(
                "chain-2", "Ordered Chain", "desc", List.of(step1, step2, step3), Map.of()
        );

        assertThat(chain.steps().get(0).name()).isEqualTo("First");
        assertThat(chain.steps().get(1).name()).isEqualTo("Second");
        assertThat(chain.steps().get(2).name()).isEqualTo("Third");
    }

    @Test
    void shouldAllowEmptyInitialInputs() {
        ChainDefinition chain = new ChainDefinition(
                "chain-3", "Empty Chain", "desc", List.of(), Map.of()
        );

        assertThat(chain.initialInputs()).isEmpty();
        assertThat(chain.steps()).isEmpty();
    }

    @Test
    void shouldHandleNullInitialInputs() {
        ChainDefinition chain = new ChainDefinition(
                "chain-4", "Null Inputs", "desc", List.of(), null
        );

        // ChainDefinition is a record, so null is allowed; ChainExecutionContext handles null
        assertThat(chain.initialInputs()).isNull();
    }

    @Test
    void shouldExposeStepProperties() {
        ChainStep step = new ChainStep("s1", "Test Step", "Hello {name}", "prev -> {name}", "result", 3, 0.9);

        assertThat(step.id()).isEqualTo("s1");
        assertThat(step.name()).isEqualTo("Test Step");
        assertThat(step.promptTemplate()).isEqualTo("Hello {name}");
        assertThat(step.inputMapping()).isEqualTo("prev -> {name}");
        assertThat(step.outputKey()).isEqualTo("result");
        assertThat(step.maxRetries()).isEqualTo(3);
        assertThat(step.temperature()).isEqualTo(0.9);
    }

    @Test
    void shouldSupportSingleStepChain() {
        ChainStep onlyStep = new ChainStep("s1", "Only", "Do something", null, "output", 0, 0.5);

        ChainDefinition chain = new ChainDefinition(
                "chain-5", "Single Step", "desc", List.of(onlyStep), Map.of()
        );

        assertThat(chain.steps()).hasSize(1);
        assertThat(chain.steps().get(0)).isEqualTo(onlyStep);
    }
}
