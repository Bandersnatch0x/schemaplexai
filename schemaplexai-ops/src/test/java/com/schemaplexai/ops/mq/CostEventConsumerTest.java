package com.schemaplexai.ops.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.model.event.CostRecordedEvent;
import com.schemaplexai.ops.entity.SfCostRecord;
import com.schemaplexai.ops.mapper.SfCostRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M4.5: Cost Event Consumer Tests")
class CostEventConsumerTest {

    @Mock
    private SfCostRecordMapper costRecordMapper;

    @Mock
    private Channel channel;

    @InjectMocks
    private CostEventConsumer costEventConsumer;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        costEventConsumer = new CostEventConsumer(costRecordMapper, objectMapper);
    }

    @Test
    @DisplayName("Persists cost event to PG sf_cost_record")
    void persistsCostEvent() throws Exception {
        CostRecordedEvent event = new CostRecordedEvent(
                UUID.randomUUID(), 1L, 10L, 2L,
                "gpt-4o", "openai", "chat",
                1000L, 500L, 1500L,
                new BigDecimal("0.0450"), "USD",
                Instant.now());
        Message message = createMessage(event);

        costEventConsumer.onMessage(message, channel);

        ArgumentCaptor<SfCostRecord> captor = ArgumentCaptor.forClass(SfCostRecord.class);
        verify(costRecordMapper).insert(captor.capture());

        SfCostRecord saved = captor.getValue();
        assertThat(saved.getExecutionId()).isEqualTo(1L);
        assertThat(saved.getTenantId()).isEqualTo("10");
        assertThat(saved.getModelName()).isEqualTo("gpt-4o");
        assertThat(saved.getCostAmount()).isEqualByComparingTo(new BigDecimal("0.0450"));
        assertThat(saved.getInputTokens()).isEqualTo(1000L);
        assertThat(saved.getOutputTokens()).isEqualTo(500L);

        verify(channel).basicAck(anyLong(), eq(false));
    }

    @Test
    @DisplayName("Skips message with missing executionId")
    void skipsMissingExecutionId() throws Exception {
        String json = "{\"eventId\":\"" + UUID.randomUUID() + "\",\"tenantId\":10,\"costAmount\":1.0}";
        Message message = new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
        message.getMessageProperties().setDeliveryTag(1L);

        costEventConsumer.onMessage(message, channel);

        verifyNoInteractions(costRecordMapper);
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("Nacks on parse failure")
    void nacksOnParseFailure() throws Exception {
        Message message = new Message("bad json".getBytes(StandardCharsets.UTF_8), new MessageProperties());
        message.getMessageProperties().setDeliveryTag(3L);

        costEventConsumer.onMessage(message, channel);

        verifyNoInteractions(costRecordMapper);
        verify(channel).basicNack(3L, false, false);
    }

    private Message createMessage(CostRecordedEvent event) throws Exception {
        String json = objectMapper.writeValueAsString(event);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(1L);
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }
}
