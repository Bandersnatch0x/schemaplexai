package com.schemaplexai.agent.engine.rag;

import java.util.List;

/**
 * Abstraction over Milvus vector database operations.
 */
public interface MilvusClient {

    /**
     * Check if a collection exists.
     *
     * @param collectionName collection name
     * @return true if the collection exists
     */
    boolean hasCollection(String collectionName);

    /**
     * Perform a vector similarity search with optional filter.
     *
     * @param collectionName   target collection
     * @param queryEmbedding   query vector
     * @param maxResults       maximum number of results
     * @param similarityThreshold minimum similarity score
     * @param filter           scalar filter expression (Milvus expression syntax)
     * @return list of search results
     */
    List<SearchResult> search(String collectionName, float[] queryEmbedding,
                              int maxResults, double similarityThreshold, String filter);
}
