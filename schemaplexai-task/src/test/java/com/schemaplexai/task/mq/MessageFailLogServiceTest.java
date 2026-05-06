package com.schemaplexai.task.mq;

import com.schemaplexai.task.entity.SfMessageFailLog;
import com.schemaplexai.task.mapper.SfMessageFailLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageFailLogServiceTest {

    @Mock
    private SfMessageFailLogMapper messageFailLogMapper;

    @InjectMocks
    private MessageFailLogService messageFailLogService;

    private Message createMessage(String messageId, String exchange, String routingKey, String body) {
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(messageId);
        properties.setReceivedExchange(exchange);
        properties.setReceivedRoutingKey(routingKey);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    @Test
    void log_savesMessageFailLogWithCorrectFields() {
        Message message = createMessage("msg-001", "sf.exchange", "sf.agent.execute",
                "{\"agentId\": 1, \"input\": \"test\"}");

        messageFailLogService.log(message, "TestConsumer", "Connection timeout");

        ArgumentCaptor<SfMessageFailLog> captor = ArgumentCaptor.forClass(SfMessageFailLog.class);
        verify(messageFailLogMapper).insert(captor.capture());
        SfMessageFailLog log = captor.getValue();
        assertThat(log.getMessageId()).isEqualTo("msg-001");
        assertThat(log.getExchange()).isEqualTo("sf.exchange");
        assertThat(log.getRoutingKey()).isEqualTo("sf.agent.execute");
        assertThat(log.getPayload()).isEqualTo("{\"agentId\": 1, \"input\": \"test\"}");
        assertThat(log.getErrorMsg()).isEqualTo("Connection timeout");
        assertThat(log.getConsumerGroup()).isEqualTo("TestConsumer");
        assertThat(log.getStatus()).isEqualTo("PENDING");
        assertThat(log.getRetryCount()).isEqualTo(0);
    }

    @Test
    void log_databaseException_isSwallowed() {
        Message message = createMessage("msg-002", "sf.exchange", "sf.notification", "payload");
        doThrow(new RuntimeException("DB connection failed"))
                .when(messageFailLogMapper).insert(any());

        messageFailLogService.log(message, "TestConsumer", "Some error");

        verify(messageFailLogMapper).insert(any());
    }

    @Test
    void log_unicodePayload_preservesCharacters() {
        Message message = createMessage("msg-003", "sf.exchange", "sf.quality",
                "{\"message\": \"你好世界\"}");

        messageFailLogService.log(message, "QualityConsumer", "Validation error");

        ArgumentCaptor<SfMessageFailLog> captor = ArgumentCaptor.forClass(SfMessageFailLog.class);
        verify(messageFailLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo("{\"message\": \"你好世界\"}");
    }
}
