package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FailedStateHandlerTest {

    @Mock
    private AgentStateMachine stateMachine;

    private final FailedStateHandler handler = new FailedStateHandler();

    @Test
    void getStateShouldReturnFailed() {
        assertEquals(AgentExecutionState.FAILED, handler.getState());
    }

    @Test
    void handleShouldSetFailedStateAndTimestamp() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setAgentId(42L);

        handler.handle(stateMachine, execution);

        assertEquals(AgentExecutionState.FAILED.name(), execution.getState());
        assertNotNull(execution.getCompletedAt());
        verify(stateMachine).saveExecution(execution);
    }
}
