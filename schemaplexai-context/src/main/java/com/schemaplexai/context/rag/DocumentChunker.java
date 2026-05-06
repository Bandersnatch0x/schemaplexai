package com.schemaplexai.context.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Component that chunks text into overlapping segments for embedding generation.
 * Prefers splitting at sentence boundaries when possible, with fallback to character-level splitting.
 */
@Component
public class DocumentChunker {

    /**
     * Chunks the given text into overlapping segments based on the provided configuration.
     *
     * @param text   the text to chunk; may be null or empty
     * @param config the chunking configuration
     * @return list of text chunks; empty list if text is null or empty
     */
    public List<TextChunk> chunk(String text, ChunkingConfig config) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        String normalizedText = text.trim();
        int chunkSize = config.getChunkSize();
        int overlap = config.getOverlap();

        if (normalizedText.length() <= chunkSize) {
            return List.of(TextChunk.builder()
                    .index(0)
                    .content(normalizedText)
                    .startPosition(0)
                    .endPosition(normalizedText.length())
                    .build());
        }

        if (config.isSplitBySentence()) {
            return chunkBySentences(normalizedText, chunkSize, overlap);
        }

        return chunkByCharacters(normalizedText, chunkSize, overlap);
    }

    /**
     * Chunks text by attempting to respect sentence boundaries.
     * Falls back to character-level splitting when a sentence exceeds chunk size.
     */
    private List<TextChunk> chunkBySentences(String text, int chunkSize, int overlap) {
        List<TextChunk> chunks = new ArrayList<>();
        List<String> sentences = splitIntoSentences(text);

        int currentIndex = 0;
        int globalStart = 0;
        StringBuilder currentChunk = new StringBuilder();
        int chunkStartPosition = 0;

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            int sentenceStartInText = text.indexOf(sentence, globalStart);
            if (sentenceStartInText < 0) {
                sentenceStartInText = globalStart;
            }

            if (currentChunk.length() == 0) {
                chunkStartPosition = sentenceStartInText;
                currentChunk.append(sentence);
                globalStart = sentenceStartInText + sentence.length();
                continue;
            }

            if (currentChunk.length() + sentence.length() <= chunkSize) {
                currentChunk.append(sentence);
                globalStart = sentenceStartInText + sentence.length();
            } else {
                int chunkEndPosition = chunkStartPosition + currentChunk.length();
                chunks.add(TextChunk.builder()
                        .index(currentIndex++)
                        .content(currentChunk.toString().trim())
                        .startPosition(chunkStartPosition)
                        .endPosition(chunkEndPosition)
                        .build());

                String previousChunk = currentChunk.toString();
                currentChunk = new StringBuilder();

                if (overlap > 0 && previousChunk.length() > overlap) {
                    String overlapText = previousChunk.substring(previousChunk.length() - overlap);
                    currentChunk.append(overlapText);
                    chunkStartPosition = chunkEndPosition - overlap;
                } else {
                    chunkStartPosition = sentenceStartInText;
                }

                currentChunk.append(sentence);
                globalStart = sentenceStartInText + sentence.length();
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(TextChunk.builder()
                    .index(currentIndex)
                    .content(currentChunk.toString().trim())
                    .startPosition(chunkStartPosition)
                    .endPosition(chunkStartPosition + currentChunk.length())
                    .build());
        }

        return chunks;
    }

    /**
     * Chunks text purely by character boundaries with overlap.
     */
    private List<TextChunk> chunkByCharacters(String text, int chunkSize, int overlap) {
        List<TextChunk> chunks = new ArrayList<>();
        int step = chunkSize - overlap;
        int index = 0;

        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(start + chunkSize, text.length());
            String content = text.substring(start, end);

            chunks.add(TextChunk.builder()
                    .index(index++)
                    .content(content)
                    .startPosition(start)
                    .endPosition(end)
                    .build());

            if (end == text.length()) {
                break;
            }
        }

        return chunks;
    }

    /**
     * Splits text into sentences using regex pattern for sentence terminators.
     * Preserves the terminators as part of each sentence.
     */
    private List<String> splitIntoSentences(String text) {
        String[] parts = text.split("(?<=[.!?]+\\s+)");
        return Arrays.stream(parts)
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .toList();
    }
}
