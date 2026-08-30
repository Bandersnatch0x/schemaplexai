package com.schemaplexai.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway route definitions (Java DSL, {@code lb://} scheme).
 * <p>
 * Instance resolution: every {@code lb://<service-id>} URI is resolved by Spring
 * Cloud LoadBalancer against the static instance source configured in
 * {@code application.yml} under
 * {@code spring.cloud.discovery.client.simple.instances} (SimpleDiscoveryClient).
 * This repository ships no registry (Eureka/Nacos/Consul/K8s), so the static
 * list is what makes the routes resolvable; without it every request would 503.
 * <p>
 * Note on {@code /admin/**}: the 2026-08-29 compliance report flagged this route
 * as pointing to a non-existent service, but {@code schemaplexai-admin} has since
 * been implemented as a real service (port 8092), so the route is kept and its
 * instance is registered alongside the others.
 */
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
                .route("agent-engine-service", r -> r.path("/agent/**", "/agents/**", "/agent-engine/**")
                        .uri("lb://schemaplexai-agent-engine"))
                .route("workflow-service", r -> r.path("/workflow/**")
                        .uri("lb://schemaplexai-workflow"))
                .route("context-service", r -> r.path("/context/**")
                        .uri("lb://schemaplexai-context"))
                .route("spec-service", r -> r.path("/spec/**")
                        .uri("lb://schemaplexai-spec"))
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
