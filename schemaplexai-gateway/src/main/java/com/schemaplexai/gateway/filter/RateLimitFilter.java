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

/**
 * Redis-backed rate limit filter (fixed-window counter, fail-closed).
 *
 * <h3>Filter chain order (custom gateway filters, lowest {@code getOrder()} first)</h3>
 * <pre>
 *   1. LoggingFilter           (Integer.MIN_VALUE)      — access log, runs first so even
 *                                                         rate-limited/rejected requests are logged
 *   2. TracePropagationFilter  (Integer.MIN_VALUE+100)  — W3C traceparent propagation
 *   3. RateLimitFilter         (-150)                   — this filter
 *   4. JwtAuthFilter           (-100)                   — JWT validation + identity injection
 *   5. TenantResolveFilter     (-90)                    — tenant resolution/validation
 *   6. Route to downstream service
 * </pre>
 *
 * <p>Rate limiting intentionally runs <b>before</b> {@link JwtAuthFilter} (spec §2:
 * "RateLimitFilter" is step 1, ahead of JWT). With this order:
 * <ul>
 *   <li>anonymous traffic (no token) is throttled at the edge instead of short-circuiting
 *       to a 401 in JwtAuthFilter before ever reaching the limiter;</li>
 *   <li>requests with invalid/expired tokens are also counted against the limit;</li>
 *   <li>no HMAC parsing cost is paid for traffic that is already over the limit.</li>
 * </ul>
 *
 * <p>Engineering consequence of the spec order: at this point the token has not been
 * validated yet, so the tenant-scoped key derives from the client-reported
 * {@code X-Tenant-Id} header (forgeable). Requests without the header fall back to an
 * IP-scoped key. JwtAuthFilter later strips/re-injects the header from the validated
 * token, and TenantResolveFilter validates the tenant, so forged values do not reach
 * downstream services — they can only influence the rate-limit bucket.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    /**
     * Sentinel returned when the Redis counter operation itself fails.
     * {@code increment} never returns a negative count, so -1 is unambiguous.
     */
    private static final long RATE_CHECK_FAILED = -1L;

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

        // Fail-closed (spec §4.1): the error handler is scoped to the Redis counter
        // operations ONLY. It must not wrap chain.filter(...), otherwise every
        // downstream failure (e.g. connection refused) would surface as a
        // misleading 429 "rate limit exceeded".
        return reactiveStringRedisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        return reactiveStringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds))
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .onErrorResume(e -> {
                    log.error("Rate limit check failed, denying request", e);
                    return Mono.just(RATE_CHECK_FAILED);
                })
                .flatMap(count -> {
                    if (count == RATE_CHECK_FAILED || count > rateLimitProperties.getDefaultLimit()) {
                        return rateLimitExceeded(exchange.getResponse());
                    }
                    return chain.filter(exchange);
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
        // Must run before JwtAuthFilter (-100) so anonymous / invalid-token
        // traffic is throttled at the edge (spec §2 filter order). See class Javadoc.
        return -150;
    }
}
