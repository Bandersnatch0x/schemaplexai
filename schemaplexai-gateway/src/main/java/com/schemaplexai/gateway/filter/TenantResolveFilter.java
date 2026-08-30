package com.schemaplexai.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.gateway.config.GatewayWhitelistProperties;
import com.schemaplexai.gateway.config.TenantValidationProperties;
import com.schemaplexai.gateway.tenant.ReactiveTenantValidator;
import com.schemaplexai.gateway.tenant.TenantStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Tenant resolution + existence validation (spec §4.3, issue 913).
 *
 * <p>Runs after {@link JwtAuthFilter} (order -90 &gt; -100), so for authenticated
 * requests the {@code X-Tenant-Id} header already carries the tenant claim of the
 * validated token (JwtAuthFilter strips client-supplied values first).
 *
 * <p>Enforcement:
 * <ul>
 *   <li>whitelisted (unauthenticated) paths are exempt — they legitimately carry
 *       no tenant (e.g. {@code /auth/login});</li>
 *   <li>no tenant anywhere → <b>400 Bad Request</b> (spec §4.3 "缺少租户信息");</li>
 *   <li>unknown/forged tenant → <b>401 Unauthorized</b>;</li>
 *   <li>disabled tenant → <b>403 Forbidden</b>;</li>
 *   <li>validation infrastructure failure → <b>503</b> (fail-closed, consistent
 *       with the rate limiter's Redis stance).</li>
 * </ul>
 *
 * <p>Existence is checked through {@link ReactiveTenantValidator} (Caffeine L1 +
 * Redis L2), see {@link com.schemaplexai.gateway.tenant.CaffeineRedisTenantValidator}
 * for the channel design. Validation can be switched off with
 * {@code tenant.validation.enabled=false} for bootstrapping environments that do
 * not yet maintain the cache.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantResolveFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper;
    private final ReactiveTenantValidator tenantValidator;
    private final TenantValidationProperties validationProperties;
    private final GatewayWhitelistProperties whitelistProperties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Unauthenticated paths (login, doc UI, ...) legitimately carry no tenant.
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String tenantId = request.getHeaders().getFirst(CommonConstants.HEADER_TENANT_ID);
        if (!StringUtils.hasText(tenantId)) {
            // Fallback: tenant resolved from the validated token by an earlier filter.
            tenantId = exchange.getAttribute(CommonConstants.CONTEXT_TENANT_ID);
        }

        if (!StringUtils.hasText(tenantId)) {
            // spec §4.3: no tenant information anywhere -> reject at the edge
            log.debug("No tenant ID found for request: {} {}", request.getMethod(), path);
            return reject(exchange.getResponse(), HttpStatus.BAD_REQUEST, "tenant information is missing");
        }

        if (tenantId.length() > 128) {
            log.warn("Suspicious tenant ID format resolved: '{}' for {} {}", tenantId, request.getMethod(), path);
        }

        if (!validationProperties.isEnabled()) {
            return passThrough(exchange, chain, tenantId);
        }

        final String resolvedTenantId = tenantId;
        return tenantValidator.validate(resolvedTenantId)
                .onErrorMap(e -> {
                    // Fail-closed: if the validation channel itself fails, do not
                    // let unvalidated tenants through. Marked so downstream errors
                    // are not misreported as validation failures.
                    log.error("Tenant validation failed for tenant '{}' — denying request", resolvedTenantId, e);
                    return new ValidationChannelException(e);
                })
                .flatMap(status -> {
                    switch (status) {
                        case ACTIVE:
                            return passThrough(exchange, chain, resolvedTenantId);
                        case DISABLED:
                            log.warn("Rejected request for disabled tenant '{}' on {} {}",
                                    resolvedTenantId, request.getMethod(), path);
                            return reject(exchange.getResponse(), HttpStatus.FORBIDDEN,
                                    ResultCode.TENANT_DISABLED.getMessage());
                        case NOT_FOUND:
                        default:
                            log.warn("Rejected request for unknown tenant '{}' on {} {}",
                                    resolvedTenantId, request.getMethod(), path);
                            return reject(exchange.getResponse(), HttpStatus.UNAUTHORIZED,
                                    ResultCode.TENANT_NOT_FOUND.getMessage());
                    }
                })
                .onErrorResume(ValidationChannelException.class, e ->
                        reject(exchange.getResponse(), HttpStatus.SERVICE_UNAVAILABLE,
                                "tenant validation unavailable"))
                .onErrorResume(e -> {
                    log.error("Downstream service error for tenant '{}' on {} {}",
                            resolvedTenantId, request.getMethod(), path, e);
                    return reject(exchange.getResponse(), HttpStatus.SERVICE_UNAVAILABLE,
                            "downstream service unavailable");
                });
    }

    private static final class ValidationChannelException extends RuntimeException {
        ValidationChannelException(Throwable cause) {
            super(cause);
        }
    }

    private boolean isWhiteListed(String path) {
        return whitelistProperties.getPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> passThrough(ServerWebExchange exchange, GatewayFilterChain chain, String tenantId) {
        exchange.getAttributes().put(CommonConstants.CONTEXT_TENANT_ID, tenantId);
        // Single mutate/build (spec §4.2 constraint applies to downstream request objects)
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CommonConstants.HEADER_TENANT_ID, tenantId)
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private Mono<Void> reject(ServerHttpResponse response, HttpStatus status, String message) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "code", status.value(),
                "message", message,
                "timestamp", System.currentTimeMillis()
        );
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
