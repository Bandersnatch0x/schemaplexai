package com.schemaplexai.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.redis.TenantRedisKeyResolver;
import com.schemaplexai.gateway.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Skip when rate limiting is disabled
        if (!rateLimitProperties.isEnabled()) {
            return chain.filter(exchange);
        }

        // Skip whitelisted paths
        String path = exchange.getRequest().getURI().getPath();
        for (String pattern : rateLimitProperties.getWhitelistPaths()) {
            if (pathMatcher.match(pattern, path)) {
                return chain.filter(exchange);
            }
        }

        ServerHttpRequest request = exchange.getRequest();
        long windowSeconds = rateLimitProperties.getWindowSize();
        // Use window-aligned time slot: epoch seconds / windowSize
        String windowKey = String.valueOf(System.currentTimeMillis() / 1000 / windowSeconds);
        String key = resolveRateLimitKey(request, windowKey);

        return reactiveStringRedisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        return reactiveStringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds))
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .flatMap(count -> {
                    if (count > rateLimitProperties.getDefaultLimit()) {
                        return rateLimitExceeded(exchange.getResponse());
                    }
                    return chain.filter(exchange);
                })
                .onErrorResume(e -> {
                    log.error("Rate limit check failed, denying request", e);
                    return rateLimitExceeded(exchange.getResponse());
                });
    }

    /**
     * Resolve rate limit key with tenant prefix.
     * Tenant requests use: sf:{tenantId}:ratelimit:tenant:{windowKey}
     * IP fallback uses:    sf:global:ratelimit:ip:{ip}:{windowKey}
     */
    private String resolveRateLimitKey(ServerHttpRequest request, String windowKey) {
        String tenantId = request.getHeaders().getFirst(CommonConstants.HEADER_TENANT_ID);
        if (StringUtils.hasText(tenantId)) {
            return TenantRedisKeyResolver.rateLimit(tenantId, "tenant", windowKey);
        }
        String ip = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        return TenantRedisKeyResolver.rateLimitGlobal("ip", ip, windowKey);
    }

    private Mono<Void> rateLimitExceeded(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "code", 429,
                "message", "rate limit exceeded",
                "timestamp", System.currentTimeMillis()
        );
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Failed to serialize rate limit response", e);
            String fallback = "{\"code\":429,\"message\":\"rate limit exceeded\"}";
            DataBuffer buffer = response.bufferFactory().wrap(fallback.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
