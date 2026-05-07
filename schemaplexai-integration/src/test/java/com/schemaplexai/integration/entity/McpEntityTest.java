package com.schemaplexai.integration.entity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for SfMcpServer entity with extended MCP fields.
 * Tests that new fields (command, args, envVars, serverPublicKey,
 * protocolVersion, toolWhitelist) exist and are accessible.
 */
class McpEntityTest {

    @Test
    void shouldSetAndGetMcpServerWithNewFields() {
        SfMcpServer server = new SfMcpServer();
        server.setTenantId("1");
        server.setName("github-mcp");
        server.setEndpoint("https://mcp.github.com");
        server.setTransport("sse");
        server.setStatus(1);
        server.setCommand("npx");
        server.setArgs(List.of("-y", "@modelcontextprotocol/server-github"));
        server.setEnvVars(Map.of("GITHUB_TOKEN", "token123"));
        server.setServerPublicKey("ssh-rsa AAAA...");
        server.setProtocolVersion("2024-11-05");
        server.setToolWhitelist(List.of("search_repos", "get_file"));

        assertEquals("github-mcp", server.getName());
        assertEquals("npx", server.getCommand());
        assertEquals(List.of("-y", "@modelcontextprotocol/server-github"), server.getArgs());
        assertEquals(Map.of("GITHUB_TOKEN", "token123"), server.getEnvVars());
        assertEquals("ssh-rsa AAAA...", server.getServerPublicKey());
        assertEquals("2024-11-05", server.getProtocolVersion());
        assertEquals(List.of("search_repos", "get_file"), server.getToolWhitelist());
    }

    @Test
    void shouldAllowNullOptionalFields() {
        SfMcpServer server = new SfMcpServer();
        server.setName("minimal-server");
        server.setEndpoint("http://localhost:3000");
        server.setTransport("stdio");
        server.setStatus(1);

        assertNull(server.getCommand());
        assertNull(server.getArgs());
        assertNull(server.getEnvVars());
        assertNull(server.getServerPublicKey());
        assertNull(server.getProtocolVersion());
        assertNull(server.getToolWhitelist());
    }

    @Test
    void shouldSupportSseTransportWithToolWhitelist() {
        SfMcpServer server = new SfMcpServer();
        server.setName("github-mcp");
        server.setEndpoint("https://mcp.github.com");
        server.setTransport("sse");
        server.setStatus(1);
        server.setProtocolVersion("2024-11-05");
        server.setToolWhitelist(List.of("search_repos", "get_file", "create_issue"));

        assertEquals("sse", server.getTransport());
        assertEquals(3, server.getToolWhitelist().size());
        assertTrue(server.getToolWhitelist().contains("search_repos"));
    }

    @Test
    void shouldSupportStdioTransportWithCommandAndArgs() {
        SfMcpServer server = new SfMcpServer();
        server.setName("filesystem-mcp");
        server.setTransport("stdio");
        server.setStatus(1);
        server.setCommand("npx");
        server.setArgs(List.of("-y", "@modelcontextprotocol/server-filesystem", "/data"));
        server.setEnvVars(Map.of("ALLOWED_DIR", "/data"));

        assertEquals("stdio", server.getTransport());
        assertEquals("npx", server.getCommand());
        assertEquals(3, server.getArgs().size());
        assertEquals("/data", server.getEnvVars().get("ALLOWED_DIR"));
    }
}
