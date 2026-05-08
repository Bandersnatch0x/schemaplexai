package com.schemaplexai.task.mq.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * MQ payload for notification delivery requests.
 * Supports email, SMS, webhook, and in-app channels.
 */
@Data
public class NotificationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String channel;
    private String tenantId;
    private Long userId;
    private String title;
    private String content;
    private String templateCode;
    private Map<String, Object> templateParams;
    private String webhookUrl;
    private String webhookMethod;
    private Map<String, String> webhookHeaders;
    private String idempotencyKey;
}
