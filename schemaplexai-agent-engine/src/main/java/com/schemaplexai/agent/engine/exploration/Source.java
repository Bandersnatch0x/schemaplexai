package com.schemaplexai.agent.engine.exploration;

import java.time.Instant;

/**
 * Record representing a research source found during automated research.
 *
 * @param url            the source URL
 * @param title          the source title
 * @param content        the extracted or summarized content
 * @param relevanceScore relevance score in range [0.0, 1.0] where higher is more relevant
 * @param timestamp      when the source was retrieved
 */
public record Source(
        String url,
        String title,
        String content,
        double relevanceScore,
        Instant timestamp
) {
}
