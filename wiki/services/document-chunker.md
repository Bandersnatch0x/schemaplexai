---
title: DocumentChunker
type: service
source: schemaplexai-context/src/main/java/com/schemaplexai/context/rag/DocumentChunker.java
creation_date: 2026-05-06
update_date: 2026-05-06
tags: [service, rag, chunking, embedding, context]
confidence: high
---

# DocumentChunker

> One-sentence summary: Sentence-aware text splitting component that divides documents into overlapping segments for RAG embedding generation.

## Responsibilities

1. Split text into overlapping chunks suitable for vector embedding
2. Prefer sentence boundaries to preserve semantic coherence
3. Fall back to character-level splitting when sentences exceed chunk size
4. Track chunk index and character positions for traceability
5. Handle edge cases: null/empty text, short text, overlap behavior

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `DocumentChunker` | `schemaplexai-context/src/main/java/com/schemaplexai/context/rag/DocumentChunker.java` | Core chunking logic |
| `ChunkingConfig` | `schemaplexai-context/src/main/java/com/schemaplexai/context/rag/ChunkingConfig.java` | Configuration POJO |
| `TextChunk` | `schemaplexai-context/src/main/java/com/schemaplexai/context/rag/TextChunk.java` | Chunk representation with index, content, positions |

### Algorithm

- **Sentence splitting**: regex `(?<=[.!?]+\s+)` — splits after sentence terminators while preserving them
- **Sentence-aware chunking**: accumulates sentences into chunks until adding the next sentence would exceed `chunkSize`, then starts a new chunk with optional overlap from the previous chunk
- **Character-level fallback**: when `splitBySentence=false` or a single sentence exceeds `chunkSize`, uses fixed-size sliding windows with `step = chunkSize - overlap`

### Configuration (ChunkingConfig defaults)

| Property | Default | Description |
|----------|---------|-------------|
| `chunkSize` | 512 | Maximum characters per chunk |
| `overlap` | 50 | Overlapping characters between consecutive chunks |
| `splitBySentence` | true | Prefer splitting at sentence boundaries |

### Edge Cases

- **Null or blank text**: returns empty list (no exception)
- **Text shorter than chunkSize**: returns single chunk containing entire text
- **Overlap larger than previous chunk**: overlap is clamped to previous chunk length; start position resets to sentence boundary
- **Sentence boundary search failure**: falls back to `globalStart` position tracking

### Key Code

```java
public List<TextChunk> chunk(String text, ChunkingConfig config) {
    if (text == null || text.isBlank()) {
        return new ArrayList<>();
    }
    // ... sentence-aware or character-level splitting
}
```

## Related

- [[services/embedding-service]] — generates vectors from chunk content
- [[services/milvus-sync-service]] — persists chunked+embedded documents to Milvus
- [[services/rag-search-service]] — searches over embedded chunks
- [[services/rag-service]] — parent RAG orchestration service
- [[entities/context]] — knowledge document entity
