package com.schemaplexai.integration.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfApiGatewayConfig;
import com.schemaplexai.integration.mapper.ApiGatewayConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class ApiGatewayServiceImpl extends ServiceImpl<ApiGatewayConfigMapper, SfApiGatewayConfig> implements ApiGatewayService {

    private final RestTemplate restTemplate;

    // In-memory route store: gatewayId -> list of routes
    private final Map<Long, List<Map<String, Object>>> routeStore = new ConcurrentHashMap<>();

    @Override
    public boolean save(SfApiGatewayConfig entity) {
        validateGateway(entity);
        return super.save(entity);
    }

    @Override
    public boolean updateById(SfApiGatewayConfig entity) {
        validateGateway(entity);
        return super.updateById(entity);
    }

    private void validateGateway(SfApiGatewayConfig entity) {
        if (entity == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Gateway config is required");
        }
        if (entity.getName() == null || entity.getName().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Gateway name is required");
        }
        if (entity.getBaseUrl() == null || entity.getBaseUrl().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Gateway base URL is required");
        }
        if (entity.getRateLimit() != null && entity.getRateLimit() < 1) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Rate limit must be at least 1");
        }
    }

    @Override
    public void upsertRoute(Long gatewayId, String routeId, String path, String targetUrl, Integer priority) {
        SfApiGatewayConfig gateway = getById(gatewayId);
        if (gateway == null) {
            throw new BaseException(ResultCode.INTEGRATION_NOT_FOUND, "Gateway not found: " + gatewayId);
        }
        if (routeId == null || routeId.isBlank() || path == null || path.isBlank() || targetUrl == null || targetUrl.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Route ID, path, and target URL are required");
        }

        List<Map<String, Object>> routes = routeStore.computeIfAbsent(gatewayId, k -> new ArrayList<>());
        routes.removeIf(r -> routeId.equals(r.get("routeId")));

        Map<String, Object> route = new ConcurrentHashMap<>();
        route.put("routeId", routeId);
        route.put("path", path);
        route.put("targetUrl", targetUrl);
        route.put("priority", priority != null ? priority : 0);
        route.put("updatedAt", Instant.now().toString());
        routes.add(route);

        log.info("Route upserted for gateway {}: routeId={}, path={}", gatewayId, routeId, path);
    }

    @Override
    public List<Map<String, Object>> listRoutes(Long gatewayId) {
        SfApiGatewayConfig gateway = getById(gatewayId);
        if (gateway == null) {
            throw new BaseException(ResultCode.INTEGRATION_NOT_FOUND, "Gateway not found: " + gatewayId);
        }
        return List.copyOf(routeStore.getOrDefault(gatewayId, List.of()));
    }

    @Override
    public void deleteRoute(Long gatewayId, String routeId) {
        SfApiGatewayConfig gateway = getById(gatewayId);
        if (gateway == null) {
            throw new BaseException(ResultCode.INTEGRATION_NOT_FOUND, "Gateway not found: " + gatewayId);
        }
        List<Map<String, Object>> routes = routeStore.get(gatewayId);
        if (routes == null || !routes.removeIf(r -> routeId.equals(r.get("routeId")))) {
            throw new BaseException(ResultCode.NOT_FOUND, "Route not found: " + routeId);
        }
        log.info("Route deleted for gateway {}: routeId={}", gatewayId, routeId);
    }

    @Override
    public void updateRateLimit(Long gatewayId, Integer requestsPerSecond, Integer burstCapacity) {
        SfApiGatewayConfig gateway = getById(gatewayId);
        if (gateway == null) {
            throw new BaseException(ResultCode.INTEGRATION_NOT_FOUND, "Gateway not found: " + gatewayId);
        }
        if (requestsPerSecond == null || requestsPerSecond < 1) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Requests per second must be at least 1");
        }
        gateway.setRateLimit(requestsPerSecond);
        updateById(gateway);
        log.info("Rate limit updated for gateway {}: rps={}, burst={}", gatewayId, requestsPerSecond, burstCapacity);
    }

    @Override
    public boolean healthCheck(Long gatewayId) {
        SfApiGatewayConfig gateway = getById(gatewayId);
        if (gateway == null || gateway.getBaseUrl() == null || gateway.getBaseUrl().isBlank()) {
            return false;
        }
        try {
            restTemplate.getForObject(gateway.getBaseUrl() + "/actuator/health", String.class);
            log.info("Gateway {} health check passed", gatewayId);
            return true;
        } catch (ResourceAccessException e) {
            log.warn("Gateway {} health check failed: connection refused", gatewayId);
            return false;
        } catch (Exception e) {
            log.warn("Gateway {} health check failed: {}", gatewayId, e.getMessage());
            return false;
        }
    }
}
