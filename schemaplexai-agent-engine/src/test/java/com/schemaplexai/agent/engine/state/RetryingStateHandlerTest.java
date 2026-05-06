package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryingStateHandlerTest {

    @Mock
    private AgentStateMachine stateMachine;

    private SfAgentExecution execution;

    @BeforeEach
    void setUp() {
        execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setAgentId(42L);
    }

    @Test
    void getStateShouldReturnRetrying() {
        RetryingStateHandler handler = new RetryingStateHandler(true, 3, 1, 30000);
        assertEquals(AgentExecutionState.RETRYING, handler.getState());
    }

    @Test
    void handleShouldTransitionToFailedWhenRetryDisabled() {
        RetryingStateHandler handler = new RetryingStateHandler(false, 3, 100, 30000);

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void handleShouldTransitionToFailedForNonRetryableError() {
        RetryingStateHandler handler = new RetryingStateHandler(true, 3, 1, 30000);
        execution.setMetadata("lastErrorCategory", ToolErrorCategory.PERMISSION_DENIED.name());

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void handleShouldTransitionToFailedWhenMaxRetriesExceeded() {
        RetryingStateHandler handler = new RetryingStateHandler(true, 2, 1, 30000);
        execution.setMetadata("lastErrorCategory", ToolErrorCategory.TIMEOUT.name());

        // First retry
        handler.handle(stateMachine, execution);
        verify(stateMachine, times(1)).transition(AgentExecutionState.TOOL_CALLING, execution);

        // Second retry
        handler.handle(stateMachine, execution);
        verify(stateMachine, times(2)).transition(AgentExecutionState.TOOL_CALLING, execution);

        // Third attempt exceeds max
        handler.handle(stateMachine, execution);
        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void handleShouldTransitionToFailedWhenCircuitBreakerOpens() {
        RetryingStateHandler handler = new RetryingStateHandler(true, 10, 1, 30000);
        execution.setMetadata("lastErrorCategory", ToolErrorCategory.TIMEOUT.name());

        // 3 consecutive failures should open circuit
        handler.handle(stateMachine, execution);
        handler.handle(stateMachine, execution);
        handler.handle(stateMachine, execution);

        // Circuit breaker opens on 3rd failure
        verify(stateMachine, times(2)).transition(AgentExecutionState.TOOL_CALLING, execution);
        verify(stateMachine, times(1)).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void handleShouldTransitionToToolCallingWithRetryContext() {
        RetryingStateHandler handler = new RetryingStateHandler(true, 3, 1, 30000);
        execution.setMetadata("lastErrorCategory", ToolErrorCategory.TIMEOUT.name());

        handler.handle(stateMachine, execution);

        assertEquals("1", execution.getMetadata("retryContext"));
        verify(stateMachine).transition(AgentExecutionState.TOOL_CALLING, execution);
    }

    @Test
    void handleShouldApplyExponentialBackoff() {
        RetryingStateHandler handler = new RetryingStateHandler(true, 5, 100, 30000);
        execution.setMetadata("lastErrorCategory", ToolErrorCategory.TIMEOUT.name());

        long start = System.currentTimeMillis();
        handler.handle(stateMachine, execution);
        long elapsed = System.currentTimeMillis() - start;

        // First retry: 100ms * 2^0 = 100ms
        assertTrue(elapsed >= 90, "Expected at least 90ms delay, got " + elapsed);
        assertTrue(elapsed < 500, "Expected less than 500ms, got " + elapsed);
    }

    @Test
    void handleShouldRespectMaxDelay() {
        RetryingStateHandler handler = new RetryingStateHandler(true, 5, 100, 50);
        execution.setMetadata("lastErrorCategory", ToolErrorCategory.TIMEOUT.name());

        long start = System.currentTimeMillis();
        handler.handle(stateMachine, execution);
        long elapsed = System.currentTimeMillis() - start;

        // Max delay is 50ms, so even first retry should be capped
        assertTrue(elapsed < 200, "Expected delay capped at ~50ms, got " + elapsed);
    }

    @Test
    void handleShouldTransitionToFailedOnInterrupt() {
        RetryingStateHandler handler = new RetryingStateHandler(true, 3, 5000, 30000) {
            @Override
            public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
                // Simulate interrupt by setting flag before calling super
                Thread.currentThread().interrupt();
                super.handle(stateMachine, execution);
            }
        };
        execution.setMetadata("lastErrorCategory", ToolErrorCategory.TIMEOUT.name());

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
        // Note: Thread.interrupted() clears the flag when reading; the assertion
        // that it was cleared is verified by the fact that the handler completed
        // without throwing and transitioned correctly
    }

    @Test
    void handleShouldAcceptNullErrorCategory() {
        RetryingStateHandler handler = new RetryingStateHandler(true, 3, 1, 30000);
        // lastErrorCategory not set -> null

        handler.handle(stateMachine, execution);

        // null category is treated as retryable (no category blocks it)
        verify(stateMachine).transition(AgentExecutionState.TOOL_CALLING, execution);
    }

    @Test
    void handleShouldAcceptBlankErrorCategory() {
        RetryingStateHandler handler = new RetryingStateHandler(true, 3, 1, 30000);
        execution.setMetadata("lastErrorCategory", "  ");

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.TOOL_CALLING, execution);
    }

    @Test
    void handleShouldAcceptInvalidErrorCategory() {
        RetryingStateHandler handler = new RetryingStateHandler(true, 3, 1, 30000);
        execution.setMetadata("lastErrorCategory", "UNKNOWN_CATEGORY");

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.TOOL_CALLING, execution);
    }

    @Test
    void clearRetryStateShouldRemoveCounters() {
        RetryingStateHandler handler = new RetryingStateHandler(true, 5, 1, 30000);
        execution.setMetadata("lastErrorCategory", ToolErrorCategory.TIMEOUT.name());

        // Circuit breaker opens after 3 consecutive failures
        handler.handle(stateMachine, execution); // failure 1 -> TOOL_CALLING
        handler.handle(stateMachine, execution); // failure 2 -> TOOL_CALLING

        verify(stateMachine, times(2)).transition(AgentExecutionState.TOOL_CALLING, execution);

        // 3rd failure opens circuit -> FAILED
        handler.handle(stateMachine, execution);
        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);

        // Clear and retry again with a new execution
        handler.clearRetryState(1L);

        SfAgentExecution exec2 = new SfAgentExecution();
        exec2.setId(2L);
        exec2.setAgentId(42L);
        exec2.setMetadata("lastErrorCategory", ToolErrorCategory.TIMEOUT.name());

        handler.handle(stateMachine, exec2);
        verify(stateMachine).transition(AgentExecutionState.TOOL_CALLING, exec2);
    }

    @Test
    void handleShouldRetryForRetryableCategories() {
        RetryingStateHandler handler = new RetryingStateHandler(true, 3, 1, 30000);

        for (ToolErrorCategory category : ToolErrorCategory.values()) {
            if (category.isRetryable()) {
                SfAgentExecution exec = new SfAgentExecution();
                exec.setId(category.ordinal() + 100L);
                exec.setAgentId(42L);
                exec.setMetadata("lastErrorCategory", category.name());

                handler.handle(stateMachine, exec);

                verify(stateMachine).transition(AgentExecutionState.TOOL_CALLING, exec);
            }
        }
    }

    @Test
    void handleShouldFailForNonRetryableCategories() {
        RetryingStateHandler handler = new RetryingStateHandler(true, 3, 1, 30000);

        for (ToolErrorCategory category : ToolErrorCategory.values()) {
            if (!category.isRetryable()) {
                SfAgentExecution exec = new SfAgentExecution();
                exec.setId(category.ordinal() + 200L);
                exec.setAgentId(42L);
                exec.setMetadata("lastErrorCategory", category.name());

                handler.handle(stateMachine, exec);

                verify(stateMachine).transition(AgentExecutionState.FAILED, exec);
            }
        }
    }
}
