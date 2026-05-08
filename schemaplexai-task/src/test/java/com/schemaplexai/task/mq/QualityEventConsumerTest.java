package com.schemaplexai.task.mq;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QualityEventConsumerTest {

    @Mock
    private MessageFailLogService messageFailLogService;

    @InjectMocks
    private QualityEventConsumer consumer;

    private Message createMessage(String body) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    @Test
    void onMessage_validMessage_acks() throws Exception {
        Message message = createMessage("{\"eventType\":\"DRIFT_DETECTED\"}");
        Channel channel = mock(Channel.class);

        consumer.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
        verify(messageFailLogService, never()).log(any(), any(), any());
    }

    @Test
    void onMessage_processingException_nacksAndLogs() throws Exception {
        Message message = createMessage("{\"eventType\":\"DRIFT_DETECTED\"}");
        Channel channel = mock(Channel.class);
        doThrow(new RuntimeException("processing error")).when(channel).basicAck(1L, false);

        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("QualityEventConsumer"), anyString());
    }
}
