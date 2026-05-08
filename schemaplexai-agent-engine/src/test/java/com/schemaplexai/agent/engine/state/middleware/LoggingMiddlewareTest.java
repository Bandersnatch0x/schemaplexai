package com.schemaplexai.agent.engine.state.middleware;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import com.schemaplexai.agent.engine.state.middleware.impl.LoggingMiddleware;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LoggingMiddlewareTest {

    @Mock
    private AgentStateMachine stateMachine;

    private final LoggingMiddleware middleware = new LoggingMiddleware();

    @Test
    void beforeShouldAlwaysReturnTrue() {
        SfAgentExecution execution = createExecution(1L);
        MiddlewareContext context = new MiddlewareContext(
                stateMachine, execution, null, AgentExecutionState.INITIALIZING);

        assertTrue(middleware.before(context));
    }

    @Test
    void orderShouldBeZero() {
        assertEquals(0, middleware.getOrder());
    }

    @Test
    void afterShouldNotThrowOnSuccess() {
        SfAgentExecution execution = createExecution(2L);
        MiddlewareContext context = new MiddlewareContext(
                stateMachine, execution, null, AgentExecutionState.INITIALIZING);

        assertDoesNotThrow(() -> middleware.after(context, null));
    }

    @Test
    void afterShouldNotThrowOnError() {
        SfAgentExecution execution = createExecution(3L);
        MiddlewareContext context = new MiddlewareContext(
                stateMachine, execution, null, AgentExecutionState.THINKING);

        assertDoesNotThrow(() -> middleware.after(context, new RuntimeException("test error")));
    }

    private SfAgentExecution createExecution(Long id) {
        SfAgentExecution e = new SfAgentExecution();
        e.setId(id);
        e.setAgentId(1L);
        return e;
    }
}
