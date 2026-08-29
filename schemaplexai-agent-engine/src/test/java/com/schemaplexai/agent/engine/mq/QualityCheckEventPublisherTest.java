package com.schemaplexai.agent.engine.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Ticket 924 / REQ-01: the post-execution quality gate trigger publishes a
 * check request on sf.exchange / sf.quality for the quality module to consume.
 */
@ExtendWith(MockitoExtension.class)
class QualityCheckEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private QualityCheckEventPublisher publisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        publisher = new QualityCheckEventPublisher(rabbitTemplate, objectMapper);
    }

    @Test
    void publishPostExecutionCheckSendsCheckRequestOnQualityRoutingKey() throws Exception {
        boolean sent = publisher.publishPostExecutionCheck(7L, 42L, "3", "Final Answer: done");

        assertTrue(sent);
        ArgumentCaptor<String> exchange = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(exchange.capture(), routingKey.capture(), body.capture());

        assertEquals(CommonConstants.EXCHANGE_SCHEMAPLEXAI, exchange.getValue());
        assertEquals(CommonConstants.RK_QUALITY, routingKey.getValue());

        JsonNode payload = objectMapper.readTree(body.getValue());
        assertEquals(QualityCheckEventPublisher.EVENT_TYPE_CHECK_REQUEST, payload.get("eventType").asText());
        assertEquals(QualityCheckEventPublisher.TRIGGER_POINT_POST_EXECUTION, payload.get("triggerPoint").asText());
        assertEquals(7L, payload.get("executionId").asLong());
        assertEquals(42L, payload.get("agentId").asLong());
        assertEquals("3", payload.get("tenantId").asText());
        assertEquals("Final Answer: done", payload.get("output").asText());
        assertNotNull(payload.get("eventId"), "idempotency key required for the consumer inbox dedup");
        assertTrue(payload.get("securityScanCompleted").asBoolean(),
                "engine guardrail pass is published as scan evidence from the COMPLETED path");
        assertTrue(payload.get("securityScanPassed").asBoolean());
        assertNotNull(payload.get("timestamp"));
    }

    @Test
    void publishPostExecutionCheckToleratesNullOutput() throws Exception {
        boolean sent = publisher.publishPostExecutionCheck(8L, 1L, "1", null);

        assertTrue(sent);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(eq(CommonConstants.EXCHANGE_SCHEMAPLEXAI),
                eq(CommonConstants.RK_QUALITY), body.capture());
        JsonNode payload = objectMapper.readTree(body.getValue());
        assertEquals(8L, payload.get("executionId").asLong());
        assertTrue(payload.get("securityScanCompleted").asBoolean());
    }

    @Test
    void mqFailureDegradesGracefullyInsteadOfThrowing() {
        doThrow(new RuntimeException("broker down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        // The completion flow must not degrade to FAILED when MQ is unavailable.
        assertDoesNotThrow(() -> publisher.publishPostExecutionCheck(9L, 1L, "1", "output"));
        assertFalse(publisher.publishPostExecutionCheck(9L, 1L, "1", "output"));
    }
}
