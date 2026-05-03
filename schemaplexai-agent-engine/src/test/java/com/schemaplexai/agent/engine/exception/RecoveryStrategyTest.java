package com.schemaplexai.agent.engine.exception;

import com.schemaplexai.agent.engine.context.AgentContext;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Recovery Strategy Tests")
class RecoveryStrategyTest {

    private AgentContext context;

    @BeforeEach
    void setUp() {
        context = AgentContext.builder()
                .tenantId("t-001")
                .projectId("p-001")
                .conversationId("conv-123")
                .agentId(42L)
                .userId("user-1")
                .build();
    }

    // ─── RecoveryResult Tests ───────────────────────────────────────────

    @Nested
    @DisplayName("RecoveryResult")
    class RecoveryResultTests {

        @Test
        @DisplayName("factory: retry() creates RETRY with message")
        void retryFactory() {
            RecoveryResult r = RecoveryResult.retry("Attempting retry");
            assertEquals(RecoveryResult.Type.RETRY, r.type());
            assertEquals("Attempting retry", r.message());
            assertNull(r.nextState());
        }

        @Test
        @DisplayName("factory: failed() creates FAILED with message")
        void failedFactory() {
            RecoveryResult r = RecoveryResult.failed("Unrecoverable error");
            assertEquals(RecoveryResult.Type.FAILED, r.type());
            assertEquals("Unrecoverable error", r.message());
            assertNull(r.nextState());
        }

        @Test
        @DisplayName("factory: fallback() creates FALLBACK with message and state")
        void fallbackFactory() {
            RecoveryResult r = RecoveryResult.fallback("Using fallback", AgentExecutionState.REFLECTING);
            assertEquals(RecoveryResult.Type.FALLBACK, r.type());
            assertEquals("Using fallback", r.message());
            assertEquals(AgentExecutionState.REFLECTING, r.nextState());
        }
    }

    // ─── RetryRecoveryStrategy Tests ────────────────────────────────────

    @Nested
    @DisplayName("RetryRecoveryStrategy")
    class RetryRecoveryStrategyTests {

        @Test
        @DisplayName("supports() returns true for TIMEOUT")
        void supportsTimeout() {
            RetryRecoveryStrategy strategy = new RetryRecoveryStrategy();
            assertTrue(strategy.supports(ToolErrorCategory.TIMEOUT));
        }

        @Test
        @DisplayName("supports() returns true for RATE_LIMITED")
        void supportsRateLimited() {
            RetryRecoveryStrategy strategy = new RetryRecoveryStrategy();
            assertTrue(strategy.supports(ToolErrorCategory.RATE_LIMITED));
        }

        @Test
        @DisplayName("supports() returns true for INTERNAL_ERROR")
        void supportsInternalError() {
            RetryRecoveryStrategy strategy = new RetryRecoveryStrategy();
            assertTrue(strategy.supports(ToolErrorCategory.INTERNAL_ERROR));
        }

        @Test
        @DisplayName("supports() returns false for PERMISSION_DENIED")
        void doesNotSupportPermissionDenied() {
            RetryRecoveryStrategy strategy = new RetryRecoveryStrategy();
            assertFalse(strategy.supports(ToolErrorCategory.PERMISSION_DENIED));
        }

        @Test
        @DisplayName("supports() returns false for INVALID_ARGUMENT")
        void doesNotSupportInvalidArgument() {
            RetryRecoveryStrategy strategy = new RetryRecoveryStrategy();
            assertFalse(strategy.supports(ToolErrorCategory.INVALID_ARGUMENT));
        }

        @Test
        @DisplayName("supports() returns false for RESOURCE_EXHAUSTED")
        void doesNotSupportResourceExhausted() {
            RetryRecoveryStrategy strategy = new RetryRecoveryStrategy();
            assertFalse(strategy.supports(ToolErrorCategory.RESOURCE_EXHAUSTED));
        }

        @Test
        @DisplayName("recover() returns RETRY on first attempt")
        void recoverReturnsRetryFirstAttempt() {
            RetryRecoveryStrategy strategy = new RetryRecoveryStrategy();
            ToolExecutionException error = new ToolExecutionException(
                    ToolErrorCategory.TIMEOUT, "Request timed out");

            RecoveryResult result = strategy.recover(error, context);

            assertEquals(RecoveryResult.Type.RETRY, result.type());
            assertTrue(result.message().contains("Retry attempt 1/3"));
        }

