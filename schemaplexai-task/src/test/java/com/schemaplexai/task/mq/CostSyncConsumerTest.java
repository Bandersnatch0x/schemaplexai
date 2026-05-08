package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.service.ClickHouseCostSyncService;
import com.schemaplexai.task.mq.dto.CostSyncMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CostSyncConsumerTest {

    @Mock
    private ClickHouseCostSyncService clickHouseCostSyncService;

    @Mock
    private MessageFailLogService messageFailLogService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CostSyncConsumer consumer;

    private Message createMessage(String body) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    @Test
    void onMessage_validPayload_syncsAndAcks() throws Exception {
        CostSyncMessage payload = new CostSyncMessage();
        payload.setSyncType("api");
        payload.setTenantId(1L);
        payload.setDateRange("2024-01");
        payload.setForceFullSync(true);
        payload.setIdempotencyKey("key1");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, CostSyncMessage.class)).thenReturn(payload);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(clickHouseCostSyncService).syncIncrementalData();
        verify(channel).basicAck(1L, false);
        verify(valueOperations).set(eq("sf:idempotency:cost:sync:key1"), anyString(), any(Duration.class));
    }

    @Test
    void onMessage_duplicateSync_acksAndSkips() throws Exception {
        CostSyncMessage payload = new CostSyncMessage();
        payload.setSyncType("api");
        payload.setTenantId(1L);
        payload.setIdempotencyKey("key1");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, CostSyncMessage.class)).thenReturn(payload);
        when(redisTemplate.hasKey("sf:idempotency:cost:sync:key1")).thenReturn(true);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
        verify(clickHouseCostSyncService, never()).syncIncrementalData();
    }

    @Test
    void onMessage_syncServiceThrowsException_nacksAndLogs() throws Exception {
        CostSyncMessage payload = new CostSyncMessage();
        payload.setSyncType("api");
        payload.setTenantId(1L);
        payload.setIdempotencyKey("key1");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, CostSyncMessage.class)).thenReturn(payload);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        doThrow(new RuntimeException("Sync failed")).when(clickHouseCostSyncService).syncIncrementalData();

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("CostSyncConsumer"), anyString());
    }

    @Test
    void onMessage_nullIdempotencyKey_generatesFallbackKey() throws Exception {
        CostSyncMessage payload = new CostSyncMessage();
        payload.setSyncType("api");
        payload.setTenantId(1L);
        payload.setIdempotencyKey(null);

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, CostSyncMessage.class)).thenReturn(payload);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
        verify(valueOperations).set(contains("sf:idempotency:cost:sync:api:1:"), anyString(), any(Duration.class));
    }

    @Test
    void onMessage_baseException_nacksAndLogs() throws Exception {
        CostSyncMessage payload = new CostSyncMessage();
        payload.setSyncType("api");
        payload.setTenantId(1L);
        payload.setIdempotencyKey("key1");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, CostSyncMessage.class)).thenReturn(payload);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        doThrow(new BaseException(ResultCode.INTERNAL_ERROR, "business error"))
                .when(clickHouseCostSyncService).syncIncrementalData();

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("CostSyncConsumer"), anyString());
    }
}
