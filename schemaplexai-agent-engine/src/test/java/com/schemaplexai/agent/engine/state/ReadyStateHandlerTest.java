package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReadyStateHandlerTest {

    @Mock
    private AgentStateMachine stateMachine;

    private final ReadyStateHandler handler = new ReadyStateHandler();

    @Test
    void getStateShouldReturnReady() {
        assertEquals(AgentExecutionState.READY, handler.getState());
    }

    @Test
    void handleShouldTransitionToThinking() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setAgentId(42L);

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);
    }
}
