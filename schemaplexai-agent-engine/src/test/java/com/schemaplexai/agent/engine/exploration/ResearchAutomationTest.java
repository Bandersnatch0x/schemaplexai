package com.schemaplexai.agent.engine.exploration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ResearchAutomation")
class ResearchAutomationTest {

    private ResearchAutomation researchAutomation;

    @BeforeEach
    void setUp() {
        researchAutomation = new ResearchAutomation();
    }

    @Nested
    @DisplayName("researchTopic")
    class ResearchTopicTests {

        @Test
        @DisplayName("should return sources for a valid topic and depth")
        void shouldReturnSources() {
            List<Source> sources = researchAutomation.researchTopic("machine learning", 3);

            assertEquals(6, sources.size());
            assertThat(sources).allMatch(s -> s.relevanceScore() >= 0.0 && s.relevanceScore() <= 1.0);
        }

        @Test
        @DisplayName("should clamp depth to minimum 1")
        void shouldClampDepthToMinimum() {
            List<Source> sources = researchAutomation.researchTopic("ai", 0);
            assertEquals(2, sources.size());
        }

        @Test
        @DisplayName("should clamp depth to maximum 5")
        void shouldClampDepthToMaximum() {
            List<Source> sources = researchAutomation.researchTopic("ai", 10);
            assertEquals(10, sources.size());
        }

        @Test
        @DisplayName("should return empty list for null topic")
        void shouldReturnEmptyForNullTopic() {
            assertThat(researchAutomation.researchTopic(null, 2)).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for blank topic")
        void shouldReturnEmptyForBlankTopic() {
            assertThat(researchAutomation.researchTopic("   ", 2)).isEmpty();
        }

        @Test
        @DisplayName("should sort sources by relevance descending")
        void shouldSortByRelevanceDescending() {
            List<Source> sources = researchAutomation.researchTopic("sorting", 2);

            for (int i = 0; i < sources.size() - 1; i++) {
                assertTrue(sources.get(i).relevanceScore() >= sources.get(i + 1).relevanceScore());
            }
        }
    }

    @Nested
    @DisplayName("synthesizeFindings")
    class SynthesizeFindingsTests {

        @Test
        @DisplayName("should synthesize findings from sources")
        void shouldSynthesizeFindings() {
            List<Source> sources = List.of(
                    new Source("https://a.com", "A", "content A", 0.9, java.time.Instant.now()),
                    new Source("https://b.com", "B", "content B", 0.8, java.time.Instant.now())
            );

            String findings = researchAutomation.synthesizeFindings(sources);

            assertThat(findings).containsIgnoringCase("synthesized");
            assertThat(findings).contains("2 sources");
            assertThat(findings).contains("A, B");
        }

        @Test
        @DisplayName("should return no-findings message for empty list")
        void shouldReturnNoFindingsForEmpty() {
            assertEquals("No findings to synthesize.", researchAutomation.synthesizeFindings(Collections.emptyList()));
        }

        @Test
        @DisplayName("should return no-findings message for null input")
        void shouldReturnNoFindingsForNull() {
            assertEquals("No findings to synthesize.", researchAutomation.synthesizeFindings(null));
        }

        @Test
        @DisplayName("should omit low-relevance titles from key themes")
        void shouldOmitLowRelevanceThemes() {
            List<Source> sources = List.of(
                    new Source("https://a.com", "High", "content", 0.9, java.time.Instant.now()),
                    new Source("https://b.com", "Low", "content", 0.1, java.time.Instant.now())
            );

            String findings = researchAutomation.synthesizeFindings(sources);
            assertThat(findings).contains("High");
            assertThat(findings).doesNotContain("Low");
        }
    }

    @Nested
    @DisplayName("generateSummary")
    class GenerateSummaryTests {

        @Test
        @DisplayName("should generate summary for researched topic")
        void shouldGenerateSummary() {
            researchAutomation.researchTopic("neural networks", 2);
            String summary = researchAutomation.generateSummary("neural networks");

            assertThat(summary).containsIgnoringCase("summary");
            assertThat(summary).contains("neural networks");
            assertThat(summary).contains("4 sources");
        }

        @Test
        @DisplayName("should return not-researched message for unknown topic")
        void shouldReturnNotResearched() {
            assertEquals("Topic has not been researched yet.", researchAutomation.generateSummary("unknown topic"));
        }

        @Test
        @DisplayName("should return error for null topic")
        void shouldReturnErrorForNullTopic() {
            assertEquals("No topic provided for summary.", researchAutomation.generateSummary(null));
        }

        @Test
        @DisplayName("should return error for blank topic")
        void shouldReturnErrorForBlankTopic() {
            assertEquals("No topic provided for summary.", researchAutomation.generateSummary("   "));
        }
    }
}
