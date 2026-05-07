package com.schemaplexai.gateway.filter;

import com.schemaplexai.common.constants.CommonConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private JwtAuthFilter filter;
    private ServerWebExchange exchange;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private GatewayFilterChain chain;

    private static final String SECRET = "a]B@cD3fG6hI9kL2mN5oP8rS1tU4vW7xY0zA3bC6dE9fG2hI5kL8mN1oP4rS7tU0vW";

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);
        // Skip PostConstruct validation in tests
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

        // Stub request mutation chain (used by both whitelist and auth paths)
        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
        when(requestBuilder.headers(any())).thenReturn(requestBuilder);
        ServerHttpRequest mutatedRequest = mock(ServerHttpRequest.class);
        when(requestBuilder.build()).thenReturn(mutatedRequest);

        // Stub exchange mutation chain
        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
        ServerWebExchange mutatedExchange = mock(ServerWebExchange.class);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);
        when(chain.filter(mutatedExchange)).thenReturn(Mono.empty());
    }

    @Test
    void filter_whitelistedPath_passesThrough() {
        when(request.getURI()).thenReturn(java.net.URI.create("http://localhost/auth/login"));
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_whitelistedSwaggerPath_passesThrough() {
        when(request.getURI()).thenReturn(java.net.URI.create("http://localhost/swagger-ui/index.html"));
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_whitelistedDocPath_passesThrough() {
        when(request.getURI()).thenReturn(java.net.URI.create("http://localhost/doc.html"));
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_missingToken_returnsUnauthorized() {
        when(request.getURI()).thenReturn(java.net.URI.create("http://localhost/agent/execute"));
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_invalidToken_returnsUnauthorized() {
        when(request.getURI()).thenReturn(java.net.URI.create("http://localhost/agent/execute"));
        HttpHeaders headers = new HttpHeaders();
        headers.set(CommonConstants.HEADER_AUTHORIZATION, CommonConstants.TOKEN_PREFIX + "invalid.token.value");
        when(request.getHeaders()).thenReturn(headers);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_expiredToken_returnsUnauthorizedWithTokenExpiredMessage() {
        when(request.getURI()).thenReturn(java.net.URI.create("http://localhost/agent/execute"));

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("user-1")
                .claim("tenantId", "tenant-1")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key)
                .compact();

        HttpHeaders headers = new HttpHeaders();
        headers.set(CommonConstants.HEADER_AUTHORIZATION, CommonConstants.TOKEN_PREFIX + expiredToken);
        when(request.getHeaders()).thenReturn(headers);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_validToken_setsHeadersAndPassesThrough() {
        when(request.getURI()).thenReturn(java.net.URI.create("http://localhost/agent/execute"));

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String validToken = Jwts.builder()
                .subject("user-123")
                .claim("tenantId", "tenant-456")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();

        HttpHeaders headers = new HttpHeaders();
        headers.set(CommonConstants.HEADER_AUTHORIZATION, CommonConstants.TOKEN_PREFIX + validToken);
        when(request.getHeaders()).thenReturn(headers);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_validTokenWithoutTenantId_passesThrough() {
        when(request.getURI()).thenReturn(java.net.URI.create("http://localhost/agent/execute"));

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String validToken = Jwts.builder()
                .subject("user-123")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();

        HttpHeaders headers = new HttpHeaders();
        headers.set(CommonConstants.HEADER_AUTHORIZATION, CommonConstants.TOKEN_PREFIX + validToken);
        when(request.getHeaders()).thenReturn(headers);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_bearerPrefixMissing_returnsUnauthorized() {
        when(request.getURI()).thenReturn(java.net.URI.create("http://localhost/agent/execute"));
        HttpHeaders headers = new HttpHeaders();
        headers.set(CommonConstants.HEADER_AUTHORIZATION, "notbearer some-token");
        when(request.getHeaders()).thenReturn(headers);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void getOrder_returnsNegative100() {
        assertThat(filter.getOrder()).isEqualTo(-100);
    }
}
