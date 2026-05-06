package com.schemaplexai.context.rag;

/**
 * Represents a chunk of text extracted from a document.
 */
public class TextChunk {

    /**
     * Sequential index of the chunk within the document.
     */
    private int index;

    /**
     * The text content of the chunk.
     */
    private String content;

    /**
     * Start position (character offset) in the original text.
     */
    private int startPosition;

    /**
     * End position (character offset) in the original text.
     */
    private int endPosition;

    /**
     * ID of the parent knowledge document.
     */
    private Long docId;

    public TextChunk() {
    }

    public TextChunk(int index, String content, int startPosition, int endPosition, Long docId) {
        this.index = index;
        this.content = content;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.docId = docId;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int index;
        private String content;
        private int startPosition;
        private int endPosition;
        private Long docId;

        public Builder index(int index) {
            this.index = index;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder startPosition(int startPosition) {
            this.startPosition = startPosition;
            return this;
        }

        public Builder endPosition(int endPosition) {
            this.endPosition = endPosition;
            return this;
        }

        public Builder docId(Long docId) {
            this.docId = docId;
            return this;
        }

        public TextChunk build() {
            return new TextChunk(index, content, startPosition, endPosition, docId);
        }
    }
}
