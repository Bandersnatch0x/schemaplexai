package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mq.AgentExecutionEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GateBlockedStateHandlerTest {

    @Mock
    private AgentExecutionEventPublisher eventPublisher;

    @Mock
    private AgentStateMachine stateMachine;

    @InjectMocks
    private GateBlockedStateHandler handler;

    private SfAgentExecution execution;

    @BeforeEach
    void setUp() {
        execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setAgentId(42L);
    }

    @Test
    void getStateShouldReturnGateBlocked() {
        assertEquals(AgentExecutionState.GATE_BLOCKED, handler.getState());
    }

    @Test
    void handleShouldTransitionToRetryingForRetryableBlock() {
        execution.setMetadata("blockedReason", "rate_limit_exceeded");
        execution.setMetadata("admissionType", "RETRYABLE");

        handler.handle(stateMachine, execution);

        verify(eventPublisher).publishExecutionEvent(eq("AGENT_GATE_BLOCKED"), any(Map.class));
        verify(stateMachine).saveExecution(execution);
        verify(stateMachine).transition(AgentExecutionState.RETRYING, execution);

        assertEquals("60", execution.getMetadata("retryCountdown"));
        assertEquals(AgentExecutionState.GATE_BLOCKED.name(), execution.getState());
    }

    @Test
    void handleShouldUseDefaultBlockedReasonWhenNull() {
        execution.setMetadata("admissionType", "TEMPORARY");

        handler.handle(stateMachine, execution);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(eventPublisher).publishExecutionEvent(eq("AGENT_GATE_BLOCKED"), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals("admission_denied", payload.get("reason"));
        assertEquals(true, payload.get("retryable"));
        assertEquals(60, payload.get("retryCountdown"));
    }

    @Test
    void handleShouldTransitionToFailedForFatalBlock() {
        execution.setMetadata("blockedReason", "security_violation");
        execution.setMetadata("admissionType", "FATAL");

        handler.handle(stateMachine, execution);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(eventPublisher).publishExecutionEvent(eq("AGENT_GATE_BLOCKED"), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals("security_violation", payload.get("reason"));
        assertEquals(false, payload.get("retryable"));
        assertNull(payload.get("retryCountdown"));

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
        verify(stateMachine, never()).saveExecution(any());
    }

    @Test
    void handleShouldTreatNullAdmissionTypeAsRetryable() {
        execution.setMetadata("blockedReason", "unknown_issue");
        // admissionType not set -> null

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.RETRYING, execution);
    }

    @Test
    void handleShouldTreatEmptyAdmissionTypeAsRetryable() {
        execution.setMetadata("blockedReason", "unknown_issue");
        execution.setMetadata("admissionType", "");

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.RETRYING, execution);
    }

    @Test
    void handleShouldBeCaseInsensitiveForFatal() {
        execution.setMetadata("admissionType", "fatal");

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void handleShouldBeCaseInsensitiveForFatalUpperCase() {
        execution.setMetadata("admissionType", "FATAL");

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }
}
