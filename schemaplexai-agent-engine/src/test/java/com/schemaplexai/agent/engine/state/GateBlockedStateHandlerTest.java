package com.schemaplexai.agent.engine.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mq.AgentExecutionEventPublisher;
import com.schemaplexai.common.constants.CommonConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    // --- Ticket 904 / REQ-09 regression: real publisher must not break GATE_BLOCKED ---

    @Test
    void retryableBlockWithRealPublisherShouldPublishEventAndProceedToRetrying() throws Exception {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        AgentExecutionEventPublisher realPublisher =
                new AgentExecutionEventPublisher(rabbitTemplate, new ObjectMapper());
        GateBlockedStateHandler realHandler = new GateBlockedStateHandler(realPublisher);

        execution.setMetadata("blockedReason", "rate_limit_exceeded");
        execution.setMetadata("admissionType", "RETRYABLE");

        // Before the fix this threw UnsupportedOperationException (immutable Map.of payload),
        // which the state machine converted into FAILED.
        assertDoesNotThrow(() -> realHandler.handle(stateMachine, execution));

        verify(stateMachine).transition(AgentExecutionState.RETRYING, execution);
        verify(stateMachine, never()).transition(AgentExecutionState.FAILED, execution);

        Map<String, Object> sent = captureSentMessage(rabbitTemplate);
        assertEquals("AGENT_GATE_BLOCKED", sent.get("eventType"));
        assertNotNull(sent.get("timestamp"));
        assertEquals(1L, ((Number) sent.get("executionId")).longValue());
        assertEquals(42L, ((Number) sent.get("agentId")).longValue());
        assertEquals("rate_limit_exceeded", sent.get("reason"));
        assertEquals(true, sent.get("retryable"));
        assertEquals(60, ((Number) sent.get("retryCountdown")).intValue());
    }

    @Test
    void fatalBlockWithRealPublisherShouldPublishEventAndProceedToFailed() throws Exception {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        AgentExecutionEventPublisher realPublisher =
                new AgentExecutionEventPublisher(rabbitTemplate, new ObjectMapper());
        GateBlockedStateHandler realHandler = new GateBlockedStateHandler(realPublisher);

        execution.setMetadata("blockedReason", "security_violation");
        execution.setMetadata("admissionType", "FATAL");

        // Even on the FATAL path the blocked event must actually reach MQ.
        assertDoesNotThrow(() -> realHandler.handle(stateMachine, execution));

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
        verify(stateMachine, never()).transition(AgentExecutionState.RETRYING, execution);

        Map<String, Object> sent = captureSentMessage(rabbitTemplate);
        assertEquals("AGENT_GATE_BLOCKED", sent.get("eventType"));
        assertEquals("security_violation", sent.get("reason"));
        assertEquals(false, sent.get("retryable"));
        assertNull(sent.get("retryCountdown"));
    }

    @Test
    void payloadHandedToPublisherShouldBeMutable() {
        // Simulates the publisher's enrichment step: if the handler passes an immutable
        // map (Map.of), this put throws UnsupportedOperationException and fails the test.
        doAnswer(invocation -> {
            Map<String, Object> payload = invocation.getArgument(1);
            payload.put("eventType", "AGENT_GATE_BLOCKED");
            payload.put("timestamp", System.currentTimeMillis());
            return null;
        }).when(eventPublisher).publishExecutionEvent(eq("AGENT_GATE_BLOCKED"), any());

        execution.setMetadata("blockedReason", "rate_limit_exceeded");
        execution.setMetadata("admissionType", "RETRYABLE");

        assertDoesNotThrow(() -> handler.handle(stateMachine, execution));
        verify(stateMachine).transition(AgentExecutionState.RETRYING, execution);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captureSentMessage(RabbitTemplate rabbitTemplate) throws Exception {
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(
                eq(CommonConstants.EXCHANGE_SCHEMAPLEXAI),
                eq(CommonConstants.RK_AGENT_EXEC_EVENT),
                messageCaptor.capture());
        return new ObjectMapper().readValue(messageCaptor.getValue(), Map.class);
    }
}
