package com.schemaplexai.agent.engine.tool.adapter.http;

import com.schemaplexai.agent.engine.config.SecurityPolicyLoader;
import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionException;
import com.schemaplexai.agent.engine.tool.ToolResult;
import com.schemaplexai.agent.engine.tool.adapter.ExecutionContext;
import com.schemaplexai.agent.engine.tool.adapter.ToolAdapter;
import com.schemaplexai.model.entity.config.TenantEnvironmentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * HTTP call tool adapter with SSRF protection.
 *
 * Security measures:
 * - Private IP blocklist (10.x, 172.16-31.x, 192.168.x, 127.x, 169.254.x)
 * - Tenant-level HTTP call permission check via {@link SecurityPolicyLoader}
 * - Redirect depth limit (max 3, re-checking target IP each time)
 * - Dangerous protocol blocking (file://, gopher://, etc.)
 * - Connection/read timeouts (5s / 30s)
 *
 * TODO: Add per-tenant URL allowlist filtering before the SSRF private-IP check.
 *       The allowlist should be loaded from TenantEnvironmentConfig.extraConfig
 *       and validated against the requested URL host before DNS resolution.
 */
@Slf4j
@Component
public class HttpCallAdapter implements ToolAdapter {

    private static final Set<String> BLOCKED_SCHEMES = Set.of("file", "gopher", "ftp", "jar");
    private static final Set<String> ALLOWED_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
    private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH");
    private static final int MAX_REDIRECTS = 3;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final SecurityPolicyLoader securityPolicyLoader;

    public HttpCallAdapter(SecurityPolicyLoader securityPolicyLoader) {
        this.securityPolicyLoader = securityPolicyLoader;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public String getToolName() {
        return "http_call";
    }

    @Override
    public ToolResult execute(ToolCall call, ExecutionContext ctx) throws ToolExecutionException {
        String urlStr = call.parameters().getOrDefault("url", "").toString();
        if (urlStr.isBlank()) {
            throw new ToolExecutionException(ToolErrorCategory.INVALID_ARGUMENT, "URL is required");
        }

        String method = call.parameters().getOrDefault("method", "GET").toString().toUpperCase();

        // Validate HTTP method against whitelist (prevent TRACE, CONNECT, etc.)
        if (!ALLOWED_METHODS.contains(method)) {
            throw new ToolExecutionException(ToolErrorCategory.INVALID_ARGUMENT,
                    "HTTP method not allowed: " + method + ". Allowed: " + ALLOWED_METHODS);
        }

        // Tenant-level permission check
        if (ctx != null && ctx.tenantId() != null) {
            TenantEnvironmentConfig config = securityPolicyLoader.load(ctx.tenantId());
            if (config.getAllowHttpCalls() == null || Boolean.FALSE.equals(config.getAllowHttpCalls())) {
                log.warn("HTTP calls blocked for tenant {}: not allowed by security policy", ctx.tenantId());
                throw new ToolExecutionException(ToolErrorCategory.ENVIRONMENT_MISMATCH,
                        "HTTP calls are not enabled for this tenant");
            }
        }

        try {
            return executeWithRedirectProtection(urlStr, method, call.parameters(), 0);
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("HttpCallAdapter error for URL {}: {}", urlStr, e.getMessage());
            throw new ToolExecutionException(ToolErrorCategory.INTERNAL_ERROR,
                    "HTTP call failed: " + e.getMessage(), e);
        }
    }

    private ToolResult executeWithRedirectProtection(String urlStr, String method,
                                                      Map<String, Object> parameters, int redirectCount)
            throws ToolExecutionException {
        if (redirectCount > MAX_REDIRECTS) {
            throw new ToolExecutionException(ToolErrorCategory.ENVIRONMENT_MISMATCH,
                    "Too many redirects (max " + MAX_REDIRECTS + ")");
        }

        URI uri;
        try {
            uri = URI.create(urlStr);
        } catch (IllegalArgumentException e) {
            throw new ToolExecutionException(ToolErrorCategory.INVALID_ARGUMENT,
                    "Invalid URL: " + urlStr);
        }

        // Block dangerous schemes
        String scheme = uri.getScheme();
        if (scheme == null || BLOCKED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new ToolExecutionException(ToolErrorCategory.ENVIRONMENT_MISMATCH,
                    "Blocked URL scheme: " + scheme);
        }
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new ToolExecutionException(ToolErrorCategory.ENVIRONMENT_MISMATCH,
                    "Only HTTP/HTTPS schemes allowed, got: " + scheme);
        }

        // SSRF check: resolve host and verify IP is not private
        String host = uri.getHost();
        if (host == null) {
            throw new ToolExecutionException(ToolErrorCategory.INVALID_ARGUMENT, "URL has no host");
        }

        InetAddress resolvedAddress;
        try {
            resolvedAddress = InetAddress.getByName(host);
            if (SsrfProtectionUtil.isPrivateAddress(resolvedAddress)) {
                log.warn("SSRF blocked: private IP {} for URL {}", resolvedAddress.getHostAddress(), urlStr);
                throw new ToolExecutionException(ToolErrorCategory.ENVIRONMENT_MISMATCH,
                        "HTTP call blocked: private/internal IP addresses not allowed");
            }
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolExecutionException(ToolErrorCategory.INTERNAL_ERROR,
                    "DNS resolution failed for host: " + host, e);
        }

        // DNS rebinding guard: resolve a second time and verify IP did not change
        try {
            InetAddress secondResolution = InetAddress.getByName(host);
            if (!resolvedAddress.getHostAddress().equals(secondResolution.getHostAddress())) {
                log.warn("DNS rebinding detected: {} resolved to {} then {}",
                        host, resolvedAddress.getHostAddress(), secondResolution.getHostAddress());
                throw new ToolExecutionException(ToolErrorCategory.ENVIRONMENT_MISMATCH,
                        "HTTP call blocked: DNS rebinding detected");
            }
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolExecutionException(ToolErrorCategory.INTERNAL_ERROR,
                    "DNS re-resolution failed for host: " + host, e);
        }

        // Execute HTTP request
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(READ_TIMEOUT);

            switch (method) {
                case "GET" -> requestBuilder.GET();
                case "DELETE" -> requestBuilder.DELETE();
                default -> {
                    HttpRequest.BodyPublisher bodyPublisher = buildBodyPublisher(method, parameters);
                    requestBuilder.method(method, bodyPublisher);
                }
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            // Handle redirects manually (to re-check target IP)
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                String location = response.headers().firstValue("Location").orElse(null);
                if (location != null) {
                    // Resolve relative redirects against the original URI
                    URI redirectUri;
                    try {
                        redirectUri = URI.create(location).isAbsolute()
                                ? URI.create(location)
                                : uri.resolve(location);
                    } catch (IllegalArgumentException e) {
                        throw new ToolExecutionException(ToolErrorCategory.INVALID_ARGUMENT,
                                "Invalid redirect URL: " + location);
                    }
                    log.info("Following redirect ({} of {}): {} -> {}", redirectCount + 1, MAX_REDIRECTS, urlStr, redirectUri);
                    return executeWithRedirectProtection(redirectUri.toString(), "GET", parameters, redirectCount + 1);
                }
            }

            log.info("HttpCallAdapter: {} {} -> {} ({} bytes)", method, urlStr, status,
                    response.body() != null ? response.body().length() : 0);
            return ToolResult.success(response.body() != null ? response.body() : "");

        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolExecutionException(ToolErrorCategory.INTERNAL_ERROR,
                    "HTTP request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build an HTTP body publisher from tool parameters.
     *
     * <p>For POST/PUT/PATCH, extracts the {@code body} parameter from the tool call.
     * If no body is provided, sends an empty body.</p>
     *
     * @param method     the HTTP method
     * @param parameters the tool call parameters
     * @return a body publisher for the request
     */
    private static HttpRequest.BodyPublisher buildBodyPublisher(String method, Map<String, Object> parameters) {
        if (!BODY_METHODS.contains(method)) {
            return HttpRequest.BodyPublishers.noBody();
        }
        Object body = parameters.get("body");
        if (body == null) {
            return HttpRequest.BodyPublishers.noBody();
        }
        return HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8);
    }
}
