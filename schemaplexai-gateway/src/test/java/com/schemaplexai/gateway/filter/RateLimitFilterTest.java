package com.schemaplexai.gateway.filter;

import com.schemaplexai.common.constants.CommonConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private ReactiveStringRedisTemplate redisTemplate;
    private ReactiveValueOperations<String, String> valueOps;
    private ServerWebExchange exchange;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        valueOps = mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        filter = new RateLimitFilter(redisTemplate);

        exchange = mock(ServerWebExchange.class);
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        chain = mock(GatewayFilterChain.class);

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(new HttpHeaders());
        when(response.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(response.writeWith(any())).thenReturn(Mono.empty());
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Default: no tenant header, remote address set
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
    }

    @Test
    void filter_withinRateLimit_passesThrough() {
        when(valueOps.increment(anyString())).thenReturn(Mono.just(5L));
        when(redisTemplate.expire(anyString(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_firstRequest_setsExpiration() {
        when(valueOps.increment(anyString())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(redisTemplate).expire(anyString(), any());
        verify(chain).filter(exchange);
    }

    @Test
    void filter_rateLimitExceeded_returns429() {
        when(valueOps.increment(anyString())).thenReturn(Mono.just(101L));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        verify(chain, never()).filter(exchange);
    }

    @Test
    void filter_exactlyAtLimit_passesThrough() {
        when(valueOps.increment(anyString())).thenReturn(Mono.just(100L));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_redisError_deniesRequest() {
        when(valueOps.increment(anyString())).thenReturn(Mono.error(new RuntimeException("Redis down")));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        verify(chain, never()).filter(exchange);
    }

    @Test
    void filter_withTenantHeader_usesTenantAsClientId() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(CommonConstants.HEADER_TENANT_ID, "tenant-xyz");
        when(request.getHeaders()).thenReturn(headers);
        when(valueOps.increment(contains("tenant-xyz"))).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(valueOps).increment(contains("tenant-xyz"));
    }

    @Test
    void filter_noRemoteAddress_usesUnknownIp() {
        when(request.getRemoteAddress()).thenReturn(null);
        when(valueOps.increment(contains("ip:unknown"))).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(valueOps).increment(contains("ip:unknown"));
    }

    @Test
    void getOrder_returnsNegative50() {
        assertThat(filter.getOrder()).isEqualTo(-50);
    }
}
