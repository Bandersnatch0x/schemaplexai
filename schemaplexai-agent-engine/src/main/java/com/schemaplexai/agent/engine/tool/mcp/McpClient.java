package com.schemaplexai.agent.engine.tool.mcp;

import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Represents a connection to a single MCP server.
 * <p>
 * This is a lightweight wrapper holding connection state.
 * The actual MCP protocol implementation will be added in a later phase.
 * <p>
 * Thread-safety: {@code connected} is volatile for safe cross-thread reads.
 */
@Getter
public final class McpClient {

    private final String endpoint;
    private volatile boolean connected;
    private final Instant createdAt;
    private volatile Instant lastAccessedAt;

    public McpClient(String endpoint) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint must not be null");
        this.connected = true;
        this.createdAt = Instant.now();
        this.lastAccessedAt = this.createdAt;
    }

    /**
     * Mark this client as disconnected.
     */
    public void close() {
        this.connected = false;
    }

    /**
     * Update the last-accessed timestamp (called on each use).
     */
    public void touch() {
        this.lastAccessedAt = Instant.now();
    }

    /**
     * List tools available on this MCP server.
     * <p>
     * Stub implementation — returns empty list. Will be replaced with
     * actual MCP protocol call in a later phase.
     *
     * @return list of tools exposed by the server
     */
    public List<McpTool> listTools() {
        return List.of();
    }

    @Override
    public String toString() {
        return "McpClient{endpoint='%s', connected=%s, createdAt=%s, lastAccessedAt=%s}"
                .formatted(endpoint, connected, createdAt, lastAccessedAt);
    }

    /**
     * Represents a tool exposed by an MCP server.
     */
    public record McpTool(String name, String description) {
    }
}
