package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.mq.dto.QualityEventMessage;
import com.schemaplexai.task.service.QualityEventRequestHandler;
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
    private final ObjectMapper objectMapper;
    private final QualityEventRequestHandler qualityEventRequestHandler;

    @RabbitListener(queues = "sf.quality.queue")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            log.info("[QualityEventConsumer] Received quality event message: {}", body);
            QualityEventMessage payload = objectMapper.readValue(body, QualityEventMessage.class);
            validatePayload(payload);
            qualityEventRequestHandler.handle(payload);
            channel.basicAck(deliveryTag, false);
        } catch (BaseException e) {
            log.error("[QualityEventConsumer] Business error processing message: {}", body, e);
            recordFailLog(message, e.getMessage(), deliveryTag);
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("[QualityEventConsumer] Failed to process message: {}", body, e);
            recordFailLog(message, e.getMessage(), deliveryTag);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void validatePayload(QualityEventMessage payload) {
        if (payload == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "quality event payload must be a JSON object");
        }
        if (payload.getEventType() == null || payload.getEventType().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "quality eventType is required");
        }
    }

    private void recordFailLog(Message message, String errorMessage, long deliveryTag) {
        boolean persisted = messageFailLogService.log(message, this.getClass().getSimpleName(), errorMessage);
        if (!persisted) {
            log.warn("[QualityEventConsumer] Message fail log persistence returned false for deliveryTag={}",
                    deliveryTag);
        }
    }
}
