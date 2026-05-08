package com.schemaplexai.agent.engine.state.middleware;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import com.schemaplexai.agent.engine.state.middleware.impl.MetricsMiddleware;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MetricsMiddlewareTest {

    @Mock
    private AgentStateMachine stateMachine;

    private final MetricsMiddleware middleware = new MetricsMiddleware();

    @Test
    void beforeShouldAlwaysReturnTrue() {
        SfAgentExecution execution = createExecution(1L);
        MiddlewareContext context = new MiddlewareContext(
                stateMachine, execution, null, AgentExecutionState.THINKING);

        assertTrue(middleware.before(context));
    }

    @Test
    void orderShouldBe200() {
        assertEquals(200, middleware.getOrder());
    }

    @Test
    void afterShouldRecordTransitionCount() {
        SfAgentExecution execution = createExecution(2L);
        MiddlewareContext context = new MiddlewareContext(
                stateMachine, execution, null, AgentExecutionState.THINKING);

        middleware.before(context);
        middleware.after(context, null);

        assertEquals(1, middleware.getTransitionCount("THINKING"));
    }

    @Test
    void afterShouldAccumulateMultipleTransitions() {
        for (int i = 0; i < 5; i++) {
            SfAgentExecution execution = createExecution((long) i);
            MiddlewareContext context = new MiddlewareContext(
                    stateMachine, execution, null, AgentExecutionState.TOOL_CALLING);

            middleware.before(context);
            middleware.after(context, null);
        }

        assertEquals(5, middleware.getTransitionCount("TOOL_CALLING"));
        assertTrue(middleware.getAverageDurationMs("TOOL_CALLING") >= 0);
    }

    @Test
    void getTransitionCountShouldReturnZeroForUnknownState() {
        assertEquals(0, middleware.getTransitionCount("UNKNOWN_STATE"));
    }

    @Test
    void getAverageDurationShouldReturnZeroForUnknownState() {
        assertEquals(0.0, middleware.getAverageDurationMs("UNKNOWN_STATE"));
    }

    private SfAgentExecution createExecution(Long id) {
        SfAgentExecution e = new SfAgentExecution();
        e.setId(id);
        e.setAgentId(1L);
        return e;
    }
}
