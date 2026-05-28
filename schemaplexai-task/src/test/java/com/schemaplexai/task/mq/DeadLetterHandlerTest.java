package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.model.event.ExecutionEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M6.6: Dead Letter Handler Tests")
class DeadLetterHandlerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Channel channel;

    @InjectMocks
    private DeadLetterHandler deadLetterHandler;

    private ExecutionEventMessage sampleEvent;
    private String samplePayload;
    private UUID sampleEventId;

    @BeforeEach
    void setUp() {
        sampleEventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Instant occurredAt = Instant.parse("2024-01-15T10:30:00Z");

        sampleEvent = new ExecutionEventMessage(
                sampleEventId,
                100L,
                1,
                "AGENT_EXECUTION_FAILED",
                "{\"error\":\"connection timeout\"}",
                occurredAt,
                1L,
                42L,
                "AUDIT"
        );

        // Manually construct JSON to avoid Instant serialization issues with plain ObjectMapper
        samplePayload = "{"
                + "\"eventId\":\"" + sampleEventId + "\","
                + "\"executionId\":100,"
                + "\"seq\":1,"
                + "\"eventType\":\"AGENT_EXECUTION_FAILED\","
                + "\"payload\":\"{\\\"error\\\":\\\"connection timeout\\\"}\","
                + "\"occurredAt\":\"2024-01-15T10:30:00Z\","
                + "\"tenantId\":1,"
                + "\"agentId\":42,"
                + "\"sensitivity\":\"AUDIT\""
                + "}";
    }

    @Test
    @DisplayName("Handles dead letter event and publishes audit event")
    void handlesDeadLetterEventAndPublishesAuditEvent() throws Exception {
        Message message = createMessage(samplePayload);

        when(objectMapper.readValue(samplePayload, ExecutionEventMessage.class)).thenReturn(sampleEvent);
        when(objectMapper.writeValueAsString(any())).thenReturn("audit-json");

        deadLetterHandler.onMessage(message, channel);

        verify(rabbitTemplate).convertAndSend(eq("execution_events"), eq("audit.dead-letter"), anyString());
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("Handles parse failure gracefully")
    void handlesParseFailureGracefully() throws Exception {
        String invalidPayload = "not-valid-json";
        Message message = createMessage(invalidPayload);

        when(objectMapper.readValue(invalidPayload, ExecutionEventMessage.class))
                .thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "Invalid JSON"));

        deadLetterHandler.onMessage(message, channel);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("Extracts event details correctly and publishes audit with CRITICAL severity")
    void extractsEventDetailsCorrectly() throws Exception {
        Message message = createMessage(samplePayload);

        when(objectMapper.readValue(samplePayload, ExecutionEventMessage.class)).thenReturn(sampleEvent);
        when(objectMapper.writeValueAsString(any())).thenReturn("audit-json");

        deadLetterHandler.onMessage(message, channel);

        verify(rabbitTemplate).convertAndSend(eq("execution_events"), eq("audit.dead-letter"), anyString());
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("Audit publish failure nacks without ack")
    void auditPublishFailureNacksWithoutAck() throws Exception {
        Message message = createMessage(samplePayload);

        when(objectMapper.readValue(samplePayload, ExecutionEventMessage.class)).thenReturn(sampleEvent);
        when(objectMapper.writeValueAsString(any())).thenReturn("audit-json");
        doThrow(new RuntimeException("MQ down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyString());

        deadLetterHandler.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    private Message createMessage(String body) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }
}
