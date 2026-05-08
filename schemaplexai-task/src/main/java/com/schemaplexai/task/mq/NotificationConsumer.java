package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.mq.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Processes notification events from MQ and routes them to the in-app delivery channel.
 * <p>
 * v1: Only {@code in-app} notifications are delivered. All other channels
 * (email, sms, webhook) are rejected and routed to the dead-letter queue (DLQ).
 * Each delivery attempt is logged. Failed deliveries are nacked and logged to the dead-letter queue.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final String CHANNEL_IN_APP = "in-app";

    private final MessageFailLogService messageFailLogService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "sf.notification.queue")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            log.info("[NotificationConsumer] Received notification message: {}", body);

            NotificationMessage payload = objectMapper.readValue(body, NotificationMessage.class);

            if (payload.getChannel() == null || payload.getChannel().isBlank()) {
                log.error("[NotificationConsumer] Missing channel in message: {}", body);
                throw new BaseException(ResultCode.PARAM_ERROR, "notification channel is required");
            }

            boolean delivered = routeToChannel(payload);

            if (delivered) {
                log.info("[NotificationConsumer] Notification delivered via {} for user {}",
                        payload.getChannel(), payload.getUserId());
                channel.basicAck(deliveryTag, false);
            } else {
                log.warn("[NotificationConsumer] Delivery returned false for channel: {}", payload.getChannel());
                messageFailLogService.log(message, this.getClass().getSimpleName(),
                        "Delivery returned false for channel: " + payload.getChannel());
                channel.basicNack(deliveryTag, false, false);
            }

        } catch (BaseException e) {
            log.error("[NotificationConsumer] Business error processing message: {}", body, e);
            messageFailLogService.log(message, this.getClass().getSimpleName(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("[NotificationConsumer] Failed to process message: {}", body, e);
            messageFailLogService.log(message, this.getClass().getSimpleName(), e.getMessage());
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
        log.info("[NotificationConsumer] [IN-APP] toUser={} title={}",
                payload.getUserId(), payload.getTitle());
        // TODO: Persist in-app notification to sf_notification table via NotificationService
        // For now, log the attempt and return success.
        return true;
    }
}
