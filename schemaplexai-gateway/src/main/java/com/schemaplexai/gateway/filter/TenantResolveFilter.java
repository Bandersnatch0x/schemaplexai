package com.schemaplexai.gateway.filter;

import com.schemaplexai.common.constants.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class TenantResolveFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String tenantId = request.getHeaders().getFirst(CommonConstants.HEADER_TENANT_ID);

        if (!StringUtils.hasText(tenantId)) {
            String tokenTenantId = exchange.getAttribute("tenantId");
            if (StringUtils.hasText(tokenTenantId)) {
                tenantId = tokenTenantId;
            }
        }

        if (StringUtils.hasText(tenantId)) {
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(CommonConstants.HEADER_TENANT_ID, tenantId)
                    .build();
            exchange.getAttributes().put(CommonConstants.CONTEXT_TENANT_ID, tenantId);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
