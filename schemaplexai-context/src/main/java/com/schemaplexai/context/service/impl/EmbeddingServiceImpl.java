package com.schemaplexai.context.service.impl;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.service.EmbeddingService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Multi-provider embedding service supporting mock, OpenAI, and Ollama.
 * <p>
 * Configure via {@code embedding.provider} property.
 * Defaults to mock (deterministic SHA-256 based) for development and testing.
 * <p>
 * Safety guard: if {@code embedding.provider=mock} is selected while the {@code prod}
 * Spring profile is active, startup fails fast with {@link IllegalStateException} to
 * prevent silent fallback to fake embeddings in production.
 */
@Service
@ConditionalOnMissingBean(EmbeddingService.class)
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceImpl.class);
    private static final int EMBEDDING_DIMENSION = 1536;

    @Value("${embedding.openai.api-key:}")
    private String openaiApiKey;

    @Value("${embedding.openai.model:text-embedding-3-small}")
    private String openaiModel;

    @Value("${embedding.openai.url:https://api.openai.com/v1/embeddings}")
    private String openaiUrl;

    @Value("${embedding.ollama.url:http://localhost:11434/api/embeddings}")
    private String ollamaUrl;

    @Value("${embedding.ollama.model:nomic-embed-text}")
    private String ollamaModel;

    private final String selectedProvider;
    private final RestTemplate restTemplate;

    @Autowired(required = false)
    private Environment environment;

    /**
     * Test-only no-arg constructor. Defaults provider to {@code mock}.
     * Production code paths always go through the {@link Value @Value}-driven constructor.
     */
    public EmbeddingServiceImpl() {
        this("mock");
    }

    @Autowired
    public EmbeddingServiceImpl(@Value("${embedding.provider:mock}") String provider) {
        this.selectedProvider = provider != null ? provider : "mock";
        this.restTemplate = new RestTemplate();
        log.info("EmbeddingService initialized with provider: {}", this.selectedProvider);
    }

    /**
     * Fail fast if a mock provider is configured under an active {@code prod} profile.
     * Prevents silent degradation of RAG quality when a yaml line is forgotten.
     */
    @PostConstruct
    void verifyProvider() {
        if (environment == null) {
            return; // No Spring context (unit test) — skip the guard.
        }
        boolean prodActive = environment.acceptsProfiles(Profiles.of("prod"));
        if ("mock".equalsIgnoreCase(selectedProvider) && prodActive) {
            throw new IllegalStateException(
                    "embedding.provider=mock is forbidden when 'prod' profile is active. "
                            + "Configure embedding.provider=openai or ollama for production.");
        }
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

    private float[] embedWithOpenAI(String text) {
        log.info("OpenAI embedding API call for text length: {}", text.length());

        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            log.error("OpenAI API key is not configured");
            throw new BaseException(ResultCode.INTERNAL_ERROR, "OpenAI API key is not configured");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + openaiApiKey);
            headers.set("Content-Type", "application/json");

            Map<String, Object> body = Map.of(
                    "input", text,
                    "model", openaiModel
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    openaiUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                log.error("OpenAI embedding API returned empty response");
                throw new BaseException(ResultCode.INTERNAL_ERROR, "OpenAI embedding API returned empty response");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
            if (data == null || data.isEmpty()) {
                log.error("OpenAI embedding API returned no data");
                throw new BaseException(ResultCode.INTERNAL_ERROR, "OpenAI embedding API returned no data");
            }

            @SuppressWarnings("unchecked")
            List<Number> embeddingList = (List<Number>) data.get(0).get("embedding");
            if (embeddingList == null) {
                log.error("OpenAI embedding API returned null embedding");
                throw new BaseException(ResultCode.INTERNAL_ERROR, "OpenAI embedding API returned null embedding");
            }

            float[] embedding = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                embedding[i] = embeddingList.get(i).floatValue();
            }

            log.info("OpenAI embedding generated successfully, dimension: {}", embedding.length);
            return embedding;

        } catch (RestClientException e) {
            log.error("OpenAI embedding API call failed: {}", e.getMessage(), e);
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "OpenAI embedding API call failed: " + e.getMessage());
        }
    }

    private float[] embedWithOllama(String text) {
        log.info("Ollama embedding API call for text length: {}", text.length());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            Map<String, Object> body = Map.of(
                    "model", ollamaModel,
                    "prompt", text
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    ollamaUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                log.error("Ollama embedding API returned empty response");
                throw new BaseException(ResultCode.INTERNAL_ERROR, "Ollama embedding API returned empty response");
            }

            @SuppressWarnings("unchecked")
            List<Number> embeddingList = (List<Number>) responseBody.get("embedding");
            if (embeddingList == null) {
                log.error("Ollama embedding API returned null embedding");
                throw new BaseException(ResultCode.INTERNAL_ERROR, "Ollama embedding API returned null embedding");
            }

            float[] embedding = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                embedding[i] = embeddingList.get(i).floatValue();
            }

            log.info("Ollama embedding generated successfully, dimension: {}", embedding.length);
            return embedding;

        } catch (RestClientException e) {
            log.error("Ollama embedding API call failed: {}", e.getMessage(), e);
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "Ollama embedding API call failed: " + e.getMessage());
        }
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
