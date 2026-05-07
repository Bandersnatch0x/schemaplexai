package com.schemaplexai.agent.engine.tool.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class McpToolRefTest {

    @Test
    void shouldParseValidMcpRef() {
        McpToolRef ref = McpToolRef.parse("mcp:github:read_file");

        assertThat(ref).isNotNull();
        assertThat(ref.serverId()).isEqualTo("github");
        assertThat(ref.toolName()).isEqualTo("read_file");
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertThat(McpToolRef.parse(null)).isNull();
    }

    @Test
    void shouldReturnNullForNonMcpPrefix() {
        assertThat(McpToolRef.parse("github:read_file")).isNull();
    }

    @Test
    void shouldReturnNullForMissingColon() {
        assertThat(McpToolRef.parse("mcp:github")).isNull();
    }

    @Test
    void shouldReturnNullForBlankServerId() {
        assertThat(McpToolRef.parse("mcp::read_file")).isNull();
    }

    @Test
    void shouldReturnNullForBlankToolName() {
        assertThat(McpToolRef.parse("mcp:github:")).isNull();
    }

    @Test
    void shouldRoundTripViaToString() {
        McpToolRef ref = new McpToolRef("github", "read_file");
        assertThat(ref.toString()).isEqualTo("mcp:github:read_file");
    }
}
