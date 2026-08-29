package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.model.event.CostRecordedEvent;
import com.schemaplexai.ops.entity.SfCostRecord;
import com.schemaplexai.ops.mapper.AiModelPriceMapper;
import com.schemaplexai.ops.mapper.BudgetMapper;
import com.schemaplexai.ops.mapper.SfCostRecordMapper;
import com.schemaplexai.ops.service.BudgetAlertNotifier;
import com.schemaplexai.ops.service.BudgetService;
import com.schemaplexai.ops.service.CostDataSyncService;
import com.schemaplexai.ops.service.CostService;
import com.schemaplexai.quality.service.InboxDeduplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Ticket 919 end-to-end collection chain (in-memory, no broker):
 * engine producer payload → sf.cost consumer → CostService pricing →
 * sf_cost_record insert with model / input-output tokens / execution identity.
 *
 * <p>The producer side is simulated byte-for-byte: the engine's
 * CostRecordedEventPublisher serializes the shared CostRecordedEvent model
 * with Jackson and stamps the {@code eventType=CostRecordedEvent} header,
 * exactly as reproduced here.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("919: end-to-end cost collection chain (call -> event -> record)")
class CostCollectionChainTest {

    @Mock
    private CostDataSyncService costDataSyncService;

    @Mock
    private MessageFailLogService messageFailLogService;

    @Mock
    private InboxDeduplicationService dedupService;

    @Mock
    private BudgetMapper budgetMapper;

    @Mock
    private SfCostRecordMapper costRecordMapper;

    @Mock
    private BudgetService budgetService;

    @Mock
    private AiModelPriceMapper aiModelPriceMapper;

    @Mock
    private BudgetAlertNotifier budgetAlertNotifier;

    private CostSyncConsumer consumer;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        // Real CostService: authoritative pricing + persistence logic
        CostService costService = new CostService(budgetMapper, costRecordMapper, budgetService,
                objectMapper, aiModelPriceMapper, budgetAlertNotifier);
        consumer = new CostSyncConsumer(costDataSyncService, messageFailLogService, objectMapper, dedupService);
        ReflectionTestUtils.setField(consumer, "costService", costService);
    }

    @Test
    @DisplayName("LLM usage event becomes an sf_cost_record row with model/tokens/execution identity")
    void llmCallEvent_flowsIntoCostRecord() throws Exception {
        // --- producer side (engine CostRecordedEventPublisher behaviour) ---
        UUID eventId = UUID.randomUUID();
        CostRecordedEvent published = new CostRecordedEvent(
                eventId, 2001L, 10L, 42L,
                "gpt-4o", "OPENAI", "chat",
                1000L, 500L, 1500L,
                null, "USD", Instant.parse("2026-08-30T10:00:00Z"));
        String payload = objectMapper.writeValueAsString(published);
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        properties.setHeader("eventType", "CostRecordedEvent");
        properties.setContentType("application/json");
        Message message = new Message(payload.getBytes(StandardCharsets.UTF_8), properties);

        when(dedupService.isProcessed(any(UUID.class), eq("CostSyncConsumer"))).thenReturn(false);
        Channel channel = mock(Channel.class);

        // --- consumer side (task CostSyncConsumer -> ops CostService) ---
        consumer.onMessage(message, channel);

        ArgumentCaptor<SfCostRecord> captor = ArgumentCaptor.forClass(SfCostRecord.class);
        verify(costRecordMapper).insert(captor.capture());
        SfCostRecord record = captor.getValue();

        // Acceptance (ticket 919): persisted fields include model, input/output
        // tokens and the execution identity.
        assertEquals("gpt-4o", record.getModelName());
        assertEquals(1000L, record.getInputTokens());
        assertEquals(500L, record.getOutputTokens());
        assertEquals(1500L, record.getTotalTokens());
        assertEquals(2001L, record.getExecutionId());
        assertEquals("10", record.getTenantId());
        assertEquals(42L, record.getAgentId());
        assertEquals(eventId.toString(), record.getRecordId());
        assertEquals("OPENAI", record.getProvider());
        assertEquals("chat", record.getRequestType());
        assertEquals("agent-engine", record.getServiceName());
        assertNotNull(record.getOccurredAt());

        // gpt-4 family fallback pricing: 1000*0.03/1000 + 500*0.06/1000 = 0.06
        assertEquals(0, new BigDecimal("0.060000").compareTo(record.getCostAmount()));
        assertEquals("USD", record.getCurrency());

        verify(budgetService).addUsedAmount(eq("10"), any(BigDecimal.class));
        verify(dedupService).markProcessed(eventId, "CostSyncConsumer");
        verify(channel).basicAck(1L, false);
        verify(costDataSyncService, never()).syncIncrementalData();
    }

    @Test
    @DisplayName("Same event delivered twice is persisted once (idempotency)")
    void duplicateDelivery_persistedOnce() throws Exception {
        UUID eventId = UUID.randomUUID();
        CostRecordedEvent published = new CostRecordedEvent(
                eventId, 2001L, 10L, 42L,
                "gpt-4o", "OPENAI", "chat",
                10L, 5L, 15L, null, "USD", Instant.now());
        String payload = objectMapper.writeValueAsString(published);

        Message first = messageWith(payload, 1L);
        Message second = messageWith(payload, 2L);

        when(dedupService.isProcessed(any(UUID.class), eq("CostSyncConsumer")))
                .thenReturn(false)   // first delivery
                .thenReturn(true);   // redelivery
        Channel channel = mock(Channel.class);

        consumer.onMessage(first, channel);
        consumer.onMessage(second, channel);

        verify(costRecordMapper, times(1)).insert(any(SfCostRecord.class));
        verify(channel).basicAck(1L, false);
        verify(channel).basicAck(2L, false);
    }

    private Message messageWith(String payload, long deliveryTag) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        properties.setHeader("eventType", "CostRecordedEvent");
        return new Message(payload.getBytes(StandardCharsets.UTF_8), properties);
    }
}
