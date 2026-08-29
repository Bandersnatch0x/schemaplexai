package com.schemaplexai.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.gateway.config.RateLimitProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the gateway module.
 * <p>
 * Tests the full filter chain with mocked downstream Redis.
 * Uses a fresh context per test class to avoid mock state pollution.
 */
@SpringBootTest(
        classes = {GatewayApplication.class, GatewayIntegrationTest.TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration" +
                        ",org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration" +
                        ",org.springframework.cloud.gateway.config.GatewayRedisAutoConfiguration" +
                        ",org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
                "jwt.secret=a]B@cD3fG6hI9kL2mN5oP8rS1tU4vW7xY0zA3bC6dE9fG2hI5kL8mN1oP4rS7tU0vW",
                "rate-limit.enabled=true",
                "rate-limit.default-limit=100",
                "rate-limit.window-size=60",
                "management.otlp.tracing.enabled=false"
        }
)
class GatewayIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveValueOperations<String, String> valueOps;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    private static final String SECRET = "a]B@cD3fG6hI9kL2mN5oP8rS1tU4vW7xY0zA3bC6dE9fG2hI5kL8mN1oP4rS7tU0vW";
    private String validToken;

    @BeforeEach
    void setUp() {
        // Reset the increment mock for default behavior; do NOT reset redisTemplate
        // to preserve the opsForValue()→valueOps chain from @TestConfiguration.
        doReturn(Mono.just(1L)).when(valueOps).increment(anyString());

        // Generate a fresh valid token for each test
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        validToken = Jwts.builder()
                .subject("user-integration")
                .claim("tenantId", "tenant-integration")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();
    }

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void whitelistedPath_doesNotRequireToken() {
        // Whitelisted path — should not return 401 even without a token
        webTestClient.get()
                .uri("/auth/login")
                .exchange()
                .expectStatus().value(status -> assertThat(status)
                        .isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
    }

    @Test
    void protectedPath_withoutToken_returns401() {
        webTestClient.get()
                .uri("/agent/execute")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedPath_withInvalidToken_returns401() {
        webTestClient.get()
                .uri("/agent/execute")
                .header(CommonConstants.HEADER_AUTHORIZATION, CommonConstants.TOKEN_PREFIX + "bad.token.value")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedPath_withValidToken_passesThrough() {
        webTestClient.get()
                .uri("/agent/execute")
                .header(CommonConstants.HEADER_AUTHORIZATION, CommonConstants.TOKEN_PREFIX + validToken)
                .exchange()
                .expectStatus().value(status -> assertThat(status)
                        .isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
    }

    @Test
    void rateLimit_withExcessRequests_returns429() {
        // Simulate exceeding the rate limit (default=100)
        doReturn(Mono.just(200L)).when(valueOps).increment(anyString());

        webTestClient.get()
                .uri("/agent/execute")
                .header(CommonConstants.HEADER_AUTHORIZATION, CommonConstants.TOKEN_PREFIX + validToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void rateLimit_withinLimit_passesThrough() {
        // Within the limit: the counter returns 1 (<= default 100), so the request
        // must not be rejected by the limiter. (Downstream is not running in this
        // test, but a routing/connection failure must NOT surface as a 429 — the
        // fail-closed handler is scoped to Redis errors only, issue 911.)
        webTestClient.get()
                .uri("/auth/login")
                .exchange()
                .expectStatus().value(status -> assertThat(status)
                        .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS.value()));
    }

    @Test
    void anonymousFlood_isRateLimitedBeforeJwtAuth() {
        // No token on a protected path. Previously the request short-circuited to a
        // 401 in JwtAuthFilter and never reached the limiter. With the limiter moved
        // ahead of auth (issue 911), an over-limit anonymous request must get 429.
        doReturn(Mono.just(200L)).when(valueOps).increment(anyString());

        webTestClient.get()
                .uri("/agent/execute")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void invalidTokenFlood_isRateLimitedBeforeJwtAuth() {
        // An invalid token request must also be counted by the limiter (issue 911).
        // Over the limit it returns 429 from the limiter rather than 401 from auth.
        doReturn(Mono.just(200L)).when(valueOps).increment(anyString());

        webTestClient.get()
                .uri("/agent/execute")
                .header(CommonConstants.HEADER_AUTHORIZATION, CommonConstants.TOKEN_PREFIX + "bad.token.value")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    /**
     * Test configuration that provides mocked Redis components.
     */
    @TestConfiguration
    static class TestConfig {

        @SuppressWarnings("unchecked")
        @Bean
        @Primary
        ReactiveValueOperations<String, String> valueOps() {
            ReactiveValueOperations<String, String> ops = mock(ReactiveValueOperations.class);
            return ops;
        }

        @Bean
        @Primary
        ReactiveStringRedisTemplate redisTemplate(ReactiveValueOperations<String, String> valueOps) {
            ReactiveStringRedisTemplate tmpl = mock(ReactiveStringRedisTemplate.class);
            // lenient: opsForValue() must always return the injected mock
            org.mockito.Mockito.lenient().when(tmpl.opsForValue()).thenReturn(valueOps);
            org.mockito.Mockito.lenient().when(tmpl.expire(anyString(), any())).thenReturn(Mono.just(true));
            return tmpl;
        }

        @Bean
        @Primary
        RateLimitProperties rateLimitProperties() {
            RateLimitProperties props = new RateLimitProperties();
            props.setEnabled(true);
            props.setDefaultLimit(100);
            props.setWindowSize(60);
            // Mirrors production application.yml: no rate-limit exemptions (issue 911),
            // so /auth/** traffic (login) is throttled at the edge too.
            props.setWhitelistPaths(List.of());
            return props;
        }

        @Bean
        @Primary
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
