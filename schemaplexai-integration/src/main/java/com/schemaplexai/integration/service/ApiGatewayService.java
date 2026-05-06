package com.schemaplexai.integration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.integration.entity.SfApiGatewayConfig;

import java.util.List;
import java.util.Map;

public interface ApiGatewayService extends IService<SfApiGatewayConfig> {

    /**
     * Add or update a route for the given gateway configuration.
     */
    void upsertRoute(Long gatewayId, String routeId, String path, String targetUrl, Integer priority);

    /**
     * List all routes for the given gateway configuration.
     */
    List<Map<String, Object>> listRoutes(Long gatewayId);

    /**
     * Delete a route by its route ID.
     */
    void deleteRoute(Long gatewayId, String routeId);

    /**
     * Update rate limit configuration for the gateway.
     */
    void updateRateLimit(Long gatewayId, Integer requestsPerSecond, Integer burstCapacity);

    /**
     * Perform health check on the gateway base URL.
     */
    boolean healthCheck(Long gatewayId);
}
