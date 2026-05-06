package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObservationStateHandlerTest {

    @Mock
    private AgentStateMachine stateMachine;

    private final ObservationStateHandler handler = new ObservationStateHandler();

    private SfAgentExecution execution;

    @BeforeEach
    void setUp() {
        execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setAgentId(42L);
    }

    @Test
    void getStateShouldReturnObservation() {
        assertEquals(AgentExecutionState.OBSERVATION, handler.getState());
    }

    @Test
    void handleShouldTransitionToCompletedWhenMaxIterationsReached() {
        execution.setTokenBudgetJson("iterations=10");
        execution.setMetadata("lastOutput", "some output");

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
    }

    @Test
    void handleShouldTransitionToCompletedWhenFinalAnswerDetected() {
        execution.setTokenBudgetJson("iterations=3");
        execution.setMetadata("lastOutput", "Final Answer: The answer is 42");

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
    }

    @Test
    void handleShouldTransitionToReflectingAndIncrementIteration() {
        execution.setTokenBudgetJson("iterations=2");
        execution.setMetadata("lastOutput", "Some intermediate output without final answer");

        handler.handle(stateMachine, execution);

        verify(stateMachine).saveExecution(execution);
        verify(stateMachine).transition(AgentExecutionState.REFLECTING, execution);
        assertEquals("iterations=3", execution.getTokenBudgetJson());
    }

    @Test
    void handleShouldTransitionToReflectingWithZeroIterationsWhenNoBudgetSet() {
        execution.setTokenBudgetJson(null);
        execution.setMetadata("lastOutput", "Some output");

        handler.handle(stateMachine, execution);

        verify(stateMachine).saveExecution(execution);
        verify(stateMachine).transition(AgentExecutionState.REFLECTING, execution);
        assertEquals("iterations=1", execution.getTokenBudgetJson());
    }

    @Test
    void handleShouldTransitionToReflectingWithEmptyBudget() {
        execution.setTokenBudgetJson("");
        execution.setMetadata("lastOutput", "Some output");

        handler.handle(stateMachine, execution);

        verify(stateMachine).saveExecution(execution);
        verify(stateMachine).transition(AgentExecutionState.REFLECTING, execution);
        assertEquals("iterations=1", execution.getTokenBudgetJson());
    }

    @Test
    void handleShouldTransitionToReflectingWhenLastOutputIsNull() {
        execution.setTokenBudgetJson("iterations=1");
        // lastOutput not set -> null

        handler.handle(stateMachine, execution);

        verify(stateMachine).saveExecution(execution);
        verify(stateMachine).transition(AgentExecutionState.REFLECTING, execution);
    }

    @Test
    void handleShouldAppendIterationToExistingBudgetWithoutIterations() {
        execution.setTokenBudgetJson("maxInput=1000,maxOutput=500");
        execution.setMetadata("lastOutput", "Some output");

        handler.handle(stateMachine, execution);

        verify(stateMachine).saveExecution(execution);
        assertEquals("maxInput=1000,maxOutput=500,iterations=1", execution.getTokenBudgetJson());
    }

    @Test
    void handleShouldIncrementIterationWithCommaSeparatedFormat() {
        execution.setTokenBudgetJson("iterations=5,other=data");
        execution.setMetadata("lastOutput", "Some output");

        handler.handle(stateMachine, execution);

        assertEquals("iterations=6,other=data", execution.getTokenBudgetJson());
    }

    @Test
    void handleShouldHandleIterationCountAtBoundary() {
        execution.setTokenBudgetJson("iterations=9");
        execution.setMetadata("lastOutput", "Some output");

        handler.handle(stateMachine, execution);

        // iterationCount is resolved BEFORE incrementing; 9 >= 10 is false,
        // so it transitions to REFLECTING and increments to 10
        verify(stateMachine).saveExecution(execution);
        verify(stateMachine).transition(AgentExecutionState.REFLECTING, execution);
        assertEquals("iterations=10", execution.getTokenBudgetJson());
    }
}
