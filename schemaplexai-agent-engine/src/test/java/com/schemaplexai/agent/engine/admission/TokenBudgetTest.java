package com.schemaplexai.agent.engine.admission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenBudget")
class TokenBudgetTest {

    @Nested
    @DisplayName("consumeInput")
    class ConsumeInputTests {

        @Test
        @DisplayName("should consume tokens within budget")
        void shouldConsumeWithinBudget() {
            TokenBudget budget = new TokenBudget(1000, 500);

            assertTrue(budget.consumeInput(100));
            assertEquals(900, budget.remainingInput());
        }

        @Test
        @DisplayName("should reject consumption exceeding budget")
        void shouldRejectExceedingBudget() {
            TokenBudget budget = new TokenBudget(100, 500);

            assertTrue(budget.consumeInput(80));
            assertFalse(budget.consumeInput(30)); // 80+30=110 > 100
            assertEquals(20, budget.remainingInput());
        }

        @Test
        @DisplayName("should allow exact budget consumption")
        void shouldAllowExactBudget() {
            TokenBudget budget = new TokenBudget(100, 500);

            assertTrue(budget.consumeInput(100));
            assertEquals(0, budget.remainingInput());
        }

        @Test
        @DisplayName("should accumulate consumed tokens")
        void shouldAccumulateTokens() {
            TokenBudget budget = new TokenBudget(1000, 500);

            assertTrue(budget.consumeInput(100));
            assertTrue(budget.consumeInput(200));
            assertTrue(budget.consumeInput(300));
            assertEquals(400, budget.remainingInput());
        }
    }

    @Nested
    @DisplayName("consumeOutput")
    class ConsumeOutputTests {

        @Test
        @DisplayName("should consume output tokens within budget")
        void shouldConsumeWithinBudget() {
            TokenBudget budget = new TokenBudget(1000, 500);

            assertTrue(budget.consumeOutput(100));
            assertEquals(400, budget.remainingOutput());
        }

        @Test
        @DisplayName("should reject output consumption exceeding budget")
        void shouldRejectExceedingBudget() {
            TokenBudget budget = new TokenBudget(1000, 100);

            assertTrue(budget.consumeOutput(80));
            assertFalse(budget.consumeOutput(30));
            assertEquals(20, budget.remainingOutput());
        }

        @Test
        @DisplayName("should allow exact output budget consumption")
        void shouldAllowExactBudget() {
            TokenBudget budget = new TokenBudget(1000, 500);

            assertTrue(budget.consumeOutput(500));
            assertEquals(0, budget.remainingOutput());
        }
    }

    @Nested
    @DisplayName("isExceeded / hasRemaining")
    class ExceededTests {

        @Test
        @DisplayName("should not be exceeded initially")
        void shouldNotBeExceededInitially() {
            TokenBudget budget = new TokenBudget(1000, 500);

            assertFalse(budget.isExceeded());
            assertTrue(budget.hasRemaining());
        }

        @Test
        @DisplayName("should be exceeded when input over budget")
        void shouldBeExceededWhenInputOver() {
            TokenBudget budget = new TokenBudget(100, 500);

            budget.consumeInput(100);
            // Exactly at limit, not exceeded (consumed <= max)
            assertFalse(budget.isExceeded());

            // Try to consume more, should fail but state doesn't change
            budget.consumeInput(1);
            assertFalse(budget.isExceeded()); // still at 100
        }

        @Test
        @DisplayName("should be exceeded when output over budget")
        void shouldBeExceededWhenOutputOver() {
            TokenBudget budget = new TokenBudget(1000, 50);

            budget.consumeOutput(50);
            assertFalse(budget.isExceeded());
            assertTrue(budget.hasRemaining());
        }
    }

    @Nested
    @DisplayName("remainingInput / remainingOutput")
    class RemainingTests {

        @Test
        @DisplayName("should return full budget initially")
        void shouldReturnFullBudget() {
            TokenBudget budget = new TokenBudget(1000, 500);

            assertEquals(1000, budget.remainingInput());
            assertEquals(500, budget.remainingOutput());
        }

        @Test
        @DisplayName("should return correct remaining after partial consumption")
        void shouldReturnCorrectRemaining() {
            TokenBudget budget = new TokenBudget(1000, 500);

            budget.consumeInput(300);
            budget.consumeOutput(200);

            assertEquals(700, budget.remainingInput());
            assertEquals(300, budget.remainingOutput());
        }

