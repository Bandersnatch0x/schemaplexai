package com.schemaplexai.task.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.model.event.ExecutionEventMessage;
import com.schemaplexai.ops.service.CostService;
import com.schemaplexai.task.mq.MessageFailLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ExecutionEventConsumerTest {

    @Mock
    private CostService costService;

    @Mock
    private MessageFailLogService messageFailLogService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ExecutionEventConsumer consumer;

    private Message createMessage(String body) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    private String toJson(ExecutionEventMessage event) {
        return "{\"eventId\":\"" + event.eventId() + "\"," +
                "\"executionId\":" + event.executionId() + "," +
                "\"seq\":" + event.seq() + "," +
                "\"eventType\":\"" + event.eventType() + "\"," +
                "\"payload\":" + (event.payload() == null ? "null" : "\"" + event.payload().replace("\\", "\\\\").replace("\"", "\\\"") + "\"") + "," +
                "\"occurredAt\":\"" + event.occurredAt().toString() + "\"," +
                "\"tenantId\":" + event.tenantId() + "," +
                "\"agentId\":" + event.agentId() + "," +
                "\"sensitivity\":\"" + event.sensitivity() + "\"}";
    }

    @Test
    void onMessage_validTokenUsedEvent_acksAndProcesses() throws Exception {
        ExecutionEventMessage event = new ExecutionEventMessage(
                UUID.randomUUID(), 1L, 1, "TOKEN_USED",
                "{\"modelName\":\"gpt-4\",\"inputTokens\":1000,\"outputTokens\":500}",
                Instant.now(), 1L, 1L, "NORMAL"
        );
        String body = toJson(event);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, ExecutionEventMessage.class)).thenReturn(event);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
        verify(costService).processExecutionEvent(event);
        verify(messageFailLogService, never()).log(any(), any(), any());
    }

    @Test
    void onMessage_validToolCallEvent_acksAndProcesses() throws Exception {
        ExecutionEventMessage event = new ExecutionEventMessage(
                UUID.randomUUID(), 1L, 1, "TOOL_CALL",
                "{\"toolName\":\"search\"}",
                Instant.now(), 1L, 1L, "NORMAL"
        );
        String body = toJson(event);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, ExecutionEventMessage.class)).thenReturn(event);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
        verify(costService).processExecutionEvent(event);
    }

    @Test
    void onMessage_processingException_nacksAndLogs() throws Exception {
        ExecutionEventMessage event = new ExecutionEventMessage(
                UUID.randomUUID(), 1L, 1, "TOKEN_USED",
                "{\"modelName\":\"gpt-4\"}",
                Instant.now(), 1L, 1L, "NORMAL"
        );
        String body = toJson(event);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, ExecutionEventMessage.class)).thenReturn(event);
        doThrow(new RuntimeException("cost processing failed")).when(costService).processExecutionEvent(event);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("ExecutionEventConsumer"), anyString());
    }

    @Test
    void onMessage_failLogPersistenceFalse_warnsAndNacks(CapturedOutput output) throws Exception {
        ExecutionEventMessage event = new ExecutionEventMessage(
                UUID.randomUUID(), 1L, 1, "TOKEN_USED",
                "{\"modelName\":\"gpt-4\"}",
                Instant.now(), 1L, 1L, "NORMAL"
        );
        String body = toJson(event);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, ExecutionEventMessage.class)).thenReturn(event);
        doThrow(new RuntimeException("cost processing failed")).when(costService).processExecutionEvent(event);
        when(messageFailLogService.log(eq(message), eq("ExecutionEventConsumer"), anyString()))
                .thenReturn(false);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(messageFailLogService).log(eq(message), eq("ExecutionEventConsumer"), anyString());
        assertThat(output).contains("[ExecutionEventConsumer] Message fail log persistence returned false");
    }

    @Test
    void onMessage_invalidJson_nacksAndLogs() throws Exception {
        String body = "invalid json";
        Message message = createMessage(body);

        when(objectMapper.readValue(body, ExecutionEventMessage.class))
                .thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "bad json"));

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("ExecutionEventConsumer"), anyString());
        verify(costService, never()).processExecutionEvent(any());
    }
}
