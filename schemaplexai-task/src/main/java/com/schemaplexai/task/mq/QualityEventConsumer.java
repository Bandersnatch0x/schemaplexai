package com.schemaplexai.task.mq;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Processes quality gate events from MQ.
 * <p>
 * TODO: Implement the following:
 * <ol>
 *   <li>Parse the MQ message payload into a QualityEventMessage DTO (define fields: eventType, projectId, commitSha, ruleId, severity, details, tenantId).</li>
 *   <li>Route by event type:
 *       <ul>
 *         <li>{@code DRIFT_DETECTED} - Trigger drift analysis workflow, notify project owners.</li>
 *         <li>{@code SECURITY_SCAN_FAILED} - Block deployment pipeline, create incident ticket.</li>
 *         <li>{@code COVERAGE_DROP} - Alert engineering team, schedule remediation.</li>
 *         <li>{@code LINT_VIOLATION} - Aggregate violations, generate report.</li>
 *       </ul>
 *   </li>
 *   <li>Delegate to QualityGateService (to be created in quality module) for rule evaluation.</li>
 *   <li>Persist event to sf_quality_event table for audit trail.</li>
 *   <li>On failure, nack the message so it routes to the dead-letter queue for retry.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QualityEventConsumer {

    private final MessageFailLogService messageFailLogService;

    @RabbitListener(queues = "sf.quality.queue")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            log.info("[QualityEventConsumer] Received quality event message: {}", body);
            // TODO: Parse payload, route by event type, delegate to QualityGateService, persist audit event
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[QualityEventConsumer] Failed to process message: {}", body, e);
            messageFailLogService.log(message, this.getClass().getSimpleName(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
