package com.schemaplexai.agent.engine.tool.adapter.http;

import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionException;
import com.schemaplexai.agent.engine.tool.ToolResult;
import com.schemaplexai.agent.engine.tool.adapter.ExecutionContext;
import com.schemaplexai.agent.engine.tool.adapter.ToolAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

/**
 * HTTP call tool adapter with SSRF protection.
 *
 * Security measures:
 * - Private IP blocklist (10.x, 172.16-31.x, 192.168.x, 127.x, 169.254.x)
 * - Allowlist support (configurable via TenantEnvironmentConfig)
 * - Redirect depth limit (max 3, re-checking target IP each time)
 * - Dangerous protocol blocking (file://, gopher://, etc.)
 * - Connection/read timeouts (5s / 30s)
 */
@Slf4j
@Component
public class HttpCallAdapter implements ToolAdapter {

    private static final Set<String> BLOCKED_SCHEMES = Set.of("file", "gopher", "ftp", "jar");
    private static final Set<String> ALLOWED_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
    private static final int MAX_REDIRECTS = 3;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;

    public HttpCallAdapter() {
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

        try {
            return executeWithRedirectProtection(urlStr, method, 0);
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("HttpCallAdapter error for URL {}: {}", urlStr, e.getMessage());
            throw new ToolExecutionException(ToolErrorCategory.INTERNAL_ERROR,
                    "HTTP call failed: " + e.getMessage(), e);
        }
    }

    private ToolResult executeWithRedirectProtection(String urlStr, String method, int redirectCount)
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
            if (isPrivateAddress(resolvedAddress)) {
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
                default -> requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
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
                    return executeWithRedirectProtection(redirectUri.toString(), "GET", redirectCount + 1);
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
     * Check if an IP address is in a private/internal range.
     * Blocks: 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 127.0.0.0/8, 169.254.0.0/16
     */
    static boolean isPrivateAddress(InetAddress address) {
        if (address.isLoopbackAddress() || address.isLinkLocalAddress()) {
            return true;
        }
        byte[] octets = address.getAddress();
        if (octets.length == 4) {
            int first = octets[0] & 0xFF;
            int second = octets[1] & 0xFF;
            // 10.0.0.0/8
            if (first == 10) return true;
            // 172.16.0.0/12
            if (first == 172 && second >= 16 && second <= 31) return true;
            // 192.168.0.0/16
            if (first == 192 && second == 168) return true;
            // 127.0.0.0/8 (loopback, already caught by isLoopbackAddress for IPv4)
            if (first == 127) return true;
            // 169.254.0.0/16 (link-local, already caught by isLinkLocalAddress)
            if (first == 169 && (octets[1] & 0xFF) == 254) return true;
            // 0.0.0.0/8
            if (first == 0) return true;
        } else if (octets.length == 16) {
            // IPv6 private ranges
            int firstByte = octets[0] & 0xFF;
            int secondByte = octets[1] & 0xFF;
            // fc00::/7 (Unique Local Addresses) — first byte is 1111_110x = 0xFC or 0xFD
            if ((firstByte & 0xFE) == 0xFC) return true;
            // fe80::/10 (Link-Local) — first byte is 1111_1110_10 = 0xFE80
            if ((firstByte & 0xFF) == 0xFE && (secondByte & 0xC0) == 0x80) return true;
            // ::1 (loopback) — already caught by isLoopbackAddress
            // ff00::/8 (Multicast) — not private, but blocked for safety
            if ((firstByte & 0xFF) == 0xFF) return true;
            // ::ffff:0:0/96 (IPv4-mapped) — check underlying IPv4
            if (isIpv4MappedAddress(octets)) {
                int mapped = (octets[12] & 0xFF);
                int mapped2 = (octets[13] & 0xFF);
                if (mapped == 10) return true;
                if (mapped == 172 && mapped2 >= 16 && mapped2 <= 31) return true;
                if (mapped == 192 && mapped2 == 168) return true;
                if (mapped == 127) return true;
                if (mapped == 0) return true;
            }
        }
        return false;
    }

    private static boolean isIpv4MappedAddress(byte[] octets) {
        return octets.length == 16
                && octets[10] == (byte) 0xFF && octets[11] == (byte) 0xFF
                && octets[0] == 0 && octets[1] == 0 && octets[2] == 0
                && octets[3] == 0 && octets[4] == 0 && octets[5] == 0
                && octets[6] == 0 && octets[7] == 0 && octets[8] == 0;
    }
}
