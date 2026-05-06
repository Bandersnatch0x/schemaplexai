package com.schemaplexai.common.manifest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentsManifestParserTest {

    private final AgentsManifestParser parser = new AgentsManifestParser();

    @Test
    void shouldParseCompleteManifest() {
        String md = """
                ---
                name: code-reviewer
                description: Reviews code changes for quality and security
                model: claude-sonnet-4-6
                type: review
                maxRounds: 8
                maxTools: 4
                maxInputTokens: 32000
                maxOutputTokens: 4000
                temperature: 0.3
                executionMode: single
                tools:
                  - name: file_read
                    type: builtin
                  - name: grep
                    type: builtin
                    config: '{"max_results": 50}'
                ---

                You are a senior code reviewer.

                Focus on:
                - Security vulnerabilities
                - Performance issues
                """;

        AgentsManifest manifest = parser.parse(md);

        assertEquals("code-reviewer", manifest.name());
        assertEquals("Reviews code changes for quality and security", manifest.description());
        assertEquals("claude-sonnet-4-6", manifest.modelId());
        assertEquals("review", manifest.type());
        assertEquals(8L, manifest.maxRounds());
        assertEquals(4L, manifest.maxTools());
        assertEquals(32000L, manifest.maxInputTokens());
        assertEquals(4000L, manifest.maxOutputTokens());
        assertEquals(0.3, manifest.temperature());
        assertEquals("single", manifest.executionMode());
        assertEquals(2, manifest.tools().size());
        assertEquals("file_read", manifest.tools().get(0).name());
        assertEquals("builtin", manifest.tools().get(0).type());
        assertEquals("grep", manifest.tools().get(1).name());
        assertTrue(manifest.tools().get(1).configJson().contains("max_results"));
        assertTrue(manifest.systemPrompt().contains("senior code reviewer"));
        assertTrue(manifest.systemPrompt().contains("Security vulnerabilities"));
    }

    @Test
    void shouldParseMinimalManifest() {
        String md = """
                ---
                name: minimal-agent
                ---

                Simple system prompt.
                """;

        AgentsManifest manifest = parser.parse(md);
        assertEquals("minimal-agent", manifest.name());
        assertNull(manifest.description());
        assertNull(manifest.modelId());
        assertNull(manifest.maxRounds());
        assertEquals("Simple system prompt.", manifest.systemPrompt().trim());
        assertTrue(manifest.tools().isEmpty(), "tools default to empty");
    }

    @Test
    void shouldRejectMissingName() {
        String md = """
                ---
                description: no name here
                ---
                Body
                """;
        assertThrows(ManifestParseException.class, () -> parser.parse(md));
    }

    @Test
    void shouldRejectMissingFrontmatter() {
        String md = "Just a body with no frontmatter at all.\n";
        assertThrows(ManifestParseException.class, () -> parser.parse(md));
    }

    @Test
    void shouldRejectMalformedYaml() {
        String md = """
                ---
                name: bad
                tools: [unclosed
                ---
                body
                """;
        assertThrows(ManifestParseException.class, () -> parser.parse(md));
    }

    @Test
    void shouldRejectNullInput() {
        assertThrows(ManifestParseException.class, () -> parser.parse(null));
    }

    @Test
    void shouldRejectBlankInput() {
        assertThrows(ManifestParseException.class, () -> parser.parse("   \n  "));
    }

    @Test
    void shouldPreserveBodyNewlines() {
        String md = """
                ---
                name: a
                ---

                Line 1
                Line 2

                Line 4
                """;
        AgentsManifest m = parser.parse(md);
        String body = m.systemPrompt();
        assertTrue(body.contains("Line 1\nLine 2"), "consecutive lines preserved");
        assertTrue(body.contains("\n\nLine 4"), "blank line preserved");
    }

    @Test
    void shouldHandleBilingualText() {
        String md = """
                ---
                name: 中文-agent
                description: 中英文混合的描述 with English
                ---

                你是一个评审员，请关注：
                - 安全漏洞
                - Performance issues
                """;
        AgentsManifest m = parser.parse(md);
        assertEquals("中文-agent", m.name());
        assertTrue(m.description().contains("中英文"));
        assertTrue(m.systemPrompt().contains("评审员"));
        assertTrue(m.systemPrompt().contains("Performance"));
    }

    @Test
    void shouldRejectInvalidNumericFields() {
        String md = """
                ---
                name: bad-types
                maxRounds: "not a number"
                ---
                body
                """;
        assertThrows(ManifestParseException.class, () -> parser.parse(md));
    }

    @Test
    void shouldDefaultEmptyToolsWhenAbsent() {
        String md = """
                ---
                name: no-tools-agent
                description: Has no tools
                ---

                System prompt.
                """;
        AgentsManifest m = parser.parse(md);
        assertNotNull(m.tools());
        assertTrue(m.tools().isEmpty());
    }

    @Test
    void shouldAcceptToolWithoutConfig() {
        String md = """
                ---
                name: simple-tools
                tools:
                  - name: read_file
                    type: builtin
                ---
                body
                """;
        AgentsManifest m = parser.parse(md);
        assertEquals(1, m.tools().size());
        assertEquals("read_file", m.tools().get(0).name());
        assertNull(m.tools().get(0).configJson());
    }
}
