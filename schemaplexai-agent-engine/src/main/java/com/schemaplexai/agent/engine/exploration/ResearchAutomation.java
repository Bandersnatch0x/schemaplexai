package com.schemaplexai.agent.engine.exploration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that automates research workflows: search, synthesize, summarize.
 * Simulates finding sources, synthesizing findings, and generating a final summary.
 */
@Slf4j
@Service
public class ResearchAutomation {

    private static final int MAX_DEPTH = 5;
    private static final double MIN_RELEVANCE_THRESHOLD = 0.3;

    private final Map<String, List<Source>> researchCache = new HashMap<>();

    /**
     * Researches a topic by simulating a search and returning discovered sources.
     *
     * @param topic the research topic
     * @param depth the desired search depth (1-5), higher means more sources
     * @return list of discovered sources sorted by relevance descending
     */
    public List<Source> researchTopic(String topic, int depth) {
        if (topic == null || topic.isBlank()) {
            return Collections.emptyList();
        }
        int clampedDepth = Math.max(1, Math.min(depth, MAX_DEPTH));

        List<Source> sources = simulateSearch(topic, clampedDepth);
        researchCache.put(topic.trim().toLowerCase(Locale.ROOT), sources);
        log.info("Researched topic='{}' with depth={}, found {} sources", topic, clampedDepth, sources.size());
        return Collections.unmodifiableList(sources);
    }

    /**
     * Synthesizes findings from a list of sources into a coherent analysis.
     *
     * @param sources the sources to synthesize
     * @return a synthesized findings string
     */
    public String synthesizeFindings(List<Source> sources) {
        if (sources == null || sources.isEmpty()) {
            return "No findings to synthesize.";
        }

        double avgRelevance = sources.stream()
                .mapToDouble(Source::relevanceScore)
                .average()
                .orElse(0.0);

        String keyThemes = sources.stream()
                .filter(s -> s.relevanceScore() >= MIN_RELEVANCE_THRESHOLD)
                .map(Source::title)
                .distinct()
                .limit(5)
                .collect(Collectors.joining(", "));

        return String.format(Locale.ROOT,
                "Synthesized %d sources (avg relevance %.2f). Key themes: %s.",
                sources.size(), avgRelevance, keyThemes.isEmpty() ? "none" : keyThemes);
    }

    /**
     * Generates a summary for a researched topic.
     *
     * @param topic the topic to summarize
     * @return a summary string, or a message if the topic has not been researched
     */
    public String generateSummary(String topic) {
        if (topic == null || topic.isBlank()) {
            return "No topic provided for summary.";
        }
        String key = topic.trim().toLowerCase(Locale.ROOT);
        List<Source> sources = researchCache.get(key);
        if (sources == null || sources.isEmpty()) {
            return "Topic has not been researched yet.";
        }

        String topSourceTitles = sources.stream()
                .sorted(Comparator.comparingDouble(Source::relevanceScore).reversed())
                .limit(3)
                .map(Source::title)
                .collect(Collectors.joining(", "));

        return String.format(Locale.ROOT,
                "Summary of '%s': based on %d sources. Top references: %s.",
                topic, sources.size(), topSourceTitles);
    }

    private List<Source> simulateSearch(String topic, int depth) {
        int sourceCount = depth * 2;
        List<Source> sources = new ArrayList<>();
        int seed = Objects.hash(topic, depth);
        Random random = new Random(seed);

        for (int i = 0; i < sourceCount; i++) {
            double relevance = clamp(random.nextDouble(0.1, 1.0), 0.0, 1.0);
            String url = "https://example.com/search/" + topic.replaceAll("\\s+", "-") + "-" + i;
            String title = topic + " - Source " + (i + 1);
            String content = "Extracted content for " + title + ".";
            sources.add(new Source(url, title, content, relevance, Instant.now()));
        }

        return sources.stream()
                .sorted(Comparator.comparingDouble(Source::relevanceScore).reversed())
                .collect(Collectors.toList());
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
