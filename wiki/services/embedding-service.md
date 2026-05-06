---
title: EmbeddingService
type: service
source: schemaplexai-context/src/main/java/com/schemaplexai/context/service/EmbeddingService.java
creation_date: 2026-05-06
update_date: 2026-05-06
tags: [service, embedding, vector, rag, context, ml]
confidence: high
---

# EmbeddingService

> One-sentence summary: Generates vector embeddings from text for RAG semantic search, with a deterministic SHA-256-based placeholder implementation designed for easy override with a real embedding API.

## Responsibilities

1. Convert text strings into dense vector representations (embeddings)
2. Support single-text and batch embedding operations
3. Provide a deterministic placeholder implementation for development/testing
4. Allow seamless replacement with real embedding APIs (OpenAI, Ollama, etc.) via `@ConditionalOnMissingBean`

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `EmbeddingService` | `schemaplexai-context/src/main/java/com/schemaplexai/context/service/EmbeddingService.java` | Interface: `embed(String)` and `embedBatch(List<String>)` |
| `EmbeddingServiceImpl` | `schemaplexai-context/src/main/java/com/schemaplexai/context/service/impl/EmbeddingServiceImpl.java` | Default SHA-256 deterministic implementation |

### Default Implementation (EmbeddingServiceImpl)

- **Algorithm**: SHA-256 hash of input text is used as a deterministic seed for `java.util.Random`
- **Dimension**: 1536 floats (compatible with OpenAI text-embedding-ada-002)
- **Range**: [-1, 1] via `random.nextFloat() * 2.0f - 1.0f`
- **Deterministic**: same input always produces same embedding (useful for testing)
- **Batch**: iterates over list, calling `embed()` per item

### Conditional Override

```java
@Service
@ConditionalOnMissingBean(EmbeddingService.class)
public class EmbeddingServiceImpl implements EmbeddingService { ... }
```

Any bean implementing `EmbeddingService` registered before this default will take precedence. This enables dropping in a real OpenAI or Ollama client without modifying existing code.

### Key Code

```java
public interface EmbeddingService {
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
}
```

## Known Issues

- **Placeholder only** — the default implementation is not semantically meaningful. It must be replaced with a real embedding API for production use.
- TODO in source: "Replace with real OpenAI / Ollama / local embedding API integration"

## Related

- [[services/document-chunker]] — produces text chunks that are fed into this service
- [[services/milvus-sync-service]] — consumes embeddings and inserts them into Milvus
- [[services/rag-search-service]] — requires query embeddings for similarity search
- [[services/rag-service]] — parent RAG orchestration service
