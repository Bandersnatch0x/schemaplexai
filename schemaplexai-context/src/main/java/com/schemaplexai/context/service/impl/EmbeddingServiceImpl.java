package com.schemaplexai.context.service.impl;

import com.schemaplexai.context.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Multi-provider embedding service supporting mock, OpenAI, and Ollama.
 * <p>
 * Configure via {@code embedding.provider} property.
 * Defaults to mock (deterministic SHA-256 based) for development and testing.
 */
@Service
@ConditionalOnMissingBean(EmbeddingService.class)
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceImpl.class);
    private static final int EMBEDDING_DIMENSION = 1536;

    @Value("${embedding.provider:mock}")
    private String provider;

    private final String selectedProvider;

    public EmbeddingServiceImpl(@Value("${embedding.provider:mock}") String provider) {
        this.selectedProvider = provider != null ? provider : "mock";
        log.info("EmbeddingService initialized with provider: {}", this.selectedProvider);
    }

    @Override
    public float[] embed(String text) {
        if (text == null) {
            text = "";
        }

        return switch (selectedProvider) {
            case "openai" -> embedWithOpenAI(text);
            case "ollama" -> embedWithOllama(text);
            default -> embedMock(text);
        };
    }

    private float[] embedMock(String text) {
        long seed = computeHashSeed(text);
        Random random = new Random(seed);
        float[] embedding = new float[EMBEDDING_DIMENSION];

        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            embedding[i] = random.nextFloat() * 2.0f - 1.0f;
        }

        return embedding;
    }

    /**
     * TODO: Integrate with OpenAI Embeddings API.
     * Expected pattern: POST https://api.openai.com/v1/embeddings
     * with body {"input": text, "model": "text-embedding-3-small"}
     * and Authorization header with API key.
     */
    private float[] embedWithOpenAI(String text) {
        log.info("OpenAI embedding API call for text length: {}", text.length());
        float[] embedding = new float[EMBEDDING_DIMENSION];
        // Placeholder: real HTTP call to OpenAI API to be implemented
        return embedding;
    }

    /**
     * TODO: Integrate with Ollama Embeddings API.
     * Expected pattern: POST http://localhost:11434/api/embeddings
     * with body {"model": "nomic-embed-text", "prompt": text}
     */
    private float[] embedWithOllama(String text) {
        log.info("Ollama embedding API call for text length: {}", text.length());
        float[] embedding = new float[EMBEDDING_DIMENSION];
        // Placeholder: real HTTP call to Ollama API to be implemented
        return embedding;
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> embeddings = new ArrayList<>(texts.size());
        for (String text : texts) {
            embeddings.add(embed(text));
        }
        return embeddings;
    }

    /**
     * Computes a deterministic seed from the text hash.
     */
    private long computeHashSeed(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            long seed = 0;
            for (int i = 0; i < 8 && i < hash.length; i++) {
                seed = (seed << 8) | (hash[i] & 0xFFL);
            }
            return seed;
        } catch (NoSuchAlgorithmException e) {
            log.warn("SHA-256 not available, falling back to String.hashCode()");
            return text.hashCode();
        }
    }
}
