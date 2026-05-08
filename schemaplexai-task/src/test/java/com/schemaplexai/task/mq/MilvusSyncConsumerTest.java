package com.schemaplexai.task.mq;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilvusSyncConsumerTest {

    @Mock
    private MessageFailLogService messageFailLogService;

    @InjectMocks
    private MilvusSyncConsumer consumer;

    private Message createMessage(String body) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    @Test
    void onMessage_validMessage_acks() throws Exception {
        Message message = createMessage("{\"collectionName\":\"test\"}");
        Channel channel = mock(Channel.class);

        consumer.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
        verify(messageFailLogService, never()).log(any(), any(), any());
    }

    @Test
    void onMessage_processingException_nacksAndLogs() throws Exception {
        Message message = createMessage("{\"collectionName\":\"test\"}");
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);

        Channel channel = mock(Channel.class);
        doThrow(new IOException("channel error")).when(channel).basicAck(1L, false);

        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("MilvusSyncConsumer"), anyString());
    }
}
