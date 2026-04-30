package com.schemaplexai.integration.skill;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillMarkdownParserTest {

    private static final String VALID_SKILL = """
        ---
        name: web-scraper
        version: 1.0
        description: Scrape web pages and extract structured data
        tags: [web, scraper, data]
        ---

        # web-scraper

        ## Description
        Fetches a URL and extracts text content.

        ## Parameters
        - `url` (string, required): The target URL
        - `selector` (string, optional): CSS selector for extraction

        ## Steps
        1. Fetch the URL
        2. Parse HTML
        3. Extract content matching the selector
        4. Return extracted text
        """;

    @Test
    void shouldParseFrontmatterFields() {
        SkillMarkdownParser.SkillMeta meta = SkillMarkdownParser.parseMeta(VALID_SKILL);

        assertThat(meta.name()).isEqualTo("web-scraper");
        assertThat(meta.version()).isEqualTo("1.0");
        assertThat(meta.description()).contains("Scrape web pages");
        assertThat(meta.tags()).contains("scraper");
    }

    @Test
    void shouldExtractBodyContent() {
        String body = SkillMarkdownParser.parseBody(VALID_SKILL);

        assertThat(body).contains("## Description");
        assertThat(body).contains("Fetches a URL");
        assertThat(body).doesNotContain("---");
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        String badSkill = """
            ---
            name: bad-skill
            ---

            No version or description
            """;

        assertThatThrownBy(() -> SkillMarkdownParser.parseMeta(badSkill))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Missing required fields");
    }

    @Test
    void shouldRejectMissingFrontmatter() {
        String noFrontmatter = "# Just markdown\n\nNo frontmatter here.";

        assertThatThrownBy(() -> SkillMarkdownParser.parseMeta(noFrontmatter))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("frontmatter");
    }

    @Test
    void shouldParseBodyWhenNoFrontmatter() {
        String noFrontmatter = "# Just markdown\n\nNo frontmatter here.";

        String body = SkillMarkdownParser.parseBody(noFrontmatter);

        assertThat(body).isEqualTo(noFrontmatter);
    }

    @Test
    void shouldHandleEmptyTags() {
        String skillNoTags = """
            ---
            name: minimal-skill
            version: 0.1
            description: A minimal skill
            ---

            # Minimal
            """;

        SkillMarkdownParser.SkillMeta meta = SkillMarkdownParser.parseMeta(skillNoTags);

        assertThat(meta.name()).isEqualTo("minimal-skill");
        assertThat(meta.tags()).isEmpty();
    }
}
