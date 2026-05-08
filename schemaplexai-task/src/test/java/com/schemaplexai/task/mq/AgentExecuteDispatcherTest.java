package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.agent.engine.AgentExecutionEngine;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.mq.dto.AgentExecuteMessage;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentExecuteDispatcherTest {

    @Mock
    private AgentExecutionEngine executionEngine;

    @Mock
    private MessageFailLogService messageFailLogService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Channel channel;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AgentExecuteDispatcher dispatcher;

    private Message createMessage(String body) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    @Test
    void onMessage_validPayload_dispatchesAndAcks() throws Exception {
        AgentExecuteMessage payload = new AgentExecuteMessage();
        payload.setAgentId(1L);
        payload.setTenantId("t1");
        payload.setPrompt("hello");
        payload.setIdempotencyKey("key1");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, AgentExecuteMessage.class)).thenReturn(payload);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(100L);
        when(executionEngine.startExecution(1L, "t1", "hello")).thenReturn(execution);

        dispatcher.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
        verify(executionEngine).startExecution(1L, "t1", "hello");
        verify(valueOperations).set(eq("sf:idempotency:agent:execute:key1"), eq("1"), any(Duration.class));
    }

    @Test
    void onMessage_duplicateKey_acksAndSkips() throws Exception {
        AgentExecuteMessage payload = new AgentExecuteMessage();
        payload.setAgentId(1L);
        payload.setTenantId("t1");
        payload.setPrompt("hello");
        payload.setIdempotencyKey("key1");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, AgentExecuteMessage.class)).thenReturn(payload);
        when(redisTemplate.hasKey("sf:idempotency:agent:execute:key1")).thenReturn(true);

        dispatcher.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
        verify(executionEngine, never()).startExecution(any(), any(), any());
    }

    @Test
    void onMessage_missingAgentId_nacksAndLogs() throws Exception {
        AgentExecuteMessage payload = new AgentExecuteMessage();
        payload.setAgentId(null);
        payload.setTenantId("t1");
        payload.setPrompt("hello");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, AgentExecuteMessage.class)).thenReturn(payload);

        dispatcher.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("AgentExecuteDispatcher"), anyString());
    }

    @Test
    void onMessage_engineException_nacksAndLogs() throws Exception {
        AgentExecuteMessage payload = new AgentExecuteMessage();
        payload.setAgentId(1L);
        payload.setTenantId("t1");
        payload.setPrompt("hello");
        payload.setIdempotencyKey("key1");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, AgentExecuteMessage.class)).thenReturn(payload);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(executionEngine.startExecution(any(), any(), any()))
                .thenThrow(new RuntimeException("Engine failure"));

        dispatcher.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("AgentExecuteDispatcher"), anyString());
    }

    @Test
    void onMessage_nullIdempotencyKey_usesFallbackKey() throws Exception {
        AgentExecuteMessage payload = new AgentExecuteMessage();
        payload.setAgentId(1L);
        payload.setTenantId("t1");
        payload.setPrompt("hello");
        payload.setIdempotencyKey(null);

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, AgentExecuteMessage.class)).thenReturn(payload);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(executionEngine.startExecution(any(), any(), any())).thenReturn(new SfAgentExecution());

        dispatcher.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
        verify(valueOperations).set(eq("sf:idempotency:agent:execute:1:t1:hello"), eq("1"), any(Duration.class));
    }
}
