package com.schemaplexai.gateway.config;

import com.schemaplexai.gateway.GatewayApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationContext;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that every {@code lb://} route defined in {@link GatewayConfig}
 * resolves to at least one reachable service instance.
 * <p>
 * The instances come from the static SimpleDiscoveryClient configuration in
 * {@code application.yml} ({@code spring.cloud.discovery.client.simple.instances}).
 * Without such an instance source the LoadBalancer would see an empty instance
 * list and every routed request would fail with 503 (REQ-02 of the API gateway
 * spec compliance review).
 */
@SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jwt.secret=a]B@cD3fG6hI9kL2mN5oP8rS1tU4vW7xY0zA3bC6dE9fG2hI5kL8mN1oP4rS7tU0vW",
                "management.otlp.tracing.enabled=false"
        }
)
class RouteResolutionTest {

    /** Expected downstream port per service id, from spec §3 and each module's application.yml. */
    private static final Map<String, Integer> EXPECTED_PORTS = Map.ofEntries(
            Map.entry("schemaplexai-system", 8081),
            Map.entry("schemaplexai-web", 8082),
            Map.entry("schemaplexai-agent-config", 8083),
            Map.entry("schemaplexai-agent-engine", 8084),
            Map.entry("schemaplexai-context", 8085),
            Map.entry("schemaplexai-spec", 8086),
            Map.entry("schemaplexai-workflow", 8087),
            Map.entry("schemaplexai-integration", 8088),
            Map.entry("schemaplexai-ops", 8089),
            Map.entry("schemaplexai-quality", 8090),
            Map.entry("schemaplexai-task", 8091),
            Map.entry("schemaplexai-admin", 8092)
    );

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private ReactiveDiscoveryClient discoveryClient;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private LoadBalancerClientFactory loadBalancerClientFactory;

    @Test
    void loadBalancerClientFilterIsPresent() {
        // If the loadbalancer starter were missing from the classpath, Spring Cloud
        // Gateway would fall back to GatewayNoLoadBalancerClientAutoConfiguration,
        // whose NoLoadBalancerClientFilter rejects every lb:// route with 503.
        assertThat(applicationContext.getBeanNamesForType(ReactiveLoadBalancerClientFilter.class))
                .as("ReactiveLoadBalancerClientFilter must be wired so lb:// routes resolve")
                .isNotEmpty();
    }

    @Test
    void everyLoadBalancedRouteHasAtLeastOneResolvableInstance() {
        List<Route> routes = routeLocator.getRoutes()
                .collectList()
                .block(Duration.ofSeconds(10));

        assertThat(routes).isNotNull().isNotEmpty();

        for (Route route : routes) {
            URI uri = route.getUri();
            assertThat(uri.getScheme())
                    .as("route '%s' should use the lb:// scheme", route.getId())
                    .isEqualTo("lb");

            String serviceId = uri.getHost();
            assertThat(EXPECTED_PORTS)
                    .as("route '%s' targets service '%s' which must have a static instance configured",
                            route.getId(), serviceId)
                    .containsKey(serviceId);

            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId)
                    .collectList()
                    .block(Duration.ofSeconds(10));

            assertThat(instances)
                    .as("service '%s' (route '%s') must have at least one reachable instance",
                            serviceId, route.getId())
                    .isNotEmpty();
            assertThat(instances.get(0).getUri().getPort())
                    .as("instance port for service '%s' must match the spec port", serviceId)
                    .isEqualTo(EXPECTED_PORTS.get(serviceId));
        }
    }

    @Test
    void allSpecDownstreamServicesAreCoveredByRoutes() {
        List<Route> routes = routeLocator.getRoutes()
                .collectList()
                .block(Duration.ofSeconds(10));

        assertThat(routes).isNotNull();

        List<String> routedServiceIds = routes.stream()
                .map(route -> route.getUri().getHost())
                .distinct()
                .toList();

        assertThat(routedServiceIds).containsExactlyInAnyOrderElementsOf(EXPECTED_PORTS.keySet());
    }

    @Test
    void loadBalancerSelectsAnInstanceForEveryRoutedService() {
        // Exercise the exact runtime resolution path used by
        // ReactiveLoadBalancerClientFilter when a request matches an lb:// route.
        for (Map.Entry<String, Integer> entry : EXPECTED_PORTS.entrySet()) {
            String serviceId = entry.getKey();
            ReactiveLoadBalancer<ServiceInstance> lb = loadBalancerClientFactory.getInstance(serviceId);
            assertThat(lb)
                    .as("LoadBalancer client must exist for service '%s'", serviceId)
                    .isNotNull();

            Response<ServiceInstance> response = Mono.from(lb.choose())
                    .block(Duration.ofSeconds(10));

            assertThat(response)
                    .as("LoadBalancer choose() response for service '%s'", serviceId)
                    .isNotNull();
            assertThat(response.hasServer())
                    .as("LoadBalancer must select a reachable instance for service '%s'", serviceId)
                    .isTrue();
            assertThat(response.getServer().getPort())
                    .as("selected instance port for service '%s'", serviceId)
                    .isEqualTo(entry.getValue());
        }
    }
}
