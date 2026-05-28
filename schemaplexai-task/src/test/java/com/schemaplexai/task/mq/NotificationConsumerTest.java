package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.dao.mapper.notification.NotificationMapper;
import com.schemaplexai.model.entity.notification.Notification;
import com.schemaplexai.quality.service.InboxDeduplicationService;
import com.schemaplexai.task.mq.dto.NotificationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class NotificationConsumerTest {

    @Mock
    private MessageFailLogService messageFailLogService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private InboxDeduplicationService dedupService;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        lenient().when(dedupService.isProcessed(any(), eq("NotificationConsumer"))).thenReturn(false);
        lenient().when(notificationMapper.insert(any(Notification.class))).thenReturn(1);
    }

    private Message createMessage(String body) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    @Test
    void onMessage_emailChannel_nacksToDlq() throws Exception {
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

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("NotificationConsumer"), anyString());
    }

    @Test
    void onMessage_failLogPersistenceFalseForUnsupportedChannel_warnsAndNacks(CapturedOutput output)
            throws Exception {
        NotificationMessage payload = new NotificationMessage();
        payload.setChannel("email");
        payload.setUserId(1L);
        payload.setTitle("title");
        payload.setContent("content");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, NotificationMessage.class)).thenReturn(payload);
        when(messageFailLogService.log(eq(message), eq("NotificationConsumer"), anyString()))
                .thenReturn(false);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("NotificationConsumer"), anyString());
        assertThat(output).contains("[NotificationConsumer] Message fail log persistence returned false");
    }

    @Test
    void onMessage_smsChannel_nacksToDlq() throws Exception {
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

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("NotificationConsumer"), anyString());
    }

    @Test
    void onMessage_inAppChannel_persistsBeforeAck() throws Exception {
        NotificationMessage payload = new NotificationMessage();
        payload.setChannel("in-app");
        payload.setTenantId("tenant-1");
        payload.setUserId(1L);
        payload.setTitle("title");
        payload.setContent("content");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, NotificationMessage.class)).thenReturn(payload);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(notificationMapper).insert(argThat(notification -> {
            assertEquals("tenant-1", notification.getTenantId());
            assertEquals(1L, notification.getUserId());
            assertEquals("title", notification.getTitle());
            assertEquals("content", notification.getContent());
            assertEquals("IN_APP", notification.getType());
            assertFalse(notification.getRead());
            return true;
        }));
        verify(channel).basicAck(1L, false);
    }

    @Test
    void onMessage_inAppPersistenceFailure_nacksToDlq() throws Exception {
        NotificationMessage payload = new NotificationMessage();
        payload.setChannel("in-app");
        payload.setTenantId("tenant-1");
        payload.setUserId(1L);
        payload.setTitle("title");
        payload.setContent("content");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, NotificationMessage.class)).thenReturn(payload);
        when(notificationMapper.insert(any(Notification.class))).thenThrow(new RuntimeException("database down"));

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("NotificationConsumer"), contains("database down"));
        verify(dedupService, never()).markProcessed(any(), eq("NotificationConsumer"));
    }

    @Test
    void onMessage_markProcessedFailure_nacksAndLogsWithoutAck() throws Exception {
        NotificationMessage payload = new NotificationMessage();
        payload.setChannel("in-app");
        payload.setTenantId("tenant-1");
        payload.setUserId(1L);
        payload.setTitle("title");
        payload.setContent("content");
        payload.setIdempotencyKey("key1");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, NotificationMessage.class)).thenReturn(payload);
        doThrow(new RuntimeException("dedup mark failed"))
                .when(dedupService).markProcessed(any(), eq("NotificationConsumer"));

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(notificationMapper).insert(any(Notification.class));
        verify(channel).basicNack(1L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(messageFailLogService).log(eq(message), eq("NotificationConsumer"), anyString());
    }

    @Test
    void onMessage_webhookChannel_nacksToDlq() throws Exception {
        NotificationMessage payload = new NotificationMessage();
        payload.setChannel("webhook");
        payload.setUserId(1L);
        payload.setTitle("title");
        payload.setContent("content");
        payload.setWebhookUrl("http://example.com/hook");
        payload.setWebhookMethod("POST");
        payload.setWebhookHeaders(Map.of("X-Auth", "token"));
        payload.setTemplateParams(Map.of("key", "value"));

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, NotificationMessage.class)).thenReturn(payload);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("NotificationConsumer"), anyString());
    }

    @Test
    void onMessage_webhookMissingUrl_nacksToDlq() throws Exception {
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
