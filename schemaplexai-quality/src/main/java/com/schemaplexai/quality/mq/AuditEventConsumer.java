package com.schemaplexai.quality.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.model.event.ExecutionEventMessage;
import com.schemaplexai.quality.entity.SfAuditEvent;
import com.schemaplexai.quality.service.AuditEventService;
import com.schemaplexai.quality.service.InboxDeduplicationService;
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
import java.util.UUID;

/**
 * Consumes execution events from Engine and projects them into sf_audit_event.
 * Computes SHA-256 content_hash for tamper detection.
 * Only processes AUDIT sensitivity events; skips DEBUG and EPHEMERAL.
 *
 * M6.5: Integrated inbox deduplication via sf_processed_event.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private static final String CONSUMER_NAME = "AuditEventConsumer";

    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;
    private final InboxDeduplicationService inboxDeduplicationService;

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
            UUID eventId = event.eventId();

            // M6.5: Inbox deduplication check
            if (inboxDeduplicationService.isProcessed(eventId, CONSUMER_NAME)) {
                log.debug("[AuditEventConsumer] Skipping already processed event: eventId={}", eventId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // Skip non-AUDIT events
            if (!"AUDIT".equals(event.sensitivity())) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            // Set tenant context for downstream multi-tenant operations
            if (event.tenantId() != null) {
                TenantContextHolder.setTenantId(String.valueOf(event.tenantId()));
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
            auditEvent.setEventId(eventId);
            auditEvent.setOccurredAt(event.occurredAt() != null
                    ? LocalDateTime.ofInstant(event.occurredAt(), ZoneId.systemDefault())
                    : LocalDateTime.now());
            auditEvent.setContentHash(computeContentHash(event));

            auditEventService.save(auditEvent);

            // M6.5: Mark as processed after successful handling
            inboxDeduplicationService.markProcessed(eventId, CONSUMER_NAME);

            log.debug("[AuditEventConsumer] Projected audit event execution={} seq={} type={}",
                    event.executionId(), event.seq(), event.eventType());

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[AuditEventConsumer] Failed to process message: {}", body, e);
            channel.basicNack(deliveryTag, false, false);
        } finally {
            TenantContextHolder.clear();
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
