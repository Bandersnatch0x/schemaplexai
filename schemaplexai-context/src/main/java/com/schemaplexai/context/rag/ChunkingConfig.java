package com.schemaplexai.context.rag;

/**
 * Configuration for document chunking operations.
 */
public class ChunkingConfig {

    /**
     * Maximum number of characters per chunk. Default: 512.
     */
    private int chunkSize = 512;

    /**
     * Number of overlapping characters between consecutive chunks. Default: 50.
     */
    private int overlap = 50;

    /**
     * Whether to prefer splitting at sentence boundaries. Default: true.
     */
    private boolean splitBySentence = true;

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getOverlap() {
        return overlap;
    }

    public void setOverlap(int overlap) {
        this.overlap = overlap;
    }

    public boolean isSplitBySentence() {
        return splitBySentence;
    }

    public void setSplitBySentence(boolean splitBySentence) {
        this.splitBySentence = splitBySentence;
    }

    /**
     * Returns a default configuration with standard settings.
     */
    public static ChunkingConfig defaults() {
        return new ChunkingConfig();
    }
}
