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

    // ----- additional defensive-path tests for higher branch coverage -----

    @Test
    void shouldRejectFrontmatterWithoutClosingDelimiter() {
        String md = """
                ---
                name: incomplete
                description: never closes
                """;
        assertThrows(ManifestParseException.class, () -> parser.parse(md));
    }

    @Test
    void shouldRejectFrontmatterWithOnlyOpeningDelimiter() {
        // single `---` and no following newline → opening line malformed
        assertThrows(ManifestParseException.class, () -> parser.parse("---"));
    }

    @Test
    void shouldRejectFrontmatterThatIsNotAMapping() {
        // frontmatter is a YAML scalar, not a mapping
        String md = """
                ---
                just-a-scalar
                ---
                body
                """;
        assertThrows(ManifestParseException.class, () -> parser.parse(md));
    }

    @Test
    void shouldRejectEmptyFrontmatter() {
        String md = """
                ---
                ---
                body
                """;
        assertThrows(ManifestParseException.class, () -> parser.parse(md));
    }

    @Test
    void shouldRejectToolsThatIsNotAList() {
        String md = """
                ---
                name: bad-tools
                tools: not-a-list
                ---
                body
                """;
        assertThrows(ManifestParseException.class, () -> parser.parse(md));
    }

    @Test
    void shouldRejectToolItemThatIsNotAMapping() {
        String md = """
                ---
                name: bad-tool-item
                tools:
                  - just-a-string
                ---
                body
                """;
        assertThrows(ManifestParseException.class, () -> parser.parse(md));
    }

    @Test
    void shouldRejectToolMissingName() {
        String md = """
                ---
                name: bad
                tools:
                  - type: builtin
                ---
                body
                """;
        assertThrows(ManifestParseException.class, () -> parser.parse(md));
    }

    @Test
    void shouldRejectToolMissingType() {
        String md = """
                ---
                name: bad
                tools:
                  - name: read_file
                ---
                body
                """;
        assertThrows(ManifestParseException.class, () -> parser.parse(md));
    }

    @Test
    void shouldRejectStringFieldWithListValue() {
        // description is a list, not a string/number/boolean — expect rejection
        String md = """
                ---
                name: bad-string
                description: [a, b, c]
                ---
                body
                """;
        assertThrows(ManifestParseException.class, () -> parser.parse(md));
    }

    @Test
    void shouldCoerceNumericFieldsFromInteger() {
        // snakeyaml may yield Integer for small ints; parser must coerce to Long
        String md = """
                ---
                name: small-ints
                maxRounds: 3
                maxTools: 1
                temperature: 1
                ---
                body
                """;
        AgentsManifest m = parser.parse(md);
        assertEquals(3L, m.maxRounds());
        assertEquals(1L, m.maxTools());
        assertEquals(1.0, m.temperature());
    }

    @Test
    void shouldRejectInvalidTemperatureType() {
        String md = """
                ---
                name: bad-temp
                temperature: "warm"
                ---
                body
                """;
        assertThrows(ManifestParseException.class, () -> parser.parse(md));
    }

    @Test
    void shouldEmitEmptyBodyWhenFrontmatterAtEof() {
        String md = """
                ---
                name: no-body
                ---
                """;
        AgentsManifest m = parser.parse(md);
        assertEquals("no-body", m.name());
        assertNotNull(m.systemPrompt());
    }
}

