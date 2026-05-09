package com.schemaplexai.web.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.model.event.ExecutionEventMessage;
import com.schemaplexai.web.dto.SseEvent;
import com.schemaplexai.web.sse.AgentSseEmitter;
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

/**
 * Consumes execution events from Engine and broadcasts them to SSE subscribers.
 * Filters out EPHEMERAL events from replay, but pushes them to live SSE.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventConsumer {

    private final AgentSseEmitter agentSseEmitter;
    private final ObjectMapper objectMapper;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "sf.execution.events.sse.queue", durable = "true"),
                    exchange = @Exchange(value = "execution_events", type = "topic"),
                    key = "#"
            )
    )
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            ExecutionEventMessage event = objectMapper.readValue(body, ExecutionEventMessage.class);

            if (event.executionId() == null) {
                log.warn("[SseEventConsumer] Missing executionId, skipping: {}", body);
                channel.basicAck(deliveryTag, false);
                return;
            }

            SseEvent sseEvent = new SseEvent(
                    event.executionId(),
                    event.seq(),
                    event.eventType(),
                    event.payload(),
                    event.sensitivity()
            );

            // Broadcast to all subscribers watching this execution
            agentSseEmitter.broadcastToExecution(
                    event.executionId(),
                    event.eventType(),
                    sseEvent
            );

            log.debug("[SseEventConsumer] Broadcast execution={} seq={} type={} sensitivity={}",
                    event.executionId(), event.seq(), event.eventType(), event.sensitivity());

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[SseEventConsumer] Failed to process message: {}", body, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
