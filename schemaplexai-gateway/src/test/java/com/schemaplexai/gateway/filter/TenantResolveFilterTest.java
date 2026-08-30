package com.schemaplexai.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.gateway.config.GatewayWhitelistProperties;
import com.schemaplexai.gateway.config.TenantValidationProperties;
import com.schemaplexai.gateway.tenant.ReactiveTenantValidator;
import com.schemaplexai.gateway.tenant.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the tenant resolution + existence validation behavior
 * (issue 913, spec §4.3).
 */
class TenantResolveFilterTest {

    private TenantResolveFilter filter;
    private ReactiveTenantValidator validator;
    private TenantValidationProperties validationProperties;
    private ServerWebExchange exchange;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private GatewayFilterChain chain;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        validator = mock(ReactiveTenantValidator.class);
        validationProperties = new TenantValidationProperties();
        filter = new TenantResolveFilter(new ObjectMapper(), validator,
                validationProperties, new GatewayWhitelistProperties());

        exchange = mock(ServerWebExchange.class);
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        chain = mock(GatewayFilterChain.class);
        attributes = new HashMap<>();

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(exchange.getAttributes()).thenReturn(attributes);
        when(response.getHeaders()).thenReturn(new HttpHeaders());
        when(response.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(response.writeWith(any())).thenReturn(Mono.empty());
        when(response.setComplete()).thenReturn(Mono.empty());
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        when(request.getURI()).thenReturn(java.net.URI.create("http://localhost/test"));

        stubMutationChain();
    }

    /** Stubs request/exchange mutation so passThrough can build the mutated exchange. */
    private void stubMutationChain() {
        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
        ServerHttpRequest mutatedRequest = mock(ServerHttpRequest.class);
        when(requestBuilder.build()).thenReturn(mutatedRequest);

        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
        ServerWebExchange mutatedExchange = mock(ServerWebExchange.class);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);
        when(chain.filter(mutatedExchange)).thenReturn(Mono.empty());
    }

    private HttpHeaders headersWithTenant(String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        if (tenantId != null) {
            headers.set(CommonConstants.HEADER_TENANT_ID, tenantId);
        }
        return headers;
    }

    @Test
    void filter_noTenantAnywhere_returnsBadRequest() {
        // spec §4.3: no tenant information -> 400 at the edge (issue 913).
        when(request.getHeaders()).thenReturn(headersWithTenant(null));
        when(request.getMethod()).thenReturn(org.springframework.http.HttpMethod.GET);
        when(exchange.getAttribute(CommonConstants.CONTEXT_TENANT_ID)).thenReturn(null);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.BAD_REQUEST);
        verify(chain, never()).filter(any());
        verifyNoInteractions(validator);
    }

    @Test
    void filter_activeTenant_passesThroughAndSetsContext() {
        when(request.getHeaders()).thenReturn(headersWithTenant("tenant-abc"));
        when(validator.validate("tenant-abc")).thenReturn(Mono.just(TenantStatus.ACTIVE));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(validator).validate("tenant-abc");
        verify(chain).filter(any(ServerWebExchange.class));
        verify(response, never()).setStatusCode(any());
        assertThat(attributes).containsEntry(CommonConstants.CONTEXT_TENANT_ID, "tenant-abc");
    }

    @Test
    void filter_unknownTenant_returnsUnauthorized() {
        when(request.getHeaders()).thenReturn(headersWithTenant("forged-tenant"));
        when(request.getMethod()).thenReturn(org.springframework.http.HttpMethod.GET);
        when(validator.validate("forged-tenant")).thenReturn(Mono.just(TenantStatus.NOT_FOUND));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_disabledTenant_returnsForbidden() {
        when(request.getHeaders()).thenReturn(headersWithTenant("tenant-disabled"));
        when(request.getMethod()).thenReturn(org.springframework.http.HttpMethod.GET);
        when(validator.validate("tenant-disabled")).thenReturn(Mono.just(TenantStatus.DISABLED));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_validationError_failsClosedWithServiceUnavailable() {
        when(request.getHeaders()).thenReturn(headersWithTenant("tenant-abc"));
        when(validator.validate("tenant-abc"))
                .thenReturn(Mono.error(new RuntimeException("validation channel down")));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_whitelistedPath_skipsValidationAndPassesThrough() {
        // /auth/login carries no tenant legitimately; must not be validated/rejected.
        when(request.getURI()).thenReturn(java.net.URI.create("http://localhost/auth/login"));
        when(request.getHeaders()).thenReturn(headersWithTenant(null));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(validator);
        verify(response, never()).setStatusCode(any());
    }

    @Test
    void filter_validationDisabled_passesThroughWithoutValidator() {
        validationProperties.setEnabled(false);
        when(request.getHeaders()).thenReturn(headersWithTenant("tenant-abc"));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any(ServerWebExchange.class));
        verifyNoInteractions(validator);
        assertThat(attributes).containsEntry(CommonConstants.CONTEXT_TENANT_ID, "tenant-abc");
    }

    @Test
    void filter_tenantFromExchangeAttribute_isValidatedToo() {
        // Fallback path: tenant resolved from the validated token by an earlier filter.
        when(request.getHeaders()).thenReturn(headersWithTenant(null));
        when(exchange.getAttribute(CommonConstants.CONTEXT_TENANT_ID)).thenReturn("tenant-from-attribute");
        when(validator.validate("tenant-from-attribute")).thenReturn(Mono.just(TenantStatus.ACTIVE));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(validator).validate("tenant-from-attribute");
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_overlyLongTenantId_isStillValidated() {
        String longTenantId = "a".repeat(200);
        when(request.getHeaders()).thenReturn(headersWithTenant(longTenantId));
        when(request.getMethod()).thenReturn(org.springframework.http.HttpMethod.GET);
        when(validator.validate(longTenantId)).thenReturn(Mono.just(TenantStatus.NOT_FOUND));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Format warnings do not bypass validation: an unknown long id is rejected.
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void getOrder_returnsNegative90() {
        assertThat(filter.getOrder()).isEqualTo(-90);
    }
}
