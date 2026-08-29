package com.schemaplexai.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Documents and pins the custom gateway filter chain order (spec §2 requires
 * RateLimit before JWT; issue 911).
 *
 * <pre>
 *   LoggingFilter           (Integer.MIN_VALUE)      — access log first (audit all requests)
 *   TracePropagationFilter  (Integer.MIN_VALUE+100)  — W3C traceparent
 *   RateLimitFilter         (-150)                   — rate limit BEFORE the auth short-circuit
 *   JwtAuthFilter           (-100)                   — JWT validation + identity injection
 *   TenantResolveFilter     (-90)                    — tenant resolution/validation
 * </pre>
 *
 * Lower {@code getOrder()} values execute first (Spring Cloud Gateway
 * GlobalFilter/Ordered semantics).
 */
class FilterChainOrderTest {

    @Test
    void rateLimitRunsBeforeJwtAuthShortCircuit() {
        RateLimitFilter rateLimitFilter = new RateLimitFilter(null, null, new ObjectMapper());
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(new ObjectMapper());

        assertThat(rateLimitFilter.getOrder())
                .as("RateLimitFilter must run before JwtAuthFilter so anonymous and "
                        + "invalid-token requests are throttled at the edge")
                .isLessThan(jwtAuthFilter.getOrder());
    }

    @Test
    void fullCustomFilterChainOrderMatchesDocumentedTable() {
        LoggingFilter loggingFilter = new LoggingFilter();
        TracePropagationFilter tracePropagationFilter = new TracePropagationFilter();
        RateLimitFilter rateLimitFilter = new RateLimitFilter(null, null, new ObjectMapper());
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(new ObjectMapper());
        TenantResolveFilter tenantResolveFilter = new TenantResolveFilter();

        assertThat(loggingFilter.getOrder()).isEqualTo(Integer.MIN_VALUE);
        assertThat(tracePropagationFilter.getOrder()).isEqualTo(Integer.MIN_VALUE + 100);
        assertThat(rateLimitFilter.getOrder()).isEqualTo(-150);
        assertThat(jwtAuthFilter.getOrder()).isEqualTo(-100);
        assertThat(tenantResolveFilter.getOrder()).isEqualTo(-90);

        // Execution order (lower order value runs first)
        assertThat(loggingFilter.getOrder())
                .isLessThan(tracePropagationFilter.getOrder());
        assertThat(tracePropagationFilter.getOrder())
                .isLessThan(rateLimitFilter.getOrder());
        assertThat(rateLimitFilter.getOrder())
                .isLessThan(jwtAuthFilter.getOrder());
        assertThat(jwtAuthFilter.getOrder())
                .isLessThan(tenantResolveFilter.getOrder());
    }
}
