package com.schemaplexai.agent.engine.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single result from RAG vector search.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private String content;
    private String source;
    private double score;

    public SearchResult(String content, double score) {
        this.content = content;
        this.score = score;
    }
}
