package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.agent.engine.AgentExecutionEngine;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.service.InboxDeduplicationService;
import com.schemaplexai.task.mq.dto.AgentExecuteMessage;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AgentExecuteDispatcherTest {

    @Mock
    private AgentExecutionEngine executionEngine;

    @Mock
    private MessageFailLogService messageFailLogService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Channel channel;

    @Mock
    private InboxDeduplicationService dedupService;

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
        when(dedupService.isProcessed(any(), eq("AgentExecuteDispatcher"))).thenReturn(false);

        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(100L);
        when(executionEngine.startExecution(1L, "t1", "hello")).thenReturn(execution);

        dispatcher.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
        verify(executionEngine).startExecution(1L, "t1", "hello");
        verify(dedupService).markProcessed(any(), eq("AgentExecuteDispatcher"));
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
        when(dedupService.isProcessed(any(), eq("AgentExecuteDispatcher"))).thenReturn(true);

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
    void onMessage_failLogPersistenceFalse_warnsAndNacks(CapturedOutput output) throws Exception {
        AgentExecuteMessage payload = new AgentExecuteMessage();
        payload.setAgentId(null);
        payload.setTenantId("t1");
        payload.setPrompt("hello");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, AgentExecuteMessage.class)).thenReturn(payload);
        when(messageFailLogService.log(eq(message), eq("AgentExecuteDispatcher"), anyString()))
                .thenReturn(false);

        dispatcher.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("AgentExecuteDispatcher"), anyString());
        assertThat(output).contains("[AgentExecuteDispatcher] Message fail log persistence returned false");
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
        when(dedupService.isProcessed(any(), eq("AgentExecuteDispatcher"))).thenReturn(false);
        when(executionEngine.startExecution(any(), any(), any()))
                .thenThrow(new RuntimeException("Engine failure"));

        dispatcher.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("AgentExecuteDispatcher"), anyString());
    }

    @Test
    void onMessage_markProcessedFailure_nacksAndLogsWithoutAck() throws Exception {
        AgentExecuteMessage payload = new AgentExecuteMessage();
        payload.setAgentId(1L);
        payload.setTenantId("t1");
        payload.setPrompt("hello");
        payload.setIdempotencyKey("key1");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, AgentExecuteMessage.class)).thenReturn(payload);
        when(dedupService.isProcessed(any(), eq("AgentExecuteDispatcher"))).thenReturn(false);

        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(100L);
        when(executionEngine.startExecution(1L, "t1", "hello")).thenReturn(execution);
        doThrow(new RuntimeException("dedup mark failed"))
                .when(dedupService).markProcessed(any(), eq("AgentExecuteDispatcher"));

        dispatcher.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
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
        when(dedupService.isProcessed(any(), eq("AgentExecuteDispatcher"))).thenReturn(false);
        when(executionEngine.startExecution(any(), any(), any())).thenReturn(new SfAgentExecution());

        dispatcher.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
        verify(dedupService).markProcessed(any(), eq("AgentExecuteDispatcher"));
    }
}
