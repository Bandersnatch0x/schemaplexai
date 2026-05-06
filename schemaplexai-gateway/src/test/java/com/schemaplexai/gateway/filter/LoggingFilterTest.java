package com.schemaplexai.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoggingFilterTest {

    private LoggingFilter filter;
    private ServerWebExchange exchange;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new LoggingFilter();
        exchange = mock(ServerWebExchange.class);
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        chain = mock(GatewayFilterChain.class);

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(exchange.getAttributes()).thenReturn(new java.util.HashMap<>());
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost/api/test?foo=bar"));
        when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 9090));
        when(response.getStatusCode()).thenReturn(null);
    }

    @Test
    void filter_passesThroughToChain() {
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_storesTraceIdInAttributes() {
        java.util.Map<String, Object> attributes = new java.util.HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(attributes).containsKey("traceId");
        assertThat(attributes.get("traceId").toString()).hasSize(16);
    }

    @Test
    void filter_storesStartTimeInAttributes() {
        java.util.Map<String, Object> attributes = new java.util.HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(attributes).containsKey("startTime");
        assertThat((Long) attributes.get("startTime")).isPositive();
    }

    @Test
    void filter_handlesNullMethod_gracefully() {
        when(request.getMethod()).thenReturn(null);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_handlesNullRemoteAddress_gracefully() {
        when(request.getRemoteAddress()).thenReturn(null);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void getOrder_returnsHighestPrecedence() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
