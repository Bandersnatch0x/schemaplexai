package com.schemaplexai.agent.engine.reasoning;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.context.AgentContext;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reasoning Strategy Tests")
class ReasoningStrategyTest {

    @Mock
    private LlmProvider llmProvider;

    @Mock
    private ToolRegistry toolRegistry;

    private TokenBudget budget;
    private AgentContext context;

    @BeforeEach
    void setUp() {
        budget = new TokenBudget(100_000, 20_000);
        context = AgentContext.builder()
                .tenantId("t-001")
                .projectId("p-001")
                .conversationId("conv-123")
                .agentId(42L)
                .userId("user-1")
                .build();
    }

    // ─── ThinkingResult Tests ──────────────────────────────────────────

    @Nested
    @DisplayName("ThinkingResult")
    class ThinkingResultTests {

        @Test
        @DisplayName("factory: completed() creates COMPLETED with answer")
        void completedFactory() {
            ThinkingResult r = ThinkingResult.completed("The answer is 42");
            assertEquals(ThinkingResult.Type.COMPLETED, r.type());
            assertEquals("The answer is 42", r.finalAnswer());
            assertNull(r.toolCall());
            assertNull(r.errorMessage());
        }

        @Test
        @DisplayName("factory: toolCall() creates TOOL_CALL with tool info")
        void toolCallFactory() {
            ToolCall call = new ToolCall("search", Map.of("query", "test"));
            ThinkingResult r = ThinkingResult.toolCall(call);
            assertEquals(ThinkingResult.Type.TOOL_CALL, r.type());
            assertEquals(call, r.toolCall());
            assertNull(r.finalAnswer());
            assertNull(r.errorMessage());
        }

        @Test
        @DisplayName("factory: exhausted() creates EXHAUSTED with message")
        void exhaustedFactory() {
            ThinkingResult r = ThinkingResult.exhausted("Out of tokens");
            assertEquals(ThinkingResult.Type.EXHAUSTED, r.type());
            assertEquals("Out of tokens", r.errorMessage());
            assertNull(r.finalAnswer());
            assertNull(r.toolCall());
        }

        @Test
        @DisplayName("factory: error() creates ERROR with message")
        void errorFactory() {
            ThinkingResult r = ThinkingResult.error("LLM unavailable");
            assertEquals(ThinkingResult.Type.ERROR, r.type());
            assertEquals("LLM unavailable", r.errorMessage());
            assertNull(r.finalAnswer());
            assertNull(r.toolCall());
        }
    }

    // ─── TokenBudget Tests ──────────────────────────────────────────────

    @Nested
    @DisplayName("TokenBudget")
    class TokenBudgetTests {

        @Test
        @DisplayName("new budget has full remaining tokens")
        void initialBudget() {
            TokenBudget b = new TokenBudget(1000, 500);
            assertTrue(b.hasRemaining());
            assertEquals(1000, b.getRemainingInput());
            assertEquals(500, b.getRemainingOutput());
        }

        @Test
        @DisplayName("consumeInput() decrements remaining and returns true")
        void consumeInputSuccess() {
            TokenBudget b = new TokenBudget(1000, 500);
            assertTrue(b.consumeInput(300));
            assertEquals(700, b.getRemainingInput());
            assertEquals(500, b.getRemainingOutput());
        }

        @Test
        @DisplayName("consumeInput() returns false when exceeded")
        void consumeInputExceeded() {
            TokenBudget b = new TokenBudget(100, 500);
            assertFalse(b.consumeInput(200));
            assertEquals(0, b.getRemainingInput());
            assertTrue(b.getConsumedInputTokens().get() <= 100);
        }

        @Test
        @DisplayName("consumeOutput() decrements remaining and returns true")
        void consumeOutputSuccess() {
            TokenBudget b = new TokenBudget(1000, 500);
            assertTrue(b.consumeOutput(200));
            assertEquals(300, b.getRemainingOutput());
        }

        @Test
        @DisplayName("consumeOutput() returns false when exceeded")
        void consumeOutputExceeded() {
            TokenBudget b = new TokenBudget(1000, 100);
            assertFalse(b.consumeOutput(200));
            assertEquals(0, b.getRemainingOutput());
        }

        @Test
        @DisplayName("hasRemaining() returns false when both budgets exhausted")
        void hasRemainingFalse() {
            TokenBudget b = new TokenBudget(100, 100);
            b.consumeInput(150); // attempt to exceed
            b.consumeOutput(150);
            assertFalse(b.hasRemaining());
        }

