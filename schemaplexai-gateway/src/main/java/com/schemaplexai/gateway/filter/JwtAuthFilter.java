package com.schemaplexai.gateway.filter;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void validateJwtSecret() {
        if (!StringUtils.hasText(jwtSecret) || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long. Please set the JWT_SECRET environment variable.");
        }
    }

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final List<String> whiteList = List.of(
            "/auth/**",
            "/system/tenants/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/webjars/**",
            "/doc.html"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange.getResponse(), ResultCode.UNAUTHORIZED.getMessage());
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

            String userId = claims.getSubject();
            String tenantId = claims.get("tenantId", String.class);

            ServerHttpRequest.Builder builder = request.mutate()
                    .header(CommonConstants.HEADER_AUTHORIZATION, CommonConstants.TOKEN_PREFIX + token)
                    .header("X-User-Id", userId);

            if (StringUtils.hasText(tenantId)) {
                builder.header(CommonConstants.HEADER_TENANT_ID, tenantId);
            }

            ServerHttpRequest mutatedRequest = builder.build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired");
            return unauthorized(exchange.getResponse(), ResultCode.TOKEN_EXPIRED.getMessage());
        } catch (Exception e) {
            log.warn("JWT token invalid: {}", e.getMessage());
            return unauthorized(exchange.getResponse(), ResultCode.TOKEN_INVALID.getMessage());
        }
    }

    private boolean isWhiteListed(String path) {
        return whiteList.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String resolveToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(CommonConstants.HEADER_AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "code", 401,
                "message", message,
                "timestamp", System.currentTimeMillis()
        );
        try {
            byte[] bytes = new ObjectMapper().writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