        @Test
        @DisplayName("recover() tracks retry count across calls")
        void recoverTracksRetryCount() {
            RetryRecoveryStrategy strategy = new RetryRecoveryStrategy();
            ToolExecutionException error = new ToolExecutionException(
                    ToolErrorCategory.RATE_LIMITED, "Too many requests");

            // First retry
            RecoveryResult r1 = strategy.recover(error, context);
            assertEquals(RecoveryResult.Type.RETRY, r1.type());
            assertTrue(r1.message().contains("1/3"));

            // Second retry
            RecoveryResult r2 = strategy.recover(error, context);
            assertEquals(RecoveryResult.Type.RETRY, r2.type());
            assertTrue(r2.message().contains("2/3"));

            // Third retry
            RecoveryResult r3 = strategy.recover(error, context);
            assertEquals(RecoveryResult.Type.RETRY, r3.type());
            assertTrue(r3.message().contains("3/3"));

            // Fourth attempt should fail
            RecoveryResult r4 = strategy.recover(error, context);
            assertEquals(RecoveryResult.Type.FAILED, r4.type());
            assertTrue(r4.message().contains("Exceeded max retries"));
        }

        @Test
        @DisplayName("recover() returns FAILED immediately when maxRetries=0")
        void recoverReturnsFailedWhenMaxRetriesZero() {
            RetryRecoveryStrategy strategy = new RetryRecoveryStrategy(0, 1000L);
            ToolExecutionException error = new ToolExecutionException(
                    ToolErrorCategory.TIMEOUT, "Timeout");

            RecoveryResult result = strategy.recover(error, context);

            assertEquals(RecoveryResult.Type.FAILED, result.type());
        }

        @Test
        @DisplayName("getMaxRetries() returns configured value")
        void getMaxRetriesReturnsConfig() {
            RetryRecoveryStrategy strategy = new RetryRecoveryStrategy(5, 500L);
            assertEquals(5, strategy.getMaxRetries());
        }

        @Test
        @DisplayName("default constructor uses 3 retries")
        void defaultMaxRetries() {
            RetryRecoveryStrategy strategy = new RetryRecoveryStrategy();
            assertEquals(3, strategy.getMaxRetries());
        }

        @Test
        @DisplayName("resetRetries() clears retry count")
        void resetRetriesClearsCount() {
            RetryRecoveryStrategy strategy = new RetryRecoveryStrategy();
            ToolExecutionException error = new ToolExecutionException(
                    ToolErrorCategory.INTERNAL_ERROR, "Internal error");

            strategy.recover(error, context);    // 1st retry
            strategy.recover(error, context);    // 2nd retry

            strategy.resetRetries(context, ToolErrorCategory.INTERNAL_ERROR);

            RecoveryResult result = strategy.recover(error, context);
            assertEquals(RecoveryResult.Type.RETRY, result.type());
            assertTrue(result.message().contains("1/3"), "Should start over at 1/3 after reset");
        }

        @Test
        @DisplayName("recover() returns FAILED after reset and exhaustion")
        void recoverReturnsFailedAfterResetAndExhaustion() {
            RetryRecoveryStrategy strategy = new RetryRecoveryStrategy(3, 0L); // zero delay for speed
            ToolExecutionException error = new ToolExecutionException(
                    ToolErrorCategory.INTERNAL_ERROR, "Error");

            strategy.recover(error, context);
            strategy.recover(error, context);
            strategy.recover(error, context);

            RecoveryResult result = strategy.recover(error, context);
            assertEquals(RecoveryResult.Type.FAILED, result.type());
        }
    }

    // ─── FallbackRecoveryStrategy Tests ─────────────────────────────────

    @Nested
    @DisplayName("FallbackRecoveryStrategy")
    class FallbackRecoveryStrategyTests {

        @Test
        @DisplayName("supports() returns true for PERMISSION_DENIED")
        void supportsPermissionDenied() {
            FallbackRecoveryStrategy strategy = new FallbackRecoveryStrategy();
            assertTrue(strategy.supports(ToolErrorCategory.PERMISSION_DENIED));
        }

