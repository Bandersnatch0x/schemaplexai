package com.schemaplexai.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TracePropagationFilter implements GlobalFilter, Ordered {

    private static final String TRACEPARENT = "traceparent";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceparent = request.getHeaders().getFirst(TRACEPARENT);

        if (traceparent == null || traceparent.isBlank()) {
            traceparent = generateTraceparent();
        }

        ServerHttpRequest mutated = request.mutate()
                .header(TRACEPARENT, traceparent)
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private String generateTraceparent() {
        String traceId = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        traceId = traceId.substring(0, 32);
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return "00-" + traceId + "-" + spanId + "-01";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
