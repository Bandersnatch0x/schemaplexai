package com.schemaplexai.quality.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.model.event.ExecutionEventMessage;
import com.schemaplexai.quality.service.AuditEventService;
import com.schemaplexai.quality.service.InboxDeduplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M6.5: Audit Event Consumer Idempotency Tests")
class AuditEventConsumerIdempotencyTest {

    @Mock
    private AuditEventService auditEventService;

    @Mock
    private InboxDeduplicationService inboxDeduplicationService;

    @Mock
    private Channel channel;

    @InjectMocks
    private AuditEventConsumer auditEventConsumer;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        auditEventConsumer = new AuditEventConsumer(auditEventService, objectMapper, inboxDeduplicationService);
    }

    @Test
    @DisplayName("Skips already processed event")
    void skipsAlreadyProcessedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        ExecutionEventMessage event = new ExecutionEventMessage(
                eventId, 1L, 3, "TOOL_CALLED",
                "{\"tool\":\"git.push\"}", Instant.now(), 10L, 2L, "AUDIT");
        Message message = createMessage(event);

        when(inboxDeduplicationService.isProcessed(eventId, "AuditEventConsumer")).thenReturn(true);

        auditEventConsumer.onMessage(message, channel);

        verify(inboxDeduplicationService).isProcessed(eventId, "AuditEventConsumer");
        verifyNoInteractions(auditEventService);
        verify(channel).basicAck(anyLong(), eq(false));
        verify(inboxDeduplicationService, never()).markProcessed(any(), any());
    }

    @Test
    @DisplayName("Processes new event and marks processed")
    void processesNewEventAndMarksProcessed() throws Exception {
        UUID eventId = UUID.randomUUID();
        ExecutionEventMessage event = new ExecutionEventMessage(
                eventId, 1L, 3, "TOOL_CALLED",
                "{\"tool\":\"git.push\"}", Instant.now(), 10L, 2L, "AUDIT");
        Message message = createMessage(event);

        when(inboxDeduplicationService.isProcessed(eventId, "AuditEventConsumer")).thenReturn(false);
        doNothing().when(inboxDeduplicationService).markProcessed(eventId, "AuditEventConsumer");

        auditEventConsumer.onMessage(message, channel);

        verify(inboxDeduplicationService).isProcessed(eventId, "AuditEventConsumer");
        verify(auditEventService).save(any());
        verify(inboxDeduplicationService).markProcessed(eventId, "AuditEventConsumer");
        verify(channel).basicAck(anyLong(), eq(false));
    }

    private Message createMessage(ExecutionEventMessage event) throws Exception {
        String json = objectMapper.writeValueAsString(event);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(1L);
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }
}
