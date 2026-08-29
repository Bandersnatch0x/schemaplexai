package com.schemaplexai.quality.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.quality.gate.QualityContext;
import com.schemaplexai.quality.gate.QualityReport;
import com.schemaplexai.quality.mq.dto.QualityCheckRequestEvent;
import com.schemaplexai.quality.service.InboxDeduplicationService;
import com.schemaplexai.quality.service.QualityOrchestrator;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes quality check request events published by the Agent engine
 * (routing key {@code sf.quality} on {@code sf.exchange}), runs the gate
 * evaluation in-process and publishes the structured verdict back via
 * {@link QualityVerdictPublisher} (ticket 924 / REQ-01 / REQ-18).
 *
 * <p>The consumer lives in the quality module itself because the task/web
 * runtimes do not component-scan {@code com.schemaplexai.quality}
 * (their {@code scanBasePackages} excludes it), so gate beans are only
 * instantiated here.
 *
 * <p>Note: the task module's legacy {@code sf.quality.queue} consumer keeps
 * its own binding to the same routing key; it receives a copy of every event
 * and dead-letters it (its handler is the pre-existing unsupported stub).
 * This duplication is recorded in ticket 924 for follow-up retirement.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QualityCheckEventConsumer {

    static final String QUEUE_NAME = "sf.quality.gate.check.queue";
    private static final String CONSUMER_NAME = "QualityCheckEventConsumer";

    private final QualityOrchestrator qualityOrchestrator;
    private final QualityVerdictPublisher verdictPublisher;
    private final InboxDeduplicationService inboxDeduplicationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = QUEUE_NAME, durable = "true"),
                    exchange = @Exchange(value = CommonConstants.EXCHANGE_SCHEMAPLEXAI, type = "direct"),
                    key = CommonConstants.RK_QUALITY
            )
    )
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            QualityCheckRequestEvent event = objectMapper.readValue(body, QualityCheckRequestEvent.class);
            if (event == null || event.getExecutionId() == null) {
                log.error("[QualityCheckEventConsumer] Missing executionId, dropping message: {}", body);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // Inbox deduplication (MQ redelivery must not double-record issues)
            UUID eventId = parseEventId(event.getEventId());
            if (eventId != null && inboxDeduplicationService.isProcessed(eventId, CONSUMER_NAME)) {
                log.debug("[QualityCheckEventConsumer] Skipping already processed event: {}", eventId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            if (event.getTenantId() != null) {
                TenantContextHolder.setTenantId(event.getTenantId());
            }

            try {
                QualityReport report = qualityOrchestrator.evaluate(
                        event.getExecutionId(), buildContext(event));

                verdictPublisher.publishVerdict(event.getExecutionId(), event.getAgentId(),
                        event.getTenantId(), event.getTriggerPoint(), report);

                if (eventId != null) {
                    inboxDeduplicationService.markProcessed(eventId, CONSUMER_NAME);
                }
            } finally {
                TenantContextHolder.clear();
            }

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[QualityCheckEventConsumer] Failed to process message: {}", body, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private QualityContext buildContext(QualityCheckRequestEvent event) {
        Map<String, Object> metadata = new HashMap<>();
        if (event.getOutput() != null) {
            metadata.put("output", event.getOutput());
        }
        if (event.getTriggerPoint() != null) {
            metadata.put("triggerPoint", event.getTriggerPoint());
        }
        if (event.getAgentId() != null) {
            metadata.put("agentId", event.getAgentId());
        }
        // Security screening evidence produced by the engine's guardrail pass
        // (only published from the COMPLETED path) — consumed by SecurityScanRule.
        if (event.getSecurityScanCompleted() != null) {
            metadata.put("securityScanCompleted", event.getSecurityScanCompleted());
        }
        if (event.getSecurityScanPassed() != null) {
            metadata.put("securityScanPassed", event.getSecurityScanPassed());
        }
        return new QualityContext(event.getExecutionId(), null, metadata);
    }

    private UUID parseEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            log.warn("[QualityCheckEventConsumer] Event without eventId — deduplication skipped");
            return null;
        }
        try {
            return UUID.fromString(eventId);
        } catch (IllegalArgumentException e) {
            log.warn("[QualityCheckEventConsumer] Non-UUID eventId '{}' — deduplication skipped", eventId);
            return null;
        }
    }
}
