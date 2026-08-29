package com.schemaplexai.workflow.service;

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
 * Ticket 924 trigger point 3: workflow node quality checks share the engine's
 * wire contract (sf.exchange / sf.quality) with a WORKFLOW_NODE trigger point.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowQualityCheckPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private WorkflowQualityCheckPublisher publisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        publisher = new WorkflowQualityCheckPublisher(rabbitTemplate, objectMapper);
    }

    @Test
    void publishPostNodeCheckSendsWorkflowNodeRequestOnQualityKey() throws Exception {
        boolean sent = publisher.publishPostNodeCheck(55L, 7L, "summarize", "3", "node output");

        assertTrue(sent);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(eq(CommonConstants.EXCHANGE_SCHEMAPLEXAI),
                eq(CommonConstants.RK_QUALITY), body.capture());

        JsonNode payload = objectMapper.readTree(body.getValue());
        assertEquals(WorkflowQualityCheckPublisher.EVENT_TYPE_CHECK_REQUEST, payload.get("eventType").asText());
        assertEquals(WorkflowQualityCheckPublisher.TRIGGER_POINT_WORKFLOW_NODE, payload.get("triggerPoint").asText());
        assertEquals(55L, payload.get("executionId").asLong(), "node execution id rides the required field");
        assertEquals(7L, payload.get("workflowInstanceId").asLong());
        assertEquals("3", payload.get("tenantId").asText());
        assertEquals("[summarize] node output", payload.get("output").asText());
        assertNotNull(payload.get("eventId"), "idempotency key required for consumer inbox dedup");
        assertFalse(payload.get("securityScanCompleted").asBoolean(),
                "node output is not guardrail-cleared — quality rules inspect it");
        assertFalse(payload.get("securityScanPassed").asBoolean());
    }

    @Test
    void mqFailureDegradesGracefullyInsteadOfFailingTheFlow() {
        doThrow(new RuntimeException("broker down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> publisher.publishPostNodeCheck(1L, 1L, "n", "1", "out"));
        assertFalse(publisher.publishPostNodeCheck(1L, 1L, "n", "1", "out"));
    }
}
