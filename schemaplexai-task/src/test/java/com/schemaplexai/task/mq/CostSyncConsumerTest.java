package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.service.CostDataSyncService;
import com.schemaplexai.quality.service.InboxDeduplicationService;
import com.schemaplexai.task.mq.dto.CostSyncMessage;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CostSyncConsumerTest {

    @Mock
    private CostDataSyncService costDataSyncService;

    @Mock
    private MessageFailLogService messageFailLogService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private InboxDeduplicationService dedupService;

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
        payload.setSyncType("incremental");
        payload.setTenantId(1L);
        payload.setIdempotencyKey("key1");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, CostSyncMessage.class)).thenReturn(payload);
        when(dedupService.isProcessed(any(), eq("CostSyncConsumer"))).thenReturn(false);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(costDataSyncService).syncIncrementalData();
        verify(channel).basicAck(1L, false);
        verify(dedupService).markProcessed(any(), eq("CostSyncConsumer"));
    }

    @Test
    void onMessage_whenFullSyncRequested_nacksWithoutIncrementalSuccess() throws Exception {
        CostSyncMessage payload = new CostSyncMessage();
        payload.setSyncType("incremental");
        payload.setTenantId(1L);
        payload.setForceFullSync(true);
        payload.setIdempotencyKey("full-key");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, CostSyncMessage.class)).thenReturn(payload);
        when(dedupService.isProcessed(any(), eq("CostSyncConsumer"))).thenReturn(false);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(costDataSyncService, never()).syncIncrementalData();
        verify(dedupService, never()).markProcessed(any(), eq("CostSyncConsumer"));
        verify(messageFailLogService).log(eq(message), eq("CostSyncConsumer"), anyString());
    }

    @Test
    void onMessage_whenDateRangeProvided_nacksWithoutIncrementalSuccess() throws Exception {
        CostSyncMessage payload = new CostSyncMessage();
        payload.setSyncType("incremental");
        payload.setTenantId(1L);
        payload.setDateRange("2024-01");
        payload.setIdempotencyKey("range-key");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, CostSyncMessage.class)).thenReturn(payload);
        when(dedupService.isProcessed(any(), eq("CostSyncConsumer"))).thenReturn(false);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(costDataSyncService, never()).syncIncrementalData();
        verify(dedupService, never()).markProcessed(any(), eq("CostSyncConsumer"));
        verify(messageFailLogService).log(eq(message), eq("CostSyncConsumer"), anyString());
    }

    @Test
    void onMessage_whenUnsupportedSyncTypeRequested_nacksWithoutIncrementalSuccess() throws Exception {
        CostSyncMessage payload = new CostSyncMessage();
        payload.setSyncType("full");
        payload.setTenantId(1L);
        payload.setIdempotencyKey("unsupported-type-key");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, CostSyncMessage.class)).thenReturn(payload);
        when(dedupService.isProcessed(any(), eq("CostSyncConsumer"))).thenReturn(false);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(costDataSyncService, never()).syncIncrementalData();
        verify(dedupService, never()).markProcessed(any(), eq("CostSyncConsumer"));
        verify(messageFailLogService).log(eq(message), eq("CostSyncConsumer"), anyString());
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
        when(dedupService.isProcessed(any(), eq("CostSyncConsumer"))).thenReturn(true);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
        verify(costDataSyncService, never()).syncIncrementalData();
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
        when(dedupService.isProcessed(any(), eq("CostSyncConsumer"))).thenReturn(false);
        doThrow(new RuntimeException("Sync failed")).when(costDataSyncService).syncIncrementalData();

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("CostSyncConsumer"), anyString());
    }

    @Test
    void onMessage_failLogPersistenceFalse_warnsAndNacks(CapturedOutput output) throws Exception {
        CostSyncMessage payload = new CostSyncMessage();
        payload.setSyncType("api");
        payload.setTenantId(1L);
        payload.setIdempotencyKey("key1");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, CostSyncMessage.class)).thenReturn(payload);
        when(dedupService.isProcessed(any(), eq("CostSyncConsumer"))).thenReturn(false);
        doThrow(new RuntimeException("Sync failed")).when(costDataSyncService).syncIncrementalData();
        when(messageFailLogService.log(eq(message), eq("CostSyncConsumer"), anyString()))
                .thenReturn(false);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(messageFailLogService).log(eq(message), eq("CostSyncConsumer"), anyString());
        assertThat(output).contains("[CostSyncConsumer] Message fail log persistence returned false");
    }

    @Test
    void onMessage_markProcessedFailure_nacksAndLogsWithoutAck() throws Exception {
        CostSyncMessage payload = new CostSyncMessage();
        payload.setSyncType("incremental");
        payload.setTenantId(1L);
        payload.setIdempotencyKey("key1");

        String body = new ObjectMapper().writeValueAsString(payload);
        Message message = createMessage(body);

        when(objectMapper.readValue(body, CostSyncMessage.class)).thenReturn(payload);
        when(dedupService.isProcessed(any(), eq("CostSyncConsumer"))).thenReturn(false);
        doThrow(new RuntimeException("dedup mark failed"))
                .when(dedupService).markProcessed(any(), eq("CostSyncConsumer"));

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(costDataSyncService).syncIncrementalData();
        verify(channel).basicNack(1L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
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
        when(dedupService.isProcessed(any(), eq("CostSyncConsumer"))).thenReturn(false);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicAck(1L, false);
        verify(dedupService).markProcessed(any(), eq("CostSyncConsumer"));
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
        when(dedupService.isProcessed(any(), eq("CostSyncConsumer"))).thenReturn(false);
        doThrow(new BaseException(ResultCode.INTERNAL_ERROR, "business error"))
                .when(costDataSyncService).syncIncrementalData();

        Channel channel = mock(Channel.class);
        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("CostSyncConsumer"), anyString());
    }
}
