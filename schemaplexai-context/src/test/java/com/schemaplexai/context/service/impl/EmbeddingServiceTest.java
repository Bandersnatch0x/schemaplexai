package com.schemaplexai.context.service.impl;

import com.schemaplexai.context.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingServiceTest {

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingServiceImpl();
    }

    @Test
    void embed_sameTextProducesSameVector() {
        String text = "The quick brown fox jumps over the lazy dog.";

        float[] embedding1 = embeddingService.embed(text);
        float[] embedding2 = embeddingService.embed(text);

        assertArrayEquals(embedding1, embedding2,
                "Same text should produce identical embeddings");
    }

    @Test
    void embed_differentTextProducesDifferentVector() {
        float[] embedding1 = embeddingService.embed("Hello world");
        float[] embedding2 = embeddingService.embed("Goodbye world");

        boolean allEqual = true;
        for (int i = 0; i < embedding1.length; i++) {
            if (embedding1[i] != embedding2[i]) {
                allEqual = false;
                break;
            }
        }
        assertFalse(allEqual, "Different texts should produce different embeddings");
    }

    @Test
    void embed_dimensionIs1536() {
        float[] embedding = embeddingService.embed("test");
        assertEquals(1536, embedding.length, "Embedding dimension should be 1536");
    }

    @Test
    void embed_valuesAreWithinRange() {
        float[] embedding = embeddingService.embed("test");

        for (float value : embedding) {
            assertTrue(value >= -1.0f && value <= 1.0f,
                    "Embedding values should be between -1 and 1, got: " + value);
        }
    }

    @Test
    void embed_nullTextHandled() {
        float[] embedding = embeddingService.embed(null);
        assertNotNull(embedding, "Null text should produce non-null embedding");
        assertEquals(1536, embedding.length, "Null text embedding should have correct dimension");
    }

    @Test
    void embedBatch_returnsCorrectSize() {
        List<String> texts = Arrays.asList("First text", "Second text", "Third text");

        List<float[]> embeddings = embeddingService.embedBatch(texts);

        assertEquals(3, embeddings.size(), "Batch should return same number of embeddings as inputs");
    }

    @Test
    void embedBatch_eachEmbeddingHasCorrectDimension() {
        List<String> texts = Arrays.asList("A", "B", "C");

        List<float[]> embeddings = embeddingService.embedBatch(texts);

        for (float[] embedding : embeddings) {
            assertEquals(1536, embedding.length, "Each embedding should have dimension 1536");
        }
    }

    @Test
    void embedBatch_embeddingsAreDeterministic() {
        List<String> texts = Arrays.asList("Hello", "World", "Test");

        List<float[]> embeddings1 = embeddingService.embedBatch(texts);
        List<float[]> embeddings2 = embeddingService.embedBatch(texts);

        for (int i = 0; i < embeddings1.size(); i++) {
            assertArrayEquals(embeddings1.get(i), embeddings2.get(i),
                    "Batch embeddings should be deterministic for same inputs");
        }
    }

    @Test
    void embedBatch_emptyList_returnsEmptyList() {
        List<float[]> embeddings = embeddingService.embedBatch(Collections.emptyList());
        assertTrue(embeddings.isEmpty(), "Empty input list should return empty output list");
    }
}
