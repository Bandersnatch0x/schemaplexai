package com.schemaplexai.agent.engine.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.model.event.CostRecordedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests the cost event producer wiring (ticket 919): CostRecordedEvents go to
 * sf.exchange / sf.cost with the eventType header, and publish failures never
 * propagate into the execution that produced the usage.
 */
@ExtendWith(MockitoExtension.class)
class CostRecordedEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private CostRecordedEventPublisher publisher;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        publisher = new CostRecordedEventPublisher(rabbitTemplate, objectMapper);
    }

    private CostRecordedEvent sampleEvent() {
        return new CostRecordedEvent(
                UUID.randomUUID(), 1001L, 10L, 42L,
                "gpt-4o", "OPENAI", "chat",
                1000L, 500L, 1500L,
                null, "USD", Instant.parse("2026-08-30T10:00:00Z"));
    }

    @Test
    void publishCostRecorded_sendsToSfExchangeWithCostRoutingKey() throws Exception {
        CostRecordedEvent event = sampleEvent();

        publisher.publishCostRecorded(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagePostProcessor> postProcessorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(CommonConstants.EXCHANGE_SCHEMAPLEXAI),
                eq(CommonConstants.RK_COST),
                payloadCaptor.capture(),
                postProcessorCaptor.capture());

        // Payload round-trips to the shared consumer contract (CostRecordedEvent)
        CostRecordedEvent parsed = objectMapper.readValue(payloadCaptor.getValue(), CostRecordedEvent.class);
        assertEquals(event.eventId(), parsed.eventId());
        assertEquals(1001L, parsed.executionId());
        assertEquals(10L, parsed.tenantId());
        assertEquals(42L, parsed.agentId());
        assertEquals("gpt-4o", parsed.modelName());
        assertEquals("OPENAI", parsed.provider());
        assertEquals(1000L, parsed.inputTokens());
        assertEquals(500L, parsed.outputTokens());
        assertEquals(1500L, parsed.totalTokens());

        // Header matches the producer contract (costRecordedEvent.groovy)
        Message message = new Message(payloadCaptor.getValue().getBytes(StandardCharsets.UTF_8),
                new MessageProperties());
        Message processed = postProcessorCaptor.getValue().postProcessMessage(message);
        assertEquals(CostRecordedEventPublisher.EVENT_TYPE_COST_RECORDED,
                processed.getMessageProperties().getHeader(CostRecordedEventPublisher.EVENT_TYPE_HEADER));
        assertEquals("application/json", processed.getMessageProperties().getContentType());
    }

    @Test
    void publishCostRecorded_usesSfCostRoutingKeyConstant() {
        assertEquals("sf.cost", CommonConstants.RK_COST);
        assertEquals("sf.exchange", CommonConstants.EXCHANGE_SCHEMAPLEXAI);

        publisher.publishCostRecorded(sampleEvent());

        verify(rabbitTemplate).convertAndSend(eq("sf.exchange"), eq("sf.cost"), any(), any(MessagePostProcessor.class));
    }

    @Test
    void publishCostRecorded_nullEvent_doesNotPublish() {
        publisher.publishCostRecorded(null);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void publishCostRecorded_brokerFailure_doesNotPropagate() {
        doThrow(new org.springframework.amqp.AmqpConnectException(new IllegalStateException("broker down")))
                .when(rabbitTemplate)
                .convertAndSend(any(String.class), any(String.class), any(), any(MessagePostProcessor.class));

        assertDoesNotThrow(() -> publisher.publishCostRecorded(sampleEvent()));
    }
}
