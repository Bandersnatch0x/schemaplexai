package com.schemaplexai.context.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChunkingConfigTest {

    @Test
    void defaults_returnsStandardSettings() {
        ChunkingConfig config = ChunkingConfig.defaults();
        assertEquals(512, config.getChunkSize());
        assertEquals(50, config.getOverlap());
        assertTrue(config.isSplitBySentence());
    }

    @Test
    void defaultConstructor_initializesDefaults() {
        ChunkingConfig config = new ChunkingConfig();
        assertEquals(512, config.getChunkSize());
        assertEquals(50, config.getOverlap());
        assertTrue(config.isSplitBySentence());
    }

    @Test
    void setters_overrideDefaults() {
        ChunkingConfig config = new ChunkingConfig();
        config.setChunkSize(256);
        config.setOverlap(25);
        config.setSplitBySentence(false);

        assertEquals(256, config.getChunkSize());
        assertEquals(25, config.getOverlap());
        assertFalse(config.isSplitBySentence());
    }
}
