package com.schemaplexai.context.service;

import java.util.List;

/**
 * Service for generating vector embeddings from text.
 */
public interface EmbeddingService {

    /**
     * Generates an embedding vector for a single text string.
     *
     * @param text the text to embed
     * @return the embedding vector
     */
    float[] embed(String text);

    /**
     * Generates embedding vectors for a batch of texts.
     *
     * @param texts the texts to embed
     * @return list of embedding vectors in the same order as input
     */
    List<float[]> embedBatch(List<String> texts);
}
