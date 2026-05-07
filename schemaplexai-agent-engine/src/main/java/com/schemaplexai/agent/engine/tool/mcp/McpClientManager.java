package com.schemaplexai.agent.engine.tool.mcp;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.schemaplexai.common.exception.BaseException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine-bounded connection pool for MCP clients.
 * <p>
 * Key = server endpoint URL, Value = {@link McpClient}.
 * <ul>
 *   <li>Maximum 32 concurrent connections</li>
 *   <li>Entries expire after 10 minutes of idle access</li>
 *   <li>Evicted clients are automatically closed</li>
 * </ul>
 */
@Slf4j
@Component
public class McpClientManager {

    /** Error code for MCP connection pool errors. */
    private static final int MCP_ERROR_CODE = 2001;

    /** Maximum number of concurrent MCP connections. */
    private static final int MAX_CONNECTIONS = 32;

    /** Idle timeout in minutes before a connection is evicted. */
    private static final int IDLE_TIMEOUT_MINUTES = 10;

    private final Cache<String, McpClient> clientPool;

    public McpClientManager() {
        this.clientPool = Caffeine.newBuilder()
                .maximumSize(MAX_CONNECTIONS)
                .expireAfterAccess(IDLE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .removalListener(this::onEviction)
                .build();
    }

    /**
     * Get or create an MCP client for the given server endpoint.
     * <p>
     * If a client already exists in the pool for this endpoint, it is returned.
     * Otherwise, a new client is created, cached, and returned.
     *
     * @param endpoint the MCP server endpoint URL (e.g. {@code http://mcp-server:8080})
     * @return a connected MCP client for the endpoint
     * @throws BaseException if the endpoint is null or blank
     */
    public McpClient create(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new BaseException(MCP_ERROR_CODE, "MCP server endpoint must not be null or blank");
        }
        return clientPool.get(endpoint, this::createClient);
    }

    /**
     * Retrieve a cached MCP client without creating a new one.
     *
     * @param endpoint the MCP server endpoint URL
     * @return the cached client, or {@code null} if no client exists for this endpoint
     */
    public McpClient get(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return null;
        }
        McpClient client = clientPool.getIfPresent(endpoint);
        if (client != null) {
            client.touch();
        }
        return client;
    }

    /**
     * Evict and close the MCP client for the given endpoint.
     * <p>
     * Idempotent: does nothing if no client exists for the endpoint.
     *
     * @param endpoint the MCP server endpoint URL
     */
    public void close(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return;
        }
        clientPool.invalidate(endpoint);
    }

    /**
     * Evict and close all MCP clients in the pool.
     */
    public void closeAll() {
        clientPool.invalidateAll();
        log.info("All MCP clients closed (pool drained)");
    }

    /**
     * Lifecycle hook: evict and close all MCP clients on application shutdown.
     */
    @PreDestroy
    public void preDestroy() {
        closeAll();
    }

    /**
     * Return the current number of clients in the pool.
     */
    public long size() {
        return clientPool.estimatedSize();
    }

    // ── internal ────────────────────────────────────────────────────

    private McpClient createClient(String endpoint) {
        log.info("Creating new MCP client for endpoint: {}", endpoint);
        return new McpClient(endpoint);
    }

    /**
     * Called when an entry is evicted from the cache (expired or size-evicted).
     * Ensures the client is properly closed.
     */
    private void onEviction(String endpoint, McpClient client, RemovalCause cause) {
        if (client != null && client.isConnected()) {
            log.info("Evicting MCP client for endpoint: {} (cause: {})", endpoint, cause);
            client.close();
        }
    }
}
