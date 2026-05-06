package com.schemaplexai.integration.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfIntegration;
import com.schemaplexai.integration.mapper.IntegrationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class IntegrationServiceImpl extends ServiceImpl<IntegrationMapper, SfIntegration> implements IntegrationService {

    private final ObjectMapper objectMapper;

    // In-memory webhook registry per integration (key: integrationId)
    private final Map<Long, List<Map<String, Object>>> webhookRegistry = new ConcurrentHashMap<>();
    private final AtomicLong webhookIdGenerator = new AtomicLong(1);

    @Override
    public boolean save(SfIntegration entity) {
        validateIntegration(entity);
        return super.save(entity);
    }

    @Override
    public boolean updateById(SfIntegration entity) {
        validateIntegration(entity);
        return super.updateById(entity);
    }

    private void validateIntegration(SfIntegration entity) {
        if (entity == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Integration entity is required");
        }
        if (entity.getName() == null || entity.getName().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Integration name is required");
        }
        if (entity.getType() == null || entity.getType().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Integration type is required");
        }
    }

    @Override
    public void registerWebhook(Long integrationId, String webhookUrl, String eventType) {
        SfIntegration integration = getById(integrationId);
        if (integration == null) {
            throw new BaseException(ResultCode.INTEGRATION_NOT_FOUND, "Integration not found: " + integrationId);
        }
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Webhook URL is required");
        }

        Map<String, Object> webhook = Map.of(
                "id", webhookIdGenerator.getAndIncrement(),
                "integrationId", integrationId,
                "url", webhookUrl,
                "eventType", eventType != null ? eventType : "all",
                "createdAt", Instant.now().toString(),
                "status", "active"
        );

        webhookRegistry.computeIfAbsent(integrationId, k -> new ArrayList<>()).add(webhook);
        log.info("Webhook registered for integration {}: url={}, eventType={}", integrationId, webhookUrl, eventType);
    }

    @Override
    public List<Map<String, Object>> listWebhooks(Long integrationId) {
        SfIntegration integration = getById(integrationId);
        if (integration == null) {
            throw new BaseException(ResultCode.INTEGRATION_NOT_FOUND, "Integration not found: " + integrationId);
        }
        return List.copyOf(webhookRegistry.getOrDefault(integrationId, List.of()));
    }

    @Override
    public void deleteWebhook(Long webhookId) {
        boolean removed = false;
        for (List<Map<String, Object>> webhooks : webhookRegistry.values()) {
            removed = webhooks.removeIf(w -> webhookId.equals(w.get("id")));
            if (removed) {
                break;
            }
        }
        if (!removed) {
            throw new BaseException(ResultCode.NOT_FOUND, "Webhook not found: " + webhookId);
        }
        log.info("Webhook deleted: {}", webhookId);
    }

    @Override
    public Map<String, Object> aggregateHealthStatus() {
        List<SfIntegration> integrations = list();
        int total = integrations.size();
        int active = 0;
        int inactive = 0;
        int unknown = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (SfIntegration integration : integrations) {
            Integer status = integration.getStatus();
            String health = status != null && status == 1 ? "healthy" : (status != null ? "unhealthy" : "unknown");
            if ("healthy".equals(health)) {
                active++;
            } else if ("unhealthy".equals(health)) {
                inactive++;
            } else {
                unknown++;
            }

            details.add(Map.of(
                    "id", integration.getId(),
                    "name", integration.getName(),
                    "type", integration.getType(),
                    "status", health
            ));
        }

        return Map.of(
                "total", total,
                "active", active,
                "inactive", inactive,
                "unknown", unknown,
                "integrations", details
        );
    }
}
