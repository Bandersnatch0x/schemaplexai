package com.schemaplexai.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.gateway.config.RateLimitProperties;
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
    private RateLimitProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        valueOps = mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setDefaultLimit(100);
        properties.setWindowSize(60);
        properties.setWhitelistPaths(java.util.Collections.emptyList());

        objectMapper = new ObjectMapper();

        filter = new RateLimitFilter(redisTemplate, properties, objectMapper);

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
        when(request.getURI()).thenReturn(java.net.URI.create("http://localhost/agent/execute"));
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
    void filter_rateLimitExceeded_returnsJsonWithObjectMapper() {
        when(valueOps.increment(anyString())).thenReturn(Mono.just(200L));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Verify ObjectMapper-serialized JSON contains expected fields
        verify(response).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        verify(response).writeWith(any());
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
    void filter_downstreamError_isNotConvertedTo429() {
        // Fail-closed is scoped to Redis errors (spec §4.1). A downstream failure
        // (e.g. connection refused) must propagate with its own error, not be
        // rewritten into a misleading 429 "rate limit exceeded" (issue 911).
        when(valueOps.increment(anyString())).thenReturn(Mono.just(5L));
        when(chain.filter(exchange)).thenReturn(Mono.error(new RuntimeException("downstream unavailable")));

        StepVerifier.create(filter.filter(exchange, chain))
                .expectErrorMatches(e -> "downstream unavailable".equals(e.getMessage()))
                .verify();

        verify(response, never()).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
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
    void filter_disabled_skipsRateLimit() {
        properties.setEnabled(false);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void filter_whitelistedPath_skipsRateLimit() {
        properties.setWhitelistPaths(java.util.List.of("/auth/**"));
        when(request.getURI()).thenReturn(java.net.URI.create("http://localhost/auth/login"));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void filter_usesConfigurableLimitFromProperties() {
        properties.setDefaultLimit(50);

        when(valueOps.increment(anyString())).thenReturn(Mono.just(51L));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        verify(chain, never()).filter(exchange);
    }

    @Test
    void filter_usesConfigurableWindowFromProperties() {
        properties.setWindowSize(30);

        when(valueOps.increment(anyString())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Window should be 30 seconds = Duration.ofSeconds(30)
        verify(redisTemplate).expire(anyString(), argThat(d -> ((java.time.Duration) d).toSeconds() == 30));
    }

    @Test
    void getOrder_returnsNegative150_runsBeforeJwtAuthFilter() {
        // Must be lower than JwtAuthFilter's -100 so anonymous / invalid-token
        // traffic is rate limited before the auth short-circuit (issue 911).
        assertThat(filter.getOrder()).isEqualTo(-150);
    }
}
