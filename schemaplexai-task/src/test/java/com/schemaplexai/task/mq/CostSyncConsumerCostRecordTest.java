package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.model.event.CostRecordedEvent;
import com.schemaplexai.ops.service.CostDataSyncService;
import com.schemaplexai.ops.service.CostService;
import com.schemaplexai.quality.service.InboxDeduplicationService;
import com.schemaplexai.task.mq.dto.CostSyncMessage;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Ticket 919: CostSyncConsumer dual-mode tests — CostRecordedEvent payloads
 * published by the Agent engine on sf.exchange/sf.cost are persisted via
 * CostService, while the legacy CostSyncMessage sync trigger keeps working.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("919: CostSyncConsumer cost-recorded event path")
class CostSyncConsumerCostRecordTest {

    @Mock
    private CostDataSyncService costDataSyncService;

    @Mock
    private MessageFailLogService messageFailLogService;

    @Mock
    private InboxDeduplicationService dedupService;

    @Mock
    private CostService costService;

    @InjectMocks
    private CostSyncConsumer consumer;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private CostRecordedEvent event;

    @BeforeEach
    void setUp() {
        // The consumer built by @InjectMocks uses a real ObjectMapper for the
        // record path; wire it explicitly via reflection-free construction.
        consumer = new CostSyncConsumer(costDataSyncService, messageFailLogService, objectMapper, dedupService);
        org.springframework.test.util.ReflectionTestUtils.setField(consumer, "costService", costService);

        event = new CostRecordedEvent(
                UUID.randomUUID(), 1001L, 10L, 42L,
                "gpt-4o", "OPENAI", "chat",
                1000L, 500L, 1500L,
                null, "USD", Instant.parse("2026-08-30T10:00:00Z"));
    }

    private Message createMessage(CostRecordedEvent event, boolean withHeader) throws Exception {
        String body = objectMapper.writeValueAsString(event);
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        if (withHeader) {
            properties.setHeader("eventType", "CostRecordedEvent");
        }
        properties.setContentType("application/json");
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    @Test
    @DisplayName("Persists cost event (header dispatch) and acks")
    void costRecordedEvent_withHeader_persistsAndAcks() throws Exception {
        when(dedupService.isProcessed(any(UUID.class), eq("CostSyncConsumer"))).thenReturn(false);

        consumer.onMessage(createMessage(event, true), mockChannel());

        ArgumentCaptor<CostRecordedEvent> captor = ArgumentCaptor.forClass(CostRecordedEvent.class);
        verify(costService).processCostRecordedEvent(captor.capture());
        assertEquals(event.eventId(), captor.getValue().eventId());
        assertEquals(1001L, captor.getValue().executionId());
        assertEquals("gpt-4o", captor.getValue().modelName());
        verify(dedupService).markProcessed(event.eventId(), "CostSyncConsumer");
        verify(costDataSyncService, never()).syncIncrementalData();
    }

    @Test
    @DisplayName("Persists cost event without header via payload-shape sniffing")
    void costRecordedEvent_withoutHeader_sniffedAndPersisted() throws Exception {
        when(dedupService.isProcessed(any(UUID.class), eq("CostSyncConsumer"))).thenReturn(false);

        consumer.onMessage(createMessage(event, false), mockChannel());

        verify(costService).processCostRecordedEvent(any(CostRecordedEvent.class));
    }

    @Test
    @DisplayName("Duplicate eventId is skipped with an ack")
    void costRecordedEvent_duplicate_skipsAndAcks() throws Exception {
        when(dedupService.isProcessed(event.eventId(), "CostSyncConsumer")).thenReturn(true);
        Channel channel = mockChannel();

        consumer.onMessage(createMessage(event, true), channel);

        verify(costService, never()).processCostRecordedEvent(any());
        verify(dedupService, never()).markProcessed(any(), any());
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("Missing executionId is skipped with an ack")
    void costRecordedEvent_missingExecutionId_acksAndSkips() throws Exception {
        CostRecordedEvent incomplete = new CostRecordedEvent(
                UUID.randomUUID(), null, 10L, 42L,
                "gpt-4o", "OPENAI", "chat", 1L, 1L, 2L, null, "USD", Instant.now());
        Channel channel = mockChannel();

        consumer.onMessage(createMessage(incomplete, true), channel);

        verifyNoInteractions(costService);
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("Missing tenantId is skipped with an ack")
    void costRecordedEvent_missingTenantId_acksAndSkips() throws Exception {
        CostRecordedEvent incomplete = new CostRecordedEvent(
                UUID.randomUUID(), 1001L, null, 42L,
                "gpt-4o", "OPENAI", "chat", 1L, 1L, 2L, null, "USD", Instant.now());
        Channel channel = mockChannel();

        consumer.onMessage(createMessage(incomplete, true), channel);

        verifyNoInteractions(costService);
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("Invalid JSON with cost header nacks and records fail log")
    void costRecordedEvent_invalidJson_nacksAndFailLogs() throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(5L);
        properties.setHeader("eventType", "CostRecordedEvent");
        Message message = new Message("not-json".getBytes(StandardCharsets.UTF_8), properties);
        Channel channel = mock(Channel.class);

        consumer.onMessage(message, channel);

        verifyNoInteractions(costService);
        verify(channel).basicNack(5L, false, false);
        verify(messageFailLogService).log(eq(message), eq("CostSyncConsumer"), any());
    }

    @Test
    @DisplayName("Persistence failure nacks and records fail log")
    void costRecordedEvent_persistenceThrows_nacksAndFailLogs() throws Exception {
        when(dedupService.isProcessed(any(UUID.class), eq("CostSyncConsumer"))).thenReturn(false);
        doThrow(new RuntimeException("insert failed")).when(costService).processCostRecordedEvent(any());
        Channel channel = mockChannel();

        consumer.onMessage(createMessage(event, true), channel);

        verify(channel).basicNack(1L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(dedupService, never()).markProcessed(any(), any());
        verify(messageFailLogService).log(any(Message.class), eq("CostSyncConsumer"), any());
    }

    @Test
    @DisplayName("Cost event without a wired CostService nacks instead of dropping silently")
    void costRecordedEvent_noCostService_nacksAndFailLogs() throws Exception {
        CostSyncConsumer unwired = new CostSyncConsumer(
                costDataSyncService, messageFailLogService, objectMapper, dedupService);
        when(dedupService.isProcessed(any(UUID.class), eq("CostSyncConsumer"))).thenReturn(false);
        Channel channel = mockChannel();

        unwired.onMessage(createMessage(event, true), channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(any(Message.class), eq("CostSyncConsumer"), any());
    }

    @Test
    @DisplayName("Legacy sync trigger still works alongside the record path")
    void legacySyncTrigger_stillProcesses() throws Exception {
        CostSyncMessage payload = new CostSyncMessage();
        payload.setSyncType("incremental");
        payload.setTenantId(1L);
        payload.setIdempotencyKey("legacy-key");
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(2L);
        Message message = new Message(
                objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8), properties);

        when(dedupService.isProcessed(any(UUID.class), eq("CostSyncConsumer"))).thenReturn(false);
        Channel channel = mock(Channel.class);

        consumer.onMessage(message, channel);

        verify(costDataSyncService).syncIncrementalData();
        verifyNoInteractions(costService);
        verify(channel).basicAck(2L, false);
    }

    private Channel mockChannel() {
        return mock(Channel.class);
    }
}
