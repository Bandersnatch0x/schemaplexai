package com.schemaplexai.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GatewayConfigTest {

    @Test
    void customRouteLocator_returnsNonNullRouteLocator() {
        RouteLocatorBuilder builder = mock(RouteLocatorBuilder.class);
        RouteLocatorBuilder.Builder routeBuilder = mock(RouteLocatorBuilder.Builder.class);
        RouteLocator routeLocator = mock(RouteLocator.class);

        when(builder.routes()).thenReturn(routeBuilder);
        when(routeBuilder.route(anyString(), any())).thenReturn(routeBuilder);
        when(routeBuilder.build()).thenReturn(routeLocator);

        GatewayConfig config = new GatewayConfig();
        RouteLocator result = config.customRouteLocator(builder);

        assertThat(result).isNotNull();
        verify(builder).routes();
        verify(routeBuilder, times(11)).route(anyString(), any());
        verify(routeBuilder).build();
    }
}
