package com.schemaplexai.task.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.model.event.ExecutionEventMessage;
import com.schemaplexai.ops.service.CostService;
import com.schemaplexai.task.mq.MessageFailLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Consumes execution events from MQ for cost projection.
 * <p>
 * Reads {@link ExecutionEventMessage} payloads from the {@code sf.execution.event.queue},
 * delegates to {@link CostService#processExecutionEvent} for cost calculation and persistence.
 */
@Slf4j
@Component
@ConditionalOnBean(CostService.class)
@RequiredArgsConstructor
public class ExecutionEventConsumer {

    private final CostService costService;
    private final MessageFailLogService messageFailLogService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "sf.execution.event.queue")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            log.info("[ExecutionEventConsumer] Received execution event: {}", body);

            ExecutionEventMessage event = objectMapper.readValue(body, ExecutionEventMessage.class);
            costService.processExecutionEvent(event);

            log.info("[ExecutionEventConsumer] Processed execution event: eventId={}, eventType={}",
                    event.eventId(), event.eventType());

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[ExecutionEventConsumer] Failed to process execution event: {}", body, e);
            recordFailLog(message, e.getMessage(), deliveryTag);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void recordFailLog(Message message, String errorMessage, long deliveryTag) {
        boolean persisted = messageFailLogService.log(message, this.getClass().getSimpleName(), errorMessage);
        if (!persisted) {
            log.warn("[ExecutionEventConsumer] Message fail log persistence returned false for deliveryTag={}",
                    deliveryTag);
        }
    }
}
