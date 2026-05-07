package com.schemaplexai.context.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentChunkerTest {

    private DocumentChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new DocumentChunker();
    }

    @Test
    void chunk_longText_returnsMultipleChunksWithOverlap() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            sb.append("This is sentence number ").append(i).append(". ");
        }
        String text = sb.toString();

        ChunkingConfig config = ChunkingConfig.defaults();
        config.setChunkSize(200);
        config.setSplitBySentence(false);
        List<TextChunk> chunks = chunker.chunk(text, config);

        assertFalse(chunks.isEmpty(), "Should produce at least one chunk");
        assertTrue(chunks.size() > 1, "Long text should produce multiple chunks");

        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            assertEquals(i, chunk.getIndex(), "Chunk index should be sequential");
            assertNotNull(chunk.getContent(), "Chunk content should not be null");
            assertFalse(chunk.getContent().isBlank(), "Chunk content should not be blank");
            assertTrue(chunk.getEndPosition() > chunk.getStartPosition(), "End position should be greater than start");
        }
    }

    @Test
    void chunk_shortText_returnsSingleChunk() {
        String text = "Short text.";

        List<TextChunk> chunks = chunker.chunk(text, ChunkingConfig.defaults());

        assertEquals(1, chunks.size(), "Short text should return exactly one chunk");
        assertEquals(0, chunks.get(0).getIndex());
        assertEquals(text, chunks.get(0).getContent());
        assertEquals(0, chunks.get(0).getStartPosition());
        assertEquals(text.length(), chunks.get(0).getEndPosition());
    }

    @Test
    void chunk_emptyText_returnsEmptyList() {
        List<TextChunk> chunks = chunker.chunk("", ChunkingConfig.defaults());
        assertTrue(chunks.isEmpty(), "Empty text should return empty list");
    }

    @Test
    void chunk_blankText_returnsEmptyList() {
        List<TextChunk> chunks = chunker.chunk("   ", ChunkingConfig.defaults());
        assertTrue(chunks.isEmpty(), "Blank text should return empty list");
    }

    @Test
    void chunk_nullText_returnsEmptyList() {
        List<TextChunk> chunks = chunker.chunk(null, ChunkingConfig.defaults());
        assertTrue(chunks.isEmpty(), "Null text should return empty list");
    }

    @Test
    void chunk_overlapBehavior_chunksShareOverlapContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            sb.append("Sentence ").append(i).append(" has enough words to make it longer. ");
        }
        String text = sb.toString();

        ChunkingConfig config = new ChunkingConfig();
        config.setChunkSize(100);
        config.setOverlap(20);
        config.setSplitBySentence(false);

        List<TextChunk> chunks = chunker.chunk(text, config);

        assertTrue(chunks.size() >= 2, "Should produce multiple chunks");

        for (int i = 1; i < chunks.size(); i++) {
            TextChunk prev = chunks.get(i - 1);
            TextChunk curr = chunks.get(i);

            assertTrue(curr.getStartPosition() < prev.getEndPosition(),
                    "Current chunk should start before previous chunk ends (overlap)");
        }
    }

    @Test
    void chunk_sentenceSplit_respectsSentenceBoundaries() {
        String text = "First sentence here. Second sentence follows. Third one too.";

        ChunkingConfig config = new ChunkingConfig();
        config.setChunkSize(100);
        config.setOverlap(0);
        config.setSplitBySentence(true);

        List<TextChunk> chunks = chunker.chunk(text, config);

        assertEquals(1, chunks.size(), "All sentences should fit in one chunk");
        assertEquals(text, chunks.get(0).getContent());
    }

    @Test
    void chunk_positionsAreConsistent() {
        String text = "The quick brown fox jumps over the lazy dog. " +
                "Pack my box with five dozen liquor jugs. " +
                "How vexingly quick daft zebras jump. " +
                "Sphinx of black quartz, judge my vow. " +
                "Two driven jocks help fax my big quiz. " +
                "The five boxing wizards jump quickly. ";

        List<TextChunk> chunks = chunker.chunk(text, ChunkingConfig.defaults());

        for (TextChunk chunk : chunks) {
            String expected = text.substring(chunk.getStartPosition(), chunk.getEndPosition());
            assertEquals(expected, chunk.getContent(),
                    "Chunk content should match the substring from original text at its positions");
        }
    }
}
