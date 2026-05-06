package com.schemaplexai.integration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.integration.entity.SfIntegration;

import java.util.List;
import java.util.Map;

public interface IntegrationService extends IService<SfIntegration> {

    /**
     * Register a webhook for the given integration.
     */
    void registerWebhook(Long integrationId, String webhookUrl, String eventType);

    /**
     * List all webhooks for the given integration.
     */
    List<Map<String, Object>> listWebhooks(Long integrationId);

    /**
     * Delete a webhook by its ID.
     */
    void deleteWebhook(Long webhookId);

    /**
     * Aggregate health status across all integrations.
     */
    Map<String, Object> aggregateHealthStatus();
}
