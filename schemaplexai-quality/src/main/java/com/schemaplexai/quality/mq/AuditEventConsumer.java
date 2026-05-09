package com.schemaplexai.quality.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.model.event.ExecutionEventMessage;
import com.schemaplexai.quality.entity.SfAuditEvent;
import com.schemaplexai.quality.service.AuditEventService;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

/**
 * Consumes execution events from Engine and projects them into sf_audit_event.
 * Computes SHA-256 content_hash for tamper detection.
 * Only processes AUDIT sensitivity events; skips DEBUG and EPHEMERAL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "sf.execution.events.audit.queue", durable = "true"),
                    exchange = @Exchange(value = "execution_events", type = "topic"),
                    key = "#"
            )
    )
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            ExecutionEventMessage event = objectMapper.readValue(body, ExecutionEventMessage.class);

            // Skip non-AUDIT events
            if (!"AUDIT".equals(event.sensitivity())) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            SfAuditEvent auditEvent = new SfAuditEvent();
            auditEvent.setEventType(event.eventType());
            auditEvent.setResourceType("EXECUTION");
            auditEvent.setResourceId(event.executionId());
            auditEvent.setAction(event.eventType());
            auditEvent.setDetailsJson(event.payload());
            auditEvent.setUserId(null); // Engine events have no direct user
            auditEvent.setExecutionId(event.executionId());
            auditEvent.setTenantId(event.tenantId() != null ? String.valueOf(event.tenantId()) : null);
            auditEvent.setEventId(event.eventId());
            auditEvent.setOccurredAt(event.occurredAt() != null
                    ? LocalDateTime.ofInstant(event.occurredAt(), ZoneId.systemDefault())
                    : LocalDateTime.now());
            auditEvent.setContentHash(computeContentHash(event));

            auditEventService.save(auditEvent);

            log.debug("[AuditEventConsumer] Projected audit event execution={} seq={} type={}",
                    event.executionId(), event.seq(), event.eventType());

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[AuditEventConsumer] Failed to process message: {}", body, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    String computeContentHash(ExecutionEventMessage event) {
        try {
            String raw = event.eventType() + "|" + event.payload() + "|" + event.occurredAt();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
