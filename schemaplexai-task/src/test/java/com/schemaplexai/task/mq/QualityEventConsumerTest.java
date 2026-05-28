package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.mq.dto.QualityEventMessage;
import com.schemaplexai.task.service.QualityEventRequestHandler;
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

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class QualityEventConsumerTest {

    @Mock
    private MessageFailLogService messageFailLogService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private QualityEventRequestHandler qualityEventRequestHandler;

    @InjectMocks
    private QualityEventConsumer consumer;

    private Message createMessage(String body) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    @Test
    void onMessage_validMessage_delegatesAndAcks() throws Exception {
        Message message = createMessage("""
                {
                  "eventType": "DRIFT_DETECTED",
                  "projectId": "project-1",
                  "commitSha": "abc123",
                  "ruleId": "coverage-drop",
                  "severity": "HIGH",
                  "details": {"delta": -12},
                  "tenantId": "tenant-1"
                }
                """);
        Channel channel = mock(Channel.class);

        consumer.onMessage(message, channel);

        verify(qualityEventRequestHandler).handle(argThat(payload ->
                "DRIFT_DETECTED".equals(payload.getEventType())
                        && "project-1".equals(payload.getProjectId())
                        && "abc123".equals(payload.getCommitSha())
                        && "coverage-drop".equals(payload.getRuleId())
                        && "HIGH".equals(payload.getSeverity())
                        && "tenant-1".equals(payload.getTenantId())
                        && payload.getDetails() != null
                        && payload.getDetails().get("delta").asInt() == -12));
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

    @Test
    void onMessage_handlerNotImplemented_nacksAndLogs() throws Exception {
        Message message = createMessage("{\"eventType\":\"DRIFT_DETECTED\"}");
        Channel channel = mock(Channel.class);
        doThrow(new BaseException(ResultCode.INTERNAL_ERROR, "quality event handler is not implemented"))
                .when(qualityEventRequestHandler).handle(any(QualityEventMessage.class));

        consumer.onMessage(message, channel);

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("QualityEventConsumer"), contains("not implemented"));
    }

    @Test
    void onMessage_failLogPersistenceFalse_warnsAndNacks(CapturedOutput output) throws Exception {
        Message message = createMessage("{\"eventType\":\"DRIFT_DETECTED\"}");
        Channel channel = mock(Channel.class);
        doThrow(new BaseException(ResultCode.INTERNAL_ERROR, "quality event handler is not implemented"))
                .when(qualityEventRequestHandler).handle(any(QualityEventMessage.class));
        when(messageFailLogService.log(eq(message), eq("QualityEventConsumer"), anyString()))
                .thenReturn(false);

        consumer.onMessage(message, channel);

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("QualityEventConsumer"), anyString());
        assertThat(output).contains("[QualityEventConsumer] Message fail log persistence returned false");
    }

    @Test
    void onMessage_invalidJson_nacksAndLogs() throws Exception {
        Message message = createMessage("invalid-json");
        Channel channel = mock(Channel.class);

        consumer.onMessage(message, channel);

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("QualityEventConsumer"), anyString());
    }

    @Test
    void onMessage_missingEventType_nacksAndLogs() throws Exception {
        Message message = createMessage("{\"projectId\":\"project-1\"}");
        Channel channel = mock(Channel.class);

        consumer.onMessage(message, channel);

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel).basicNack(1L, false, false);
        verify(messageFailLogService).log(eq(message), eq("QualityEventConsumer"), contains("eventType"));
    }
}
