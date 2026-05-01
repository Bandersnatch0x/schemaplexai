package com.schemaplexai.agent.engine.context;

/**
 * Interface for generating text embeddings (vectors).
 */
public interface EmbeddingService {

    /**
     * Generate an embedding vector for the given text.
     *
     * @param text input text
     * @return embedding vector
     */
    float[] embed(String text);
}
