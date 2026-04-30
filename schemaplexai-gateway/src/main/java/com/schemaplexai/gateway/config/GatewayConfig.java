package com.schemaplexai.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("system-service", r -> r.path("/system/**", "/auth/**")
                        .uri("lb://schemaplexai-system"))
                .route("web-service", r -> r.path("/web/**", "/sse/**", "/ws/**")
                        .uri("lb://schemaplexai-web"))
                .route("agent-config-service", r -> r.path("/agent-config/**")
                        .uri("lb://schemaplexai-agent-config"))
                .route("agent-engine-service", r -> r.path("/agent/**")
                        .uri("lb://schemaplexai-agent-engine"))
                .route("workflow-service", r -> r.path("/workflow/**")
                        .uri("lb://schemaplexai-workflow"))
                .route("context-service", r -> r.path("/context/**")
                        .uri("lb://schemaplexai-context"))
                .route("quality-service", r -> r.path("/quality/**")
                        .uri("lb://schemaplexai-quality"))
                .route("integration-service", r -> r.path("/integration/**")
                        .uri("lb://schemaplexai-integration"))
                .route("task-service", r -> r.path("/task/**")
                        .uri("lb://schemaplexai-task"))
                .route("ops-service", r -> r.path("/ops/**")
                        .uri("lb://schemaplexai-ops"))
                .route("admin-service", r -> r.path("/admin/**")
                        .uri("lb://schemaplexai-admin"))
                .build();
    }
}
