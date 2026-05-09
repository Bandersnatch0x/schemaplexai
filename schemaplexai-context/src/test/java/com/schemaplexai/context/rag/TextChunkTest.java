package com.schemaplexai.context.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkTest {

    @Test
    void defaultConstructor_createsEmptyChunk() {
        TextChunk chunk = new TextChunk();
        assertEquals(0, chunk.getIndex());
        assertNull(chunk.getContent());
        assertEquals(0, chunk.getStartPosition());
        assertEquals(0, chunk.getEndPosition());
        assertNull(chunk.getDocId());
    }

    @Test
    void parameterizedConstructor_setsAllFields() {
        TextChunk chunk = new TextChunk(1, "hello", 0, 5, 100L);
        assertEquals(1, chunk.getIndex());
        assertEquals("hello", chunk.getContent());
        assertEquals(0, chunk.getStartPosition());
        assertEquals(5, chunk.getEndPosition());
        assertEquals(100L, chunk.getDocId());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        TextChunk chunk = new TextChunk();
        chunk.setIndex(2);
        chunk.setContent("world");
        chunk.setStartPosition(10);
        chunk.setEndPosition(15);
        chunk.setDocId(200L);

        assertEquals(2, chunk.getIndex());
        assertEquals("world", chunk.getContent());
        assertEquals(10, chunk.getStartPosition());
        assertEquals(15, chunk.getEndPosition());
        assertEquals(200L, chunk.getDocId());
    }

    @Test
    void builder_createsChunkWithAllFields() {
        TextChunk chunk = TextChunk.builder()
                .index(3)
                .content("test content")
                .startPosition(20)
                .endPosition(32)
                .docId(300L)
                .build();

        assertEquals(3, chunk.getIndex());
        assertEquals("test content", chunk.getContent());
        assertEquals(20, chunk.getStartPosition());
        assertEquals(32, chunk.getEndPosition());
        assertEquals(300L, chunk.getDocId());
    }

    @Test
    void builder_buildsMinimalChunk() {
        TextChunk chunk = TextChunk.builder()
                .index(0)
                .content("a")
                .startPosition(0)
                .endPosition(1)
                .build();

        assertNull(chunk.getDocId());
    }
}
