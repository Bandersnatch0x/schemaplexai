package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.dao.mapper.notification.NotificationMapper;
import com.schemaplexai.model.entity.notification.Notification;
import com.schemaplexai.quality.service.InboxDeduplicationService;
import com.schemaplexai.task.mq.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Processes notification events from MQ and routes them to the in-app delivery channel.
 * <p>
 * v1: Only {@code in-app} notifications are delivered. All other channels
 * (email, sms, webhook) are rejected and routed to the dead-letter queue (DLQ).
 * Each delivery attempt is logged. Failed deliveries are nacked and logged to the dead-letter queue.
 */
@Slf4j
@Component
@ConditionalOnBean(InboxDeduplicationService.class)
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final String CHANNEL_IN_APP = "in-app";

    private final MessageFailLogService messageFailLogService;
    private final ObjectMapper objectMapper;
    private final InboxDeduplicationService dedupService;
    private final NotificationMapper notificationMapper;

    @RabbitListener(queues = "sf.notification.queue")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        UUID eventId = null;

        try {
            log.info("[NotificationConsumer] Received notification message: {}", body);

            NotificationMessage payload = objectMapper.readValue(body, NotificationMessage.class);
            eventId = resolveEventId(payload, body);

            if (dedupService.isProcessed(eventId, "NotificationConsumer")) {
                log.warn("[NotificationConsumer] Duplicate notification detected for eventId={}, skipping", eventId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            if (payload.getChannel() == null || payload.getChannel().isBlank()) {
                log.error("[NotificationConsumer] Missing channel in message: {}", body);
                throw new BaseException(ResultCode.PARAM_ERROR, "notification channel is required");
            }

            boolean delivered = routeToChannel(payload);

            if (delivered) {
                dedupService.markProcessed(eventId, "NotificationConsumer");
                log.info("[NotificationConsumer] Notification delivered via {} for user {}",
                        payload.getChannel(), payload.getUserId());
                channel.basicAck(deliveryTag, false);
            } else {
                log.warn("[NotificationConsumer] Delivery returned false for channel: {}", payload.getChannel());
                recordFailLog(message, "Delivery returned false for channel: " + payload.getChannel(), deliveryTag);
                channel.basicNack(deliveryTag, false, false);
            }

        } catch (BaseException e) {
            log.error("[NotificationConsumer] Business error processing message: {}", body, e);
            recordFailLog(message, e.getMessage(), deliveryTag);
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("[NotificationConsumer] Failed to process message: {}", body, e);
            recordFailLog(message, e.getMessage(), deliveryTag);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * Routes the notification to the appropriate channel handler.
     * <p>
     * v1: Only in-app notifications are delivered. All other channels
     * (email, sms, webhook) are rejected and routed to the DLQ.
     *
     * @param payload the parsed notification payload
     * @return true if the notification was successfully handed off
     */
    private boolean routeToChannel(NotificationMessage payload) {
        String channel = payload.getChannel().toLowerCase();

        if (CHANNEL_IN_APP.equals(channel)) {
            return handleInApp(payload);
        }

        log.warn("[NotificationConsumer] Non-in-app channel rejected for DLQ: channel={}, user={}",
                channel, payload.getUserId());
        return false;
    }

    private boolean handleInApp(NotificationMessage payload) {
        Notification notification = new Notification();
        notification.setTenantId(payload.getTenantId());
        notification.setUserId(payload.getUserId());
        notification.setTitle(payload.getTitle());
        notification.setContent(payload.getContent());
        notification.setType("IN_APP");
        notification.setRead(false);

        int inserted = notificationMapper.insert(notification);
        log.info("[NotificationConsumer] [IN-APP] persisted notificationId={} toUser={} title={}",
                notification.getId(), payload.getUserId(), payload.getTitle());
        return inserted > 0;
    }

    private void recordFailLog(Message message, String errorMessage, long deliveryTag) {
        boolean persisted = messageFailLogService.log(message, this.getClass().getSimpleName(), errorMessage);
        if (!persisted) {
            log.warn("[NotificationConsumer] Message fail log persistence returned false for deliveryTag={}",
                    deliveryTag);
        }
    }

    private UUID resolveEventId(NotificationMessage payload, String body) {
        if (payload.getIdempotencyKey() != null && !payload.getIdempotencyKey().isBlank()) {
            return UUID.nameUUIDFromBytes(payload.getIdempotencyKey().getBytes(StandardCharsets.UTF_8));
        }
        return UUID.nameUUIDFromBytes(body.getBytes(StandardCharsets.UTF_8));
    }
}
