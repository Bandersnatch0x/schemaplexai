package com.schemaplexai.web.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.model.event.ExecutionEventMessage;
import com.schemaplexai.web.dto.SseEvent;
import com.schemaplexai.web.sse.AgentSseEmitter;
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
@DisplayName("M4.2: SSE Event Consumer Tests")
class SseEventConsumerTest {

    @Mock
    private AgentSseEmitter agentSseEmitter;

    @Mock
    private Channel channel;

    @InjectMocks
    private SseEventConsumer sseEventConsumer;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        sseEventConsumer = new SseEventConsumer(agentSseEmitter, objectMapper);
    }

    @Test
    @DisplayName("Broadcasts execution event to SSE subscribers")
    void broadcastsExecutionEvent() throws Exception {
        ExecutionEventMessage event = new ExecutionEventMessage(
                UUID.randomUUID(), 1L, 5, "STATUS_CHANGED",
                "{\"state\":\"RUNNING\"}", Instant.now(), 10L, 2L, "AUDIT");
        Message message = createMessage(event);

        sseEventConsumer.onMessage(message, channel);

        ArgumentCaptor<Long> execCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> dataCaptor = ArgumentCaptor.forClass(Object.class);
        verify(agentSseEmitter).broadcastToExecution(execCaptor.capture(), typeCaptor.capture(), dataCaptor.capture());

        assertThat(execCaptor.getValue()).isEqualTo(1L);
        assertThat(typeCaptor.getValue()).isEqualTo("STATUS_CHANGED");
        SseEvent sseEvent = (SseEvent) dataCaptor.getValue();
        assertThat(sseEvent.seq()).isEqualTo(5);
        assertThat(sseEvent.sensitivity()).isEqualTo("AUDIT");

        verify(channel).basicAck(anyLong(), eq(false));
    }

    @Test
    @DisplayName("Skips message with missing executionId")
    void skipsMissingExecutionId() throws Exception {
        String json = "{\"eventId\":\"" + UUID.randomUUID() + "\",\"seq\":1,\"eventType\":\"TEST\"}";
        Message message = new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
        message.getMessageProperties().setDeliveryTag(1L);

        sseEventConsumer.onMessage(message, channel);

        verifyNoInteractions(agentSseEmitter);
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("Nacks on parse failure")
    void nacksOnParseFailure() throws Exception {
        Message message = new Message("invalid json".getBytes(StandardCharsets.UTF_8), new MessageProperties());
        message.getMessageProperties().setDeliveryTag(2L);

        sseEventConsumer.onMessage(message, channel);

        verifyNoInteractions(agentSseEmitter);
        verify(channel).basicNack(2L, false, false);
    }

    private Message createMessage(ExecutionEventMessage event) throws Exception {
        String json = objectMapper.writeValueAsString(event);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(1L);
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }
}
