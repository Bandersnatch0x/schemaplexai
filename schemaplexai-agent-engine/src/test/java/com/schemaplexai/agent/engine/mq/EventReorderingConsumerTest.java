package com.schemaplexai.agent.engine.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.job.GapRecoveryJob;
import com.schemaplexai.agent.engine.service.ExecutionEventBuffer;
import com.schemaplexai.agent.engine.service.ExecutionEventService;
import com.schemaplexai.model.event.ExecutionEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M7.7: Event Reordering Consumer Tests")
class EventReorderingConsumerTest {

    @Mock
    private ExecutionEventBuffer eventBuffer;

    @Mock
    private ExecutionEventService executionEventService;

    @Mock
    private GapRecoveryJob gapRecoveryJob;

    @Mock
    private ObjectProvider<GapRecoveryJob> gapRecoveryJobProvider;

    @Mock
    private Channel channel;

    private ObjectMapper objectMapper;
    private EventReorderingConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        consumer = new EventReorderingConsumer(
                eventBuffer, executionEventService, gapRecoveryJobProvider, objectMapper);
    }

    @Test
    @DisplayName("Applies ordered events and acks message")
    void appliesOrderedEvents() throws Exception {
        ExecutionEventMessage msg = createMessage(1L, 1, "TOOL_CALLED");
        Message amqpMsg = toAmqpMessage(msg);

        when(eventBuffer.applyInOrder(eq(1L), any())).thenReturn(List.of(convert(msg)));
        when(eventBuffer.hasGap(1L)).thenReturn(false);

        consumer.onMessage(amqpMsg, channel);

        verify(executionEventService).writeEvent(any(ExecutionEvent.class));
        verify(channel).basicAck(anyLong(), eq(false));
        verify(gapRecoveryJob, never()).recoverGapsForExecution(anyLong());
    }

    @Test
    @DisplayName("Nacks when a ready event cannot be persisted")
    void nacksWhenReadyEventPersistenceFails() throws Exception {
        ExecutionEventMessage msg = createMessage(1L, 1, "TOOL_CALLED");
        Message amqpMsg = toAmqpMessage(msg);

        when(eventBuffer.applyInOrder(eq(1L), any())).thenReturn(List.of(convert(msg)));
        doThrow(new RuntimeException("database down"))
                .when(executionEventService).writeEvent(any(ExecutionEvent.class));

        consumer.onMessage(amqpMsg, channel);

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel).basicNack(1L, false, false);
    }

    @Test
    @DisplayName("Buffers out-of-order events and triggers gap recovery")
    void buffersOutOfOrderAndTriggersGapRecovery() throws Exception {
        ExecutionEventMessage msg = createMessage(1L, 3, "TOOL_RESULT");
        Message amqpMsg = toAmqpMessage(msg);

        when(eventBuffer.applyInOrder(eq(1L), any())).thenReturn(List.of());
        when(eventBuffer.hasGap(1L)).thenReturn(true);
        when(gapRecoveryJobProvider.getIfAvailable()).thenReturn(gapRecoveryJob);

        consumer.onMessage(amqpMsg, channel);

        verify(executionEventService, never()).writeEvent(any());
        verify(gapRecoveryJob).recoverGapsForExecution(1L);
        verify(channel).basicAck(anyLong(), eq(false));
    }

    @Test
    @DisplayName("Acks gap event when recovery job is disabled")
    void acksGapEventWhenRecoveryJobDisabled() throws Exception {
        ExecutionEventMessage msg = createMessage(1L, 3, "TOOL_RESULT");
        Message amqpMsg = toAmqpMessage(msg);

        when(eventBuffer.applyInOrder(eq(1L), any())).thenReturn(List.of());
        when(eventBuffer.hasGap(1L)).thenReturn(true);
        when(gapRecoveryJobProvider.getIfAvailable()).thenReturn(null);

        consumer.onMessage(amqpMsg, channel);

        verify(executionEventService, never()).writeEvent(any());
        verify(gapRecoveryJob, never()).recoverGapsForExecution(anyLong());
        verify(channel).basicAck(anyLong(), eq(false));
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("Skips message with missing executionId")
    void skipsMissingExecutionId() throws Exception {
        ExecutionEventMessage msg = new ExecutionEventMessage(
                UUID.randomUUID(), null, 1, "TEST", "{}", Instant.now(), 1L, 1L, "AUDIT");
        Message amqpMsg = toAmqpMessage(msg);

        consumer.onMessage(amqpMsg, channel);

        verify(eventBuffer, never()).applyInOrder(any(), any());
        verify(channel).basicAck(anyLong(), eq(false));
    }

    @Test
    @DisplayName("Nacks on processing failure")
    void nacksOnFailure() throws Exception {
        Message amqpMsg = new Message("not-valid-json".getBytes(StandardCharsets.UTF_8), new MessageProperties());

        consumer.onMessage(amqpMsg, channel);

        verify(channel).basicNack(anyLong(), eq(false), eq(false));
    }

    private ExecutionEventMessage createMessage(Long executionId, int seq, String eventType) {
        return new ExecutionEventMessage(
                UUID.randomUUID(), executionId, seq, eventType, "{}", Instant.now(), 1L, 1L, "AUDIT");
    }

    private Message toAmqpMessage(ExecutionEventMessage msg) throws Exception {
        byte[] body = objectMapper.writeValueAsString(msg).getBytes(StandardCharsets.UTF_8);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(1L);
        return new Message(body, props);
    }

    private ExecutionEvent convert(ExecutionEventMessage msg) {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventId(msg.eventId());
        event.setExecutionId(msg.executionId());
        event.setSeq(msg.seq());
        event.setEventType(msg.eventType());
        event.setPayload(msg.payload());
        event.setOccurredAt(msg.occurredAt());
        event.setTenantId(msg.tenantId());
        event.setAgentId(msg.agentId());
        event.setSensitivity(msg.sensitivity());
        return event;
    }
}