        @Test
        @DisplayName("supports() returns true for INVALID_ARGUMENT")
        void supportsInvalidArgument() {
            FallbackRecoveryStrategy strategy = new FallbackRecoveryStrategy();
            assertTrue(strategy.supports(ToolErrorCategory.INVALID_ARGUMENT));
        }

        @Test
        @DisplayName("supports() returns true for RESOURCE_EXHAUSTED")
        void supportsResourceExhausted() {
            FallbackRecoveryStrategy strategy = new FallbackRecoveryStrategy();
            assertTrue(strategy.supports(ToolErrorCategory.RESOURCE_EXHAUSTED));
        }

        @Test
        @DisplayName("supports() returns false for TIMEOUT")
        void doesNotSupportTimeout() {
            FallbackRecoveryStrategy strategy = new FallbackRecoveryStrategy();
            assertFalse(strategy.supports(ToolErrorCategory.TIMEOUT));
        }

        @Test
        @DisplayName("supports() returns false for RATE_LIMITED")
        void doesNotSupportRateLimited() {
            FallbackRecoveryStrategy strategy = new FallbackRecoveryStrategy();
            assertFalse(strategy.supports(ToolErrorCategory.RATE_LIMITED));
        }

        @Test
        @DisplayName("recover() returns FALLBACK for PERMISSION_DENIED")
        void recoverPermissionDenied() {
            FallbackRecoveryStrategy strategy = new FallbackRecoveryStrategy();
            ToolExecutionException error = new ToolExecutionException(
                    ToolErrorCategory.PERMISSION_DENIED, "Access denied");

            RecoveryResult result = strategy.recover(error, context);

            assertEquals(RecoveryResult.Type.FALLBACK, result.type());
            assertEquals(AgentExecutionState.REFLECTING, result.nextState());
            assertTrue(result.message().contains("[FALLBACK]"));
        }

        @Test
        @DisplayName("recover() returns FALLBACK for INVALID_ARGUMENT")
        void recoverInvalidArgument() {
            FallbackRecoveryStrategy strategy = new FallbackRecoveryStrategy();
            ToolExecutionException error = new ToolExecutionException(
                    ToolErrorCategory.INVALID_ARGUMENT, "Missing required parameter");

            RecoveryResult result = strategy.recover(error, context);

            assertEquals(RecoveryResult.Type.FALLBACK, result.type());
            assertTrue(result.message().contains("[FALLBACK]"));
        }

        @Test
        @DisplayName("recover() returns FALLBACK for RESOURCE_EXHAUSTED")
        void recoverResourceExhausted() {
            FallbackRecoveryStrategy strategy = new FallbackRecoveryStrategy();
            ToolExecutionException error = new ToolExecutionException(
                    ToolErrorCategory.RESOURCE_EXHAUSTED, "Out of memory");

            RecoveryResult result = strategy.recover(error, context);

            assertEquals(RecoveryResult.Type.FALLBACK, result.type());
            assertTrue(result.message().contains("[FALLBACK]"));
        }

        @Test
        @DisplayName("custom fallback state is used in result")
        void customFallbackState() {
            FallbackRecoveryStrategy strategy = new FallbackRecoveryStrategy(AgentExecutionState.COMPLETED);
            ToolExecutionException error = new ToolExecutionException(
                    ToolErrorCategory.PERMISSION_DENIED, "Access denied");

            RecoveryResult result = strategy.recover(error, context);

            assertEquals(AgentExecutionState.COMPLETED, result.nextState());
        }

        @Test
        @DisplayName("registerMessage() overrides default fallback message")
        void registerCustomMessage() {
            FallbackRecoveryStrategy strategy = new FallbackRecoveryStrategy();
            strategy.registerMessage(ToolErrorCategory.PERMISSION_DENIED, "Custom denial message");

            ToolExecutionException error = new ToolExecutionException(
                    ToolErrorCategory.PERMISSION_DENIED, "Access denied");

            RecoveryResult result = strategy.recover(error, context);

            assertTrue(result.message().contains("Custom denial message"));
        }

        @Test
        @DisplayName("getMaxRetries() always returns 0")
        void getMaxRetriesZero() {
            FallbackRecoveryStrategy strategy = new FallbackRecoveryStrategy();
            assertEquals(0, strategy.getMaxRetries());
        }
    }
}
