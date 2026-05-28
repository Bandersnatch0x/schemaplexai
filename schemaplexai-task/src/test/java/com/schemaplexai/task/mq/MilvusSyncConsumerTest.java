package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.mq.dto.MilvusSyncMessage;
import com.schemaplexai.task.service.MilvusSyncRequestHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class MilvusSyncConsumerTest {

    @Mock
    private MessageFailLogService messageFailLogService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MilvusSyncRequestHandler syncRequestHandler;

    @InjectMocks
    private MilvusSyncConsumer consumer;

    private Message createMessage(String body) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    @Test
    void onMessage_validMessage_delegatesAndAcks() throws Exception {
        Message message = createMessage("{\"collectionName\":\"test\",\"operation\":\"SYNC_DOC\",\"docId\":11}");
        Channel channel = mock(Channel.class);

        consumer.onMessage(message, channel);

        verify(syncRequestHandler).handle(argThat(payload ->
                "test".equals(payload.getCollectionName())
                        && "SYNC_DOC".equals(payload.getOperation())
                        && Long.valueOf(11L).equals(payload.getDocId())));
        verify(channel).basicAck(1L, false);
        verify(messageFailLogService, never()).log(any(), any(), any());
    }

    @Test
    void onMessage_processingException_nacksAndLogs() throws Exception {
        Message message = createMessage("{\"collectionName\":\"test\",\"operation\":\"SYNC_DOC\",\"docId\":11}");
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);

        Channel channel = mock(Channel.class);
        doThrow(new IOException("channel error")).when(channel).basicAck(1L, false);

        consumer.onMessage(message, channel);

        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("MilvusSyncConsumer"), anyString());
    }

    @Test
    void onMessage_handlerNotImplemented_nacksAndLogs() throws Exception {
        Message message = createMessage("{\"collectionName\":\"test\",\"operation\":\"SYNC_DOC\",\"docId\":11}");
        Channel channel = mock(Channel.class);
        doThrow(new BaseException(ResultCode.INTERNAL_ERROR, "milvus sync handler is not implemented"))
                .when(syncRequestHandler).handle(any(MilvusSyncMessage.class));

        consumer.onMessage(message, channel);

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("MilvusSyncConsumer"), contains("not implemented"));
    }

    @Test
    void onMessage_failLogPersistenceFalse_warnsAndNacks(CapturedOutput output) throws Exception {
        Message message = createMessage("{\"collectionName\":\"test\",\"operation\":\"SYNC_DOC\",\"docId\":11}");
        Channel channel = mock(Channel.class);
        doThrow(new BaseException(ResultCode.INTERNAL_ERROR, "milvus sync handler is not implemented"))
                .when(syncRequestHandler).handle(any(MilvusSyncMessage.class));
        when(messageFailLogService.log(eq(message), eq("MilvusSyncConsumer"), anyString()))
                .thenReturn(false);

        consumer.onMessage(message, channel);

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("MilvusSyncConsumer"), anyString());
        assertThat(output).contains("[MilvusSyncConsumer] Message fail log persistence returned false");
    }

    @Test
    void onMessage_invalidJson_nacksAndLogs() throws Exception {
        Message message = createMessage("invalid-json");
        Channel channel = mock(Channel.class);

        consumer.onMessage(message, channel);

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("MilvusSyncConsumer"), anyString());
    }

    @Test
    void onMessage_missingCollectionName_nacksAndLogs() throws Exception {
        Message message = createMessage("{\"operation\":\"UPSERT\"}");
        Channel channel = mock(Channel.class);

        consumer.onMessage(message, channel);

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("MilvusSyncConsumer"), contains("collectionName"));
    }

    @Test
    void onMessage_syncDocMissingDocId_nacksAndLogs() throws Exception {
        Message message = createMessage("{\"collectionName\":\"test\",\"operation\":\"SYNC_DOC\"}");
        Channel channel = mock(Channel.class);

        consumer.onMessage(message, channel);

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("MilvusSyncConsumer"), contains("docId"));
    }
}
