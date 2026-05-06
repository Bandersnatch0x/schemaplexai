package com.schemaplexai.context.service.impl;

import com.schemaplexai.context.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simulated embedding service that generates deterministic hash-based embeddings.
 * <p>
 * TODO: Replace with real OpenAI / Ollama / local embedding API integration.
 * This implementation is for development and testing only.
 */
@Service
@ConditionalOnMissingBean(EmbeddingService.class)
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceImpl.class);
    private static final int EMBEDDING_DIMENSION = 1536;

    @Override
    public float[] embed(String text) {
        if (text == null) {
            text = "";
        }

        long seed = computeHashSeed(text);
        Random random = new Random(seed);
        float[] embedding = new float[EMBEDDING_DIMENSION];

        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            embedding[i] = random.nextFloat() * 2.0f - 1.0f;
        }

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
