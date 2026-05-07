package com.schemaplexai.agent.engine.tool.mcp;

/**
 * Reference to an MCP tool invocation.
 * Format: mcp:<serverId>:<toolName>
 */
public record McpToolRef(String serverId, String toolName) {

    private static final String PREFIX = "mcp:";

    /**
     * Parse a tool reference string like "mcp:github:read_file".
     * Returns null if the string is not a valid MCP tool reference.
     */
    public static McpToolRef parse(String ref) {
        if (ref == null || !ref.startsWith(PREFIX)) {
            return null;
        }
        String withoutPrefix = ref.substring(PREFIX.length());
        int colonIndex = withoutPrefix.indexOf(':');
        if (colonIndex < 0) {
            return null;
        }
        String serverId = withoutPrefix.substring(0, colonIndex);
        String toolName = withoutPrefix.substring(colonIndex + 1);
        if (serverId.isBlank() || toolName.isBlank()) {
            return null;
        }
        return new McpToolRef(serverId, toolName);
    }

    @Override
    public String toString() {
        return PREFIX + serverId + ":" + toolName;
    }
}
