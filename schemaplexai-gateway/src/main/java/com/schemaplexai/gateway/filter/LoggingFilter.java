package com.schemaplexai.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        exchange.getAttributes().put("traceId", traceId);
        exchange.getAttributes().put("startTime", startTime);

        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String uri = request.getURI().getPath();
        String query = request.getURI().getQuery();
        String clientIp = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        log.info("[{}] {} {} {}?{} from {}", traceId, "REQUEST", method, uri, query, clientIp);

        return chain.filter(exchange).doFinally(signalType -> {
            ServerHttpResponse response = exchange.getResponse();
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
            log.info("[{}] {} {} {} {} {}ms", traceId, "RESPONSE", method, uri, statusCode, duration);
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
