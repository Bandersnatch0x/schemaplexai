package com.schemaplexai.quality.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.model.event.ExecutionEventMessage;
import com.schemaplexai.quality.entity.SfAuditEvent;
import com.schemaplexai.quality.service.AuditEventService;
import com.schemaplexai.quality.service.InboxDeduplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M4.4: Audit Event Consumer Tests")
class AuditEventConsumerTest {

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
    @DisplayName("Projects AUDIT event to sf_audit_event with content hash")
    void projectsAuditEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        ExecutionEventMessage event = new ExecutionEventMessage(
                eventId, 1L, 3, "TOOL_CALLED",
                "{\"tool\":\"git.push\"}", Instant.now(), 10L, 2L, "AUDIT");
        Message message = createMessage(event);

        when(inboxDeduplicationService.isProcessed(eventId, "AuditEventConsumer")).thenReturn(false);
        doNothing().when(inboxDeduplicationService).markProcessed(eventId, "AuditEventConsumer");

        auditEventConsumer.onMessage(message, channel);

        ArgumentCaptor<SfAuditEvent> captor = ArgumentCaptor.forClass(SfAuditEvent.class);
        verify(auditEventService).save(captor.capture());

        SfAuditEvent saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("TOOL_CALLED");
        assertThat(saved.getExecutionId()).isEqualTo(1L);
        assertThat(saved.getTenantId()).isEqualTo("10");
        assertThat(saved.getEventId()).isEqualTo(event.eventId());
        assertThat(saved.getContentHash()).isNotBlank();
        assertThat(saved.getCorrupted()).isNull();

        verify(channel).basicAck(anyLong(), eq(false));
    }

    @Test
    @DisplayName("Skips DEBUG and EPHEMERAL events")
    void skipsNonAuditEvents() throws Exception {
        UUID eventId = UUID.randomUUID();
        ExecutionEventMessage debugEvent = new ExecutionEventMessage(
                eventId, 1L, 1, "THOUGHT",
                "{}", Instant.now(), 10L, 2L, "DEBUG");
        Message message = createMessage(debugEvent);

        when(inboxDeduplicationService.isProcessed(eventId, "AuditEventConsumer")).thenReturn(false);

        auditEventConsumer.onMessage(message, channel);

        verifyNoInteractions(auditEventService);
        verify(channel).basicAck(anyLong(), eq(false));
    }

    @Test
    @DisplayName("Nacks on processing failure")
    void nacksOnFailure() throws Exception {
        Message message = new Message("invalid".getBytes(StandardCharsets.UTF_8), new MessageProperties());
        message.getMessageProperties().setDeliveryTag(5L);

        auditEventConsumer.onMessage(message, channel);

        verifyNoInteractions(auditEventService);
        verify(channel).basicNack(5L, false, false);
    }

    @Test
    @DisplayName("Skips already processed event via deduplication")
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
    @DisplayName("Computes deterministic content hash")
    void computesDeterministicHash() {
        ExecutionEventMessage event = new ExecutionEventMessage(
                UUID.randomUUID(), 1L, 1, "TEST", "{}",
                Instant.parse("2026-01-01T00:00:00Z"), 10L, 2L, "AUDIT");

        String hash1 = auditEventConsumer.computeContentHash(event);
        String hash2 = auditEventConsumer.computeContentHash(event);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSizeGreaterThan(40);
    }

    private Message createMessage(ExecutionEventMessage event) throws Exception {
        String json = objectMapper.writeValueAsString(event);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(1L);
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }
}
