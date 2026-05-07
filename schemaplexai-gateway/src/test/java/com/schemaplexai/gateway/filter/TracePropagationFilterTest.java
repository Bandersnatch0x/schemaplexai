package com.schemaplexai.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TracePropagationFilterTest {

    private final TracePropagationFilter filter = new TracePropagationFilter();

    @Test
    void shouldPropagateExistingTraceparent() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ServerWebExchange passed = captureExchange(chain);
        String traceparent = passed.getRequest().getHeaders().getFirst("traceparent");
        assertThat(traceparent).isEqualTo("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");
    }

    @Test
    void shouldGenerateTraceparentWhenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ServerWebExchange passed = captureExchange(chain);
        String traceparent = passed.getRequest().getHeaders().getFirst("traceparent");
        assertThat(traceparent).isNotNull();
        assertThat(traceparent).startsWith("00-");
        assertThat(traceparent.split("-")).hasSize(4);
    }

    private ServerWebExchange captureExchange(GatewayFilterChain chain) {
        var captor = org.mockito.ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        return captor.getValue();
    }
}
