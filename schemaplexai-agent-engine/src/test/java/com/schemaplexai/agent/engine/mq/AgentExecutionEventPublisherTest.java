package com.schemaplexai.agent.engine.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Regression tests for ticket 904 / REQ-09: publishExecutionEvent must tolerate
 * immutable payloads (Map.of) because it enriches the payload via put(). Before the
 * fix, an immutable payload triggered UnsupportedOperationException which the
 * state machine converted into FAILED for every GATE_BLOCKED execution.
 */
@ExtendWith(MockitoExtension.class)
class AgentExecutionEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private AgentExecutionEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new AgentExecutionEventPublisher(rabbitTemplate, new ObjectMapper());
    }

    @Test
    void immutablePayloadShouldPublishWithoutThrowing() throws Exception {
        // Map.of is immutable — exactly what GateBlockedStateHandler used to pass
        Map<String, Object> immutablePayload = Map.of(
                "executionId", 1L,
                "agentId", 42L,
                "reason", "rate_limit_exceeded",
                "retryable", true,
                "retryCountdown", 60
        );

        assertDoesNotThrow(() ->
                publisher.publishExecutionEvent("AGENT_GATE_BLOCKED", immutablePayload));

        Map<String, Object> sent = captureSentMessage();
        assertEquals("AGENT_GATE_BLOCKED", sent.get("eventType"));
        assertNotNull(sent.get("timestamp"), "publisher must enrich the payload with a timestamp");
        assertEquals(1L, ((Number) sent.get("executionId")).longValue());
        assertEquals(42L, ((Number) sent.get("agentId")).longValue());
        assertEquals("rate_limit_exceeded", sent.get("reason"));
        assertEquals(true, sent.get("retryable"));
        assertEquals(60, ((Number) sent.get("retryCountdown")).intValue());
    }

    @Test
    void immutableEmptyPayloadShouldPublishWithEnvelopeFieldsOnly() throws Exception {
        assertDoesNotThrow(() ->
                publisher.publishExecutionEvent("AGENT_GATE_BLOCKED", Map.of()));

        Map<String, Object> sent = captureSentMessage();
        assertEquals("AGENT_GATE_BLOCKED", sent.get("eventType"));
        assertNotNull(sent.get("timestamp"));
    }

    @Test
    void nullPayloadShouldPublishWithEnvelopeFieldsOnly() throws Exception {
        assertDoesNotThrow(() ->
                publisher.publishExecutionEvent("AGENT_GATE_BLOCKED", null));

        Map<String, Object> sent = captureSentMessage();
        assertEquals("AGENT_GATE_BLOCKED", sent.get("eventType"));
        assertNotNull(sent.get("timestamp"));
    }

    @Test
    void mutablePayloadShouldPublishAndNotBeMutatedForCaller() throws Exception {
        Map<String, Object> callerPayload = new HashMap<>();
        callerPayload.put("executionId", 7L);
        callerPayload.put("reason", "token_budget_exceeded");

        assertDoesNotThrow(() ->
                publisher.publishExecutionEvent("AGENT_EXECUTION_PAUSED", callerPayload));

        Map<String, Object> sent = captureSentMessage();
        assertEquals("AGENT_EXECUTION_PAUSED", sent.get("eventType"));
        assertEquals(7L, ((Number) sent.get("executionId")).longValue());

        // The defensive copy must not leak enrichment back into the caller's map
        assertFalse(callerPayload.containsKey("eventType"));
        assertFalse(callerPayload.containsKey("timestamp"));
    }

    @Test
    void payloadShouldPreserveCallerValuesWhenKeysCollideWithEnvelope() throws Exception {
        Map<String, Object> immutablePayload = Map.of("eventType", "CALLER_VALUE");

        assertDoesNotThrow(() ->
                publisher.publishExecutionEvent("AGENT_GATE_BLOCKED", immutablePayload));

        Map<String, Object> sent = captureSentMessage();
        // Publisher envelope wins over caller-provided envelope keys
        assertEquals("AGENT_GATE_BLOCKED", sent.get("eventType"));
        assertTrue(sent.containsKey("timestamp"));
    }

    private Map<String, Object> captureSentMessage() throws Exception {
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(
                eq(CommonConstants.EXCHANGE_SCHEMAPLEXAI),
                eq(CommonConstants.RK_AGENT_EXEC_EVENT),
                messageCaptor.capture());
        return new ObjectMapper().readValue(messageCaptor.getValue(), Map.class);
    }
}
