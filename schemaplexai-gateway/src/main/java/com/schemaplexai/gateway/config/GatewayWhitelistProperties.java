package com.schemaplexai.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Paths that bypass JWT authentication ({@code JwtAuthFilter}) and tenant
 * validation ({@code TenantResolveFilter}) — the gateway's unauthenticated
 * surface (issue 912/913).
 * <p>
 * The field default mirrors the converged spec whitelist so programmatic
 * construction (unit tests) behaves identically to the packaged configuration.
 * Override via {@code gateway.whitelist.paths} in application.yml if needed.
 * <p>
 * Security note: entries here are reachable without a token AND without tenant
 * validation — keep this list minimal.
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.whitelist")
public class GatewayWhitelistProperties {

    private List<String> paths = new ArrayList<>(List.of(
            "/auth/login",
            "/auth/refresh",
            "/doc.html",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/webjars/**"
    ));
}