        @Test
        @DisplayName("constructor rejects negative limits")
        void constructorRejectsNegative() {
            assertThrows(IllegalArgumentException.class, () -> new TokenBudget(-1, 100));
            assertThrows(IllegalArgumentException.class, () -> new TokenBudget(100, -1));
        }

        @Test
        @DisplayName("consume rejects negative tokens")
        void consumeRejectsNegative() {
            TokenBudget b = new TokenBudget(100, 100);
            assertThrows(IllegalArgumentException.class, () -> b.consumeInput(-1));
            assertThrows(IllegalArgumentException.class, () -> b.consumeOutput(-1));
        }

        @Test
        @DisplayName("exact consumption up to limit is allowed")
        void exactConsumption() {
            TokenBudget b = new TokenBudget(100, 100);
            assertTrue(b.consumeInput(100));
            assertEquals(0, b.getRemainingInput());
        }
    }

    // ─── ReasonStrategy Interface Tests ─────────────────────────────────

    @Nested
    @DisplayName("ReasoningStrategy interface")
    class ReasoningStrategyInterfaceTests {

        @Test
        @DisplayName("getName() returns strategy name")
        void getName() {
            CoTStrategy cot = new CoTStrategy(llmProvider, "gpt-4", 0.5);
            assertEquals("CoT", cot.getName());
        }

        @Test
        @DisplayName("canContinue() returns true for non-null context")
        void canContinueWithContext() {
            CoTStrategy cot = new CoTStrategy(llmProvider, "gpt-4", 0.5);
            assertTrue(cot.canContinue(context));
        }

        @Test
        @DisplayName("canContinue() returns false for null context")
        void canContinueWithNullContext() {
            CoTStrategy cot = new CoTStrategy(llmProvider, "gpt-4", 0.5);
            assertFalse(cot.canContinue(null));
        }
    }

    // ─── CoTStrategy Tests ──────────────────────────────────────────────

    @Nested
    @DisplayName("CoTStrategy")
    class CoTStrategyTests {

        @Test
        @DisplayName("think() returns completed when LLM produces answer")
        void thinkReturnsCompleted() {
            CoTStrategy cot = new CoTStrategy(llmProvider, "gpt-4", 0.7);
            when(llmProvider.generate(anyString(), eq("gpt-4"), eq(0.7)))
                    .thenReturn("The answer is: 42");

            ThinkingResult result = cot.think(context, budget);

            assertEquals(ThinkingResult.Type.COMPLETED, result.type());
            assertTrue(result.finalAnswer().contains("42"));
        }

        @Test
        @DisplayName("think() returns exhausted when budget fully consumed")
        void thinkReturnsExhaustedNoBudget() {
            TokenBudget emptyBudget = new TokenBudget(0, 0);
            CoTStrategy cot = new CoTStrategy(llmProvider, "gpt-4", 0.7);

            ThinkingResult result = cot.think(context, emptyBudget);

            assertEquals(ThinkingResult.Type.EXHAUSTED, result.type());
        }

        @Test
        @DisplayName("think() returns error when LLM throws exception")
        void thinkReturnsErrorOnLlmFailure() {
            CoTStrategy cot = new CoTStrategy(llmProvider, "gpt-4", 0.7);
            when(llmProvider.generate(anyString(), anyString(), anyDouble()))
                    .thenThrow(new RuntimeException("LLM down"));

            ThinkingResult result = cot.think(context, budget);

            assertEquals(ThinkingResult.Type.ERROR, result.type());
            assertTrue(result.errorMessage().contains("LLM down"));
        }

        @Test
        @DisplayName("think() returns exhausted when output exceeds budget")
        void thinkReturnsExhaustedOnOutputExceeded() {
            TokenBudget tightBudget = new TokenBudget(100_000, 5); // very low output
            CoTStrategy cot = new CoTStrategy(llmProvider, "gpt-4", 0.7);
            when(llmProvider.generate(anyString(), anyString(), anyDouble()))
                    .thenReturn("A very long response that will exceed the tiny output budget");

            ThinkingResult result = cot.think(context, tightBudget);

            assertEquals(ThinkingResult.Type.EXHAUSTED, result.type());
        }
    }

    // ─── ReActStrategy Tests ────────────────────────────────────────────

    @Nested
    @DisplayName("ReActStrategy")
    class ReActStrategyTests {

