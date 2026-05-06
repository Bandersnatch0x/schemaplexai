package com.schemaplexai.gateway.filter;

import com.schemaplexai.common.constants.CommonConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TenantResolveFilterTest {

    private TenantResolveFilter filter;
    private ServerWebExchange exchange;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new TenantResolveFilter();
        exchange = mock(ServerWebExchange.class);
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        chain = mock(GatewayFilterChain.class);

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void filter_tenantIdFromHeader_setsHeaderAndPassesThrough() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(CommonConstants.HEADER_TENANT_ID, "tenant-abc");
        when(request.getHeaders()).thenReturn(headers);

        // Mock the mutated request builder
        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
        ServerHttpRequest mutatedRequest = mock(ServerHttpRequest.class);
        when(requestBuilder.build()).thenReturn(mutatedRequest);

        // Mock exchange mutate chain
        Map<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);

        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
        ServerWebExchange mutatedExchange = mock(ServerWebExchange.class);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);
        when(chain.filter(mutatedExchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(mutatedExchange);
    }

    @Test
    void filter_noTenantIdInHeader_usesExchangeAttribute() {
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        when(exchange.getAttribute("tenantId")).thenReturn("tenant-from-attribute");

        // Mock the mutated request builder
        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
        ServerHttpRequest mutatedRequest = mock(ServerHttpRequest.class);
        when(requestBuilder.build()).thenReturn(mutatedRequest);

        // Mock exchange mutate chain
        Map<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);

        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
        ServerWebExchange mutatedExchange = mock(ServerWebExchange.class);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);
        when(chain.filter(mutatedExchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(mutatedExchange);
    }

    @Test
    void filter_noTenantIdAnywhere_passesThroughWithoutMutation() {
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(exchange.getAttribute("tenantId")).thenReturn(null);

        Map<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Should pass the original exchange (no mutation)
        verify(chain).filter(exchange);
        verify(request, never()).mutate();
    }

    @Test
    void filter_emptyTenantIdHeader_usesExchangeAttribute() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(CommonConstants.HEADER_TENANT_ID, "");
        when(request.getHeaders()).thenReturn(headers);

        when(exchange.getAttribute("tenantId")).thenReturn("fallback-tenant");

        // Mock the mutated request builder
        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
        ServerHttpRequest mutatedRequest = mock(ServerHttpRequest.class);
        when(requestBuilder.build()).thenReturn(mutatedRequest);

        // Mock exchange mutate chain
        Map<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);

        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
        ServerWebExchange mutatedExchange = mock(ServerWebExchange.class);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);
        when(chain.filter(mutatedExchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(mutatedExchange);
    }

    @Test
    void getOrder_returnsNegative90() {
        assertThat(filter.getOrder()).isEqualTo(-90);
    }
}
