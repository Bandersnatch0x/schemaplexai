package com.schemaplexai.ops.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.model.event.CostRecordedEvent;
import com.schemaplexai.ops.entity.SfCostRecord;
import com.schemaplexai.ops.mapper.SfCostRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Consumes cost-recorded events from Engine and persists to PG sf_cost_record.
 * v1 short-path: PG stores cost records directly.
 * v1.1: dual-write to ClickHouse for analytics.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CostEventConsumer {

    private final SfCostRecordMapper costRecordMapper;
    private final ObjectMapper objectMapper;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "sf.execution.events.cost.queue", durable = "true"),
                    exchange = @Exchange(value = "execution_events", type = "topic"),
                    key = "cost.*"
            )
    )
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            CostRecordedEvent event = objectMapper.readValue(body, CostRecordedEvent.class);

            if (event.executionId() == null || event.tenantId() == null) {
                log.warn("[CostEventConsumer] Missing executionId or tenantId, skipping: {}", body);
                channel.basicAck(deliveryTag, false);
                return;
            }

            SfCostRecord record = new SfCostRecord();
            record.setTenantId(event.tenantId() != null ? String.valueOf(event.tenantId()) : null);
            record.setRecordId(event.eventId() != null ? event.eventId().toString() : null);
            record.setServiceName("agent-engine");
            record.setModelName(event.modelName());
            record.setProvider(event.provider());
            record.setRequestType(event.requestType());
            record.setInputTokens(event.inputTokens());
            record.setOutputTokens(event.outputTokens());
            record.setTotalTokens(event.totalTokens());
            record.setCostAmount(event.costAmount());
            record.setCurrency(event.currency());
            record.setOccurredAt(event.occurredAt() != null
                    ? LocalDateTime.ofInstant(event.occurredAt(), ZoneId.systemDefault())
                    : LocalDateTime.now());
            record.setExecutionId(event.executionId());
            record.setAgentId(event.agentId());

            costRecordMapper.insert(record);

            log.debug("[CostEventConsumer] Persisted cost record execution={} amount={} {}",
                    event.executionId(), event.costAmount(), event.currency());

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[CostEventConsumer] Failed to process message: {}", body, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
