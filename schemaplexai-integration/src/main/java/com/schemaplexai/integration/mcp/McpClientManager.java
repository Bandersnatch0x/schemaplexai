package com.schemaplexai.integration.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfMcpServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache of {@link McpClient} instances keyed by server endpoint.
 * <p>
 * Issue 930: discovery and invocation share the same client per endpoint so
 * the MCP initialize handshake (and any server session) is established once
 * and reused across the discover → register → execute chain. Callers must
 * {@link #invalidate(String)} an endpoint after a structured failure so the
 * next interaction re-initializes instead of reusing a poisoned session.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpClientManager {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final Map<String, McpClient> clients = new ConcurrentHashMap<>();

    /**
     * Get or create the client for the server's configured endpoint.
     * The endpoint is always taken from the server configuration row — the
     * numeric server id is never used as an endpoint.
     *
     * @throws BaseException INTEGRATION_NOT_FOUND when the server has no endpoint
     */
    public McpClient forServer(SfMcpServer server) {
        if (server == null || server.getEndpoint() == null || server.getEndpoint().isBlank()) {
            throw new BaseException(ResultCode.INTEGRATION_NOT_FOUND,
                    "MCP server has no configured endpoint");
        }
        String endpoint = server.getEndpoint();
        return clients.computeIfAbsent(endpoint,
                ep -> new McpClient(ep, restTemplate, objectMapper));
    }

    /**
     * Drop the cached client for an endpoint (idempotent). Used after a
     * structured failure so the next call re-runs the initialize handshake.
     */
    public void invalidate(String endpoint) {
        if (endpoint == null) {
            return;
        }
        if (clients.remove(endpoint) != null) {
            log.info("Invalidated MCP client for endpoint {}", endpoint);
        }
    }

    /** Number of cached clients (diagnostics/tests). */
    public int size() {
        return clients.size();
    }
}
