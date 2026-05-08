package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.task.mq.dto.NotificationMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private MessageFailLogService messageFailLogService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationConsumer consumer;

    private Message createMessage(String body) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    @Test
    void onMessage_emailChannel_acks() throws Exception {
        NotificationMessage payload = new NotificationMessage();
        payload.setChannel("email");
        payload.setUserId(1L);
        payload.setTitle("title");
        payload.setContent("content");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, NotificationMessage.class)).thenReturn(payload);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
    }

    @Test
    void onMessage_smsChannel_acks() throws Exception {
        NotificationMessage payload = new NotificationMessage();
        payload.setChannel("sms");
        payload.setUserId(1L);
        payload.setTitle("title");
        payload.setContent("content");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, NotificationMessage.class)).thenReturn(payload);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
    }

    @Test
    void onMessage_inAppChannel_acks() throws Exception {
        NotificationMessage payload = new NotificationMessage();
        payload.setChannel("in-app");
        payload.setUserId(1L);
        payload.setTitle("title");
        payload.setContent("content");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, NotificationMessage.class)).thenReturn(payload);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
    }

    @Test
    void onMessage_webhookChannel_missingUrl_nacks() throws Exception {
        NotificationMessage payload = new NotificationMessage();
        payload.setChannel("webhook");
        payload.setUserId(1L);
        payload.setTitle("title");
        payload.setContent("content");
        payload.setWebhookUrl(null);
        payload.setWebhookMethod("POST");
        payload.setWebhookHeaders(Map.of("X-Auth", "token"));
        payload.setTemplateParams(Map.of("key", "value"));

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, NotificationMessage.class)).thenReturn(payload);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
    }

    @Test
    void onMessage_webhookMissingUrl_nacks() throws Exception {
        NotificationMessage payload = new NotificationMessage();
        payload.setChannel("webhook");
        payload.setUserId(1L);
        payload.setTitle("title");
        payload.setContent("content");
        payload.setWebhookUrl(null);

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, NotificationMessage.class)).thenReturn(payload);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("NotificationConsumer"), anyString());
    }

    @Test
    void onMessage_missingChannel_nacks() throws Exception {
        NotificationMessage payload = new NotificationMessage();
        payload.setChannel(null);
        payload.setUserId(1L);

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, NotificationMessage.class)).thenReturn(payload);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("NotificationConsumer"), anyString());
    }

    @Test
    void onMessage_unsupportedChannel_nacks() throws Exception {
        NotificationMessage payload = new NotificationMessage();
        payload.setChannel("unknown");
        payload.setUserId(1L);

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, NotificationMessage.class)).thenReturn(payload);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("NotificationConsumer"), anyString());
    }

    @Test
    void onMessage_parseException_nacks() throws Exception {
        String body = "invalid-json";
        Message message = createMessage(body);

        when(objectMapper.readValue(body, NotificationMessage.class))
                .thenThrow(new RuntimeException("parse error"));

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("NotificationConsumer"), anyString());
    }
}