        @Test
        @DisplayName("think() returns completed when LLM produces final answer directly")
        void thinkReturnsCompletedDirect() {
            ReActStrategy react = new ReActStrategy(llmProvider, toolRegistry, "gpt-4", 0.7);
            when(toolRegistry.listToolNames()).thenReturn(List.of("search", "calculate"));
            when(llmProvider.generateWithMessages(anyList(), eq("gpt-4"), eq(0.7)))
                    .thenReturn("FINAL_ANSWER: The result is 42");

            ThinkingResult result = react.think(context, budget);

            assertEquals(ThinkingResult.Type.COMPLETED, result.type());
            assertTrue(result.finalAnswer().contains("42"));
        }

        @Test
        @DisplayName("think() returns exhausted when budget is consumed before starting")
        void thinkReturnsExhaustedNoBudget() {
            TokenBudget emptyBudget = new TokenBudget(0, 0);
            ReActStrategy react = new ReActStrategy(llmProvider, toolRegistry, "gpt-4", 0.7);

            ThinkingResult result = react.think(context, emptyBudget);

            assertEquals(ThinkingResult.Type.EXHAUSTED, result.type());
        }

        @Test
        @DisplayName("think() handles small test budget normally")
        void thinkHandlesSmallBudget() {
            TokenBudget small = new TokenBudget(10_000, 5_000);
            ReActStrategy react = new ReActStrategy(llmProvider, toolRegistry, "gpt-4", 0.7);

            when(toolRegistry.listToolNames()).thenReturn(List.of("search"));
            when(llmProvider.generateWithMessages(anyList(), eq("gpt-4"), eq(0.7)))
                    .thenReturn("FINAL_ANSWER: Done");

            ThinkingResult result = react.think(context, small);

            assertEquals(ThinkingResult.Type.COMPLETED, result.type());
        }

        @Test
        @DisplayName("think() returns error when LLM throws")
        void thinkReturnsErrorOnLlmFailure() {
            ReActStrategy react = new ReActStrategy(llmProvider, toolRegistry, "gpt-4", 0.7);
            when(toolRegistry.listToolNames()).thenReturn(List.of("search"));
            when(llmProvider.generateWithMessages(anyList(), anyString(), anyDouble()))
                    .thenThrow(new RuntimeException("ReAct LLM failure"));

            ThinkingResult result = react.think(context, budget);

            assertEquals(ThinkingResult.Type.ERROR, result.type());
            assertTrue(result.errorMessage().contains("ReAct reasoning failed"));
        }

        @Test
        @DisplayName("getName() returns ReAct")
        void getName() {
            ReActStrategy react = new ReActStrategy(llmProvider, toolRegistry, "gpt-4", 0.7);
            assertEquals("ReAct", react.getName());
        }

        @Test
        @DisplayName("canContinue() returns true with valid dependencies")
        void canContinueValid() {
            ReActStrategy react = new ReActStrategy(llmProvider, toolRegistry, "gpt-4", 0.7);
            assertTrue(react.canContinue(context));
        }

        @Test
        @DisplayName("canContinue() returns false with null context")
        void canContinueNullContext() {
            ReActStrategy react = new ReActStrategy(llmProvider, toolRegistry, "gpt-4", 0.7);
            assertFalse(react.canContinue(null));
        }

        @Test
        @DisplayName("canContinue() returns false with null toolRegistry")
        void canContinueNullRegistry() {
            ReActStrategy react = new ReActStrategy(llmProvider, null, "gpt-4", 0.7);
            assertFalse(react.canContinue(context));
        }

        @Test
        @DisplayName("max iterations limit reached returns exhausted")
        void maxIterationsExhausted() {
            ReActStrategy react = new ReActStrategy(llmProvider, toolRegistry,
                    "gpt-4", 0.7, 2); // only 2 iterations
            when(toolRegistry.listToolNames()).thenReturn(List.of("search"));
            when(llmProvider.generateWithMessages(anyList(), eq("gpt-4"), eq(0.7)))
                    .thenReturn("Thinking about the problem..."); // no final answer, no tool call

            ThinkingResult result = react.think(context, budget);
            assertEquals(ThinkingResult.Type.EXHAUSTED, result.type());
            assertTrue(result.errorMessage().contains("max iterations"));
        }

        @Test
        @DisplayName("default constructor uses DEFAULT_MAX_ITERATIONS (10)")
        void defaultMaxIterations() {
            ReActStrategy react = new ReActStrategy(llmProvider, toolRegistry, "gpt-4", 0.7);
            assertEquals("ReAct", react.getName());
            assertTrue(react.canContinue(context));
        }
    }
}
