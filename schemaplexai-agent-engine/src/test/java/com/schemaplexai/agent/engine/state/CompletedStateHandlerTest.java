package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mq.QualityCheckEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompletedStateHandlerTest {

    @Mock
    private AgentStateMachine stateMachine;

    @Mock
    private QualityCheckEventPublisher qualityCheckEventPublisher;

    private CompletedStateHandler handler;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        handler = new CompletedStateHandler(qualityCheckEventPublisher);
    }

    @Test
    void getStateShouldReturnCompleted() {
        assertEquals(AgentExecutionState.COMPLETED, handler.getState());
    }

    @Test
    void handleShouldSetCompletedStateAndTimestamp() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setAgentId(42L);
        execution.setTenantId("3");

        handler.handle(stateMachine, execution);

        assertEquals(AgentExecutionState.COMPLETED.name(), execution.getState());
        assertNotNull(execution.getCompletedAt());
        verify(stateMachine).saveExecution(execution);
    }

    /**
     * Ticket 924 / REQ-01 trigger point: completing an execution publishes a
     * post-execution quality check request carrying the last output.
     */
    @Test
    void handleShouldPublishPostExecutionQualityCheck() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(2L);
        execution.setAgentId(42L);
        execution.setTenantId("3");
        execution.setMetadata("lastOutput", "Final Answer: 42");

        handler.handle(stateMachine, execution);

        verify(qualityCheckEventPublisher).publishPostExecutionCheck(
                eq(2L), eq(42L), eq("3"), eq("Final Answer: 42"));
    }

    /**
     * The quality check trigger must not break completion when the publisher
     * misbehaves (defense in depth — the publisher itself degrades gracefully).
     */
    @Test
    void handleShouldCompleteEvenIfQualityPublishThrows() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(3L);
        execution.setAgentId(42L);
        when(qualityCheckEventPublisher.publishPostExecutionCheck(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("mq down"));

        handler.handle(stateMachine, execution);

        assertEquals(AgentExecutionState.COMPLETED.name(), execution.getState());
        assertNotNull(execution.getCompletedAt());
    }
}
