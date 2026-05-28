package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.model.event.ExecutionEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes dead letter queue events for permanently failed outbox entries.
 *
 * <p>Flow:
 * <ol>
 *   <li>Listens on {@code execution.dead-letter} queue</li>
 *   <li>Parses payload as {@link ExecutionEventMessage}</li>
 *   <li>Logs at ERROR level with full payload</li>
 *   <li>Publishes an audit event to {@code execution_events} exchange with
 *       eventType="DEAD_LETTER" and severity="CRITICAL" in payload</li>
 *   <li>Acks the message after processing</li>
 * </ol>
 *
 * <p>The audit event is consumed by {@code AuditEventConsumer} in the quality module
 * and projected into {@code sf_audit_event}.
 *
 * <p>Parse errors are caught and logged to prevent infinite retry loops.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterHandler {

    private static final String EXCHANGE_NAME = "execution_events";
    private static final String AUDIT_TOPIC = "audit.dead-letter";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "execution.dead-letter")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        ExecutionEventMessage event;

        try {
            event = objectMapper.readValue(body, ExecutionEventMessage.class);
        } catch (Exception e) {
            log.error("[DeadLetterHandler] Failed to parse dead letter payload: {}", body, e);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            log.error("[DEAD_LETTER] Permanently failed outbox entry: eventId={}, executionId={}, seq={}, eventType={}, payload={}",
                    event.eventId(), event.executionId(), event.seq(), event.eventType(), body);

            publishAuditEvent(event, body);

            log.debug("[DeadLetterHandler] Audit event published for dead letter: eventId={}", event.eventId());

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[DeadLetterHandler] Failed to publish audit event for dead letter: eventId={}",
                    event.eventId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void publishAuditEvent(ExecutionEventMessage originalEvent, String rawPayload) throws IOException {
        Map<String, Object> auditPayload = Map.of(
                "severity", "CRITICAL",
                "originalEventType", originalEvent.eventType(),
                "rawPayload", rawPayload,
                "seq", originalEvent.seq(),
                "agentId", originalEvent.agentId()
        );

        ExecutionEventMessage auditEvent = new ExecutionEventMessage(
                UUID.randomUUID(),
                originalEvent.executionId(),
                originalEvent.seq(),
                "DEAD_LETTER",
                objectMapper.writeValueAsString(auditPayload),
                Instant.now(),
                originalEvent.tenantId(),
                originalEvent.agentId(),
                "AUDIT"
        );

        rabbitTemplate.convertAndSend(EXCHANGE_NAME, AUDIT_TOPIC, objectMapper.writeValueAsString(auditEvent));
    }
}
