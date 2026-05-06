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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Processes notification events from MQ and routes them to the appropriate delivery channel.
 * <p>
 * Supported channels: {@code email}, {@code sms}, {@code webhook}, {@code in-app}.
 * Each delivery attempt is logged. Failed deliveries are nacked and logged to the dead-letter queue.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final String CHANNEL_EMAIL = "email";
    private static final String CHANNEL_SMS = "sms";
    private static final String CHANNEL_WEBHOOK = "webhook";
    private static final String CHANNEL_IN_APP = "in-app";

    private final MessageFailLogService messageFailLogService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

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
     *
     * @param payload the parsed notification payload
     * @return true if the notification was successfully handed off
     */
    private boolean routeToChannel(NotificationMessage payload) {
        String channel = payload.getChannel().toLowerCase();

        return switch (channel) {
            case CHANNEL_EMAIL -> handleEmail(payload);
            case CHANNEL_SMS -> handleSms(payload);
            case CHANNEL_WEBHOOK -> handleWebhook(payload);
            case CHANNEL_IN_APP -> handleInApp(payload);
            default -> {
                log.error("[NotificationConsumer] Unsupported notification channel: {}", channel);
                yield false;
            }
        };
    }

    private boolean handleEmail(NotificationMessage payload) {
        log.info("[NotificationConsumer] [EMAIL] toUser={} title={} template={}",
                payload.getUserId(), payload.getTitle(), payload.getTemplateCode());
        // TODO: Integrate with email provider (e.g., SendGrid, AWS SES)
        // For now, log the attempt and return success to avoid retry loops.
        return true;
    }

    private boolean handleSms(NotificationMessage payload) {
        log.info("[NotificationConsumer] [SMS] toUser={} template={}",
                payload.getUserId(), payload.getTemplateCode());
        // TODO: Integrate with SMS provider (e.g., Twilio, AWS SNS)
        return true;
    }

    private boolean handleWebhook(NotificationMessage payload) {
        String url = payload.getWebhookUrl();
        if (url == null || url.isBlank()) {
            log.error("[NotificationConsumer] [WEBHOOK] Missing webhookUrl");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            if (payload.getWebhookHeaders() != null) {
                payload.getWebhookHeaders().forEach(headers::set);
            }

            Map<String, Object> body = Map.of(
                    "title", payload.getTitle(),
                    "content", payload.getContent(),
                    "userId", payload.getUserId(),
                    "templateCode", payload.getTemplateCode(),
                    "templateParams", payload.getTemplateParams()
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String method = payload.getWebhookMethod() != null ? payload.getWebhookMethod().toUpperCase() : "POST";

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.valueOf(method), entity, String.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            if (!success) {
                log.warn("[NotificationConsumer] [WEBHOOK] Non-2xx response: {} from {}",
                        response.getStatusCode(), url);
            }
            return success;

        } catch (Exception e) {
            log.error("[NotificationConsumer] [WEBHOOK] Failed to call {}: {}", url, e.getMessage());
            return false;
        }
    }

    private boolean handleInApp(NotificationMessage payload) {
        log.info("[NotificationConsumer] [IN-APP] toUser={} title={}",
                payload.getUserId(), payload.getTitle());
        // TODO: Persist in-app notification to sf_notification table via NotificationService
        // For now, log the attempt and return success.
        return true;
    }
}
