package com.schemaplexai.agent.engine.memory.compaction;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.model.LlmMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ToolResultCompactionStrategy")
class ToolResultCompactionTest {

    private ToolResultCompactionStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ToolResultCompactionStrategy();
    }

    @Test
    @DisplayName("should return noop when token count is under threshold")
    void shouldReturnNoOpWhenUnderThreshold() {
        TokenBudget budget = new TokenBudget(1_000_000, 100_000);
        List<LlmMessage> messages = List.of(
            new LlmMessage("user", "Hello"),
            new LlmMessage("assistant", "Hi there")
        );

        CompactionResult result = strategy.compact("conv-1", messages, budget);

        assertThat(result.success()).isTrue();
        assertThat(result.noOp()).isTrue();
        assertThat(result.messages()).isNull();
        assertThat(result.strategyName()).isNull();
        assertThat(result.failureReason()).isNull();
    }

    @Test
    @DisplayName("should clear old tool results when over threshold")
    void shouldClearOldToolResults() {
        TokenBudget budget = new TokenBudget(1_000_000, 100_000);
        List<LlmMessage> messages = new ArrayList<>();
        // Add non-tool messages
        messages.add(new LlmMessage("user", "Question 1"));
        messages.add(new LlmMessage("assistant", "Answer 1"));
        // Add tool messages - 5 tool results, each ~120k tokens (480k chars)
        for (int i = 0; i < 5; i++) {
            messages.add(new LlmMessage("tool", "x".repeat(480_000)));
        }

        CompactionResult result = strategy.compact("conv-1", messages, budget);

        assertThat(result.success()).isTrue();
        assertThat(result.noOp()).isFalse();
        assertThat(result.strategyName()).isEqualTo("tool_result_cleanup");

        List<LlmMessage> compacted = result.messages();
        assertThat(compacted).hasSize(messages.size());

        // The last 3 tool messages should be preserved
        assertThat(compacted.get(compacted.size() - 1).getContent()).isEqualTo("x".repeat(480_000));
        assertThat(compacted.get(compacted.size() - 2).getContent()).isEqualTo("x".repeat(480_000));
        assertThat(compacted.get(compacted.size() - 3).getContent()).isEqualTo("x".repeat(480_000));

        // The first 2 tool messages should be cleared
        assertThat(compacted.get(2).getContent()).isEqualTo("[cleared: tool result]");
        assertThat(compacted.get(3).getContent()).isEqualTo("[cleared: tool result]");
    }

    @Test
    @DisplayName("should keep non-tool messages intact")
    void shouldKeepNonToolMessages() {
        TokenBudget budget = new TokenBudget(1_000_000, 100_000);
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", "System prompt"));
        messages.add(new LlmMessage("user", "User question"));
        messages.add(new LlmMessage("assistant", "Assistant response"));
        // Add tool messages that push us over threshold
        for (int i = 0; i < 5; i++) {
            messages.add(new LlmMessage("tool", "x".repeat(480_000)));
        }

        CompactionResult result = strategy.compact("conv-1", messages, budget);

        assertThat(result.success()).isTrue();
        assertThat(result.noOp()).isFalse();

        List<LlmMessage> compacted = result.messages();
        assertThat(compacted.get(0).getContent()).isEqualTo("System prompt");
        assertThat(compacted.get(0).getRole()).isEqualTo("system");
        assertThat(compacted.get(1).getContent()).isEqualTo("User question");
        assertThat(compacted.get(1).getRole()).isEqualTo("user");
        assertThat(compacted.get(2).getContent()).isEqualTo("Assistant response");
        assertThat(compacted.get(2).getRole()).isEqualTo("assistant");
    }

    @Test
    @DisplayName("getName() should return 'tool_result_cleanup'")
    void getNameShouldReturnToolResultCleanup() {
        assertThat(strategy.getName()).isEqualTo("tool_result_cleanup");
    }
}