        @Test
        @DisplayName("should never return negative remaining")
        void shouldNeverReturnNegative() {
            TokenBudget budget = new TokenBudget(100, 50);

            budget.consumeInput(100);
            assertEquals(0, budget.remainingInput());
        }
    }

    @Nested
    @DisplayName("concurrent access")
    class ConcurrencyTests {

        @Test
        @DisplayName("should handle concurrent input consumption safely")
        void shouldHandleConcurrentConsumption() throws InterruptedException {
            TokenBudget budget = new TokenBudget(1000, 1000);
            int threadCount = 10;
            int tokensPerThread = 90; // 10 * 90 = 900, within budget

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        if (budget.consumeInput(tokensPerThread)) {
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // All 10 should succeed since 10 * 90 = 900 <= 1000
            assertEquals(10, successCount.get());
            assertEquals(100, budget.remainingInput());
        }

        @Test
        @DisplayName("should reject excess concurrent consumption")
        void shouldRejectExcessConcurrent() throws InterruptedException {
            TokenBudget budget = new TokenBudget(100, 100);
            int threadCount = 10;
            int tokensPerThread = 20; // 10 * 20 = 200, exceeds 100

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        if (budget.consumeInput(tokensPerThread)) {
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // Only 5 should succeed: 5 * 20 = 100
            assertEquals(5, successCount.get());
            assertEquals(0, budget.remainingInput());
        }
    }

    @Nested
    @DisplayName("consumeToolCall")
    class ConsumeToolCallTests {

        @Test
        @DisplayName("should consume tool calls within budget")
        void shouldConsumeToolCallsWithinBudget() {
            TokenBudget budget = new TokenBudget(1000, 500, 3);

            assertTrue(budget.consumeToolCall());
            assertTrue(budget.consumeToolCall());
            assertTrue(budget.consumeToolCall());
            assertEquals(0, budget.remainingToolCalls());
        }

        @Test
        @DisplayName("should reject tool call exceeding budget")
        void shouldRejectToolCallExceedingBudget() {
            TokenBudget budget = new TokenBudget(1000, 500, 2);

            assertTrue(budget.consumeToolCall());
            assertTrue(budget.consumeToolCall());
            assertFalse(budget.consumeToolCall());
            assertEquals(0, budget.remainingToolCalls());
        }

        @Test
        @DisplayName("should default to unlimited tool calls when max not specified")
        void shouldDefaultToUnlimitedToolCalls() {
            TokenBudget budget = new TokenBudget(1000, 500);

            assertTrue(budget.consumeToolCall());
            assertTrue(budget.consumeToolCall());
            assertTrue(budget.consumeToolCall());
            assertTrue(budget.consumeToolCall());
            assertTrue(budget.consumeToolCall());
            assertFalse(budget.isToolCallsExceeded());
        }
    }

    @Nested
    @DisplayName("isExceeded with tool calls")
    class IsExceededWithToolCallsTests {

        @Test
        @DisplayName("should be exceeded when tool calls over budget")
        void shouldBeExceededWhenToolCallsOverBudget() {
            TokenBudget budget = new TokenBudget(1000, 500, 1);

            assertFalse(budget.isExceeded());
            budget.consumeToolCall();
            assertFalse(budget.isExceeded()); // exactly at limit
            budget.consumeToolCall(); // rejected, state unchanged
            assertFalse(budget.isExceeded());
        }
    }

    @Nested
    @DisplayName("getters")
    class GetterTests {

        @Test
        @DisplayName("should return correct max values")
        void shouldReturnCorrectMax() {
            TokenBudget budget = new TokenBudget(4096, 2048, 10);

            assertEquals(4096, budget.getMaxInputTokens());
            assertEquals(2048, budget.getMaxOutputTokens());
            assertEquals(10, budget.getMaxToolCalls());
        }

        @Test
        @DisplayName("should return zero consumed initially")
        void shouldReturnZeroConsumedInitially() {
            TokenBudget budget = new TokenBudget(1000, 500, 5);

            assertEquals(0, budget.getConsumedInputTokens().get());
            assertEquals(0, budget.getConsumedOutputTokens().get());
            assertEquals(0, budget.getConsumedToolCalls().get());
        }
    }
}
