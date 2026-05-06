---
title: MilvusSyncService
type: service
source: schemaplexai-context/src/main/java/com/schemaplexai/context/service/MilvusSyncService.java
creation_date: 2026-05-06
update_date: 2026-05-06
tags: [service, milvus, vector, sync, rag, context]
confidence: high
---

# MilvusSyncService

> One-sentence summary: Syncs knowledge documents to the Milvus vector store by extracting text, chunking, embedding, and batch-inserting vectors with automatic status tracking.

## Responsibilities

1. Validate document status before sync (accepts UPLOADED, PENDING, FAILED)
2. Extract text from knowledge documents (currently simulated; Tika integration planned)
3. Chunk text using [[services/document-chunker]] with default config
4. Generate embeddings via [[services/embedding-service]]
5. Batch insert vectors into Milvus collection
6. Update document status to SYNCED on success or FAILED on error

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `syncToMilvus(Long docId)` | Sync a document's chunked embeddings into Milvus | `docId` — knowledge document primary key | void |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `MilvusSyncService` | `schemaplexai-context/src/main/java/com/schemaplexai/context/service/MilvusSyncService.java` | Interface: `syncToMilvus(Long docId)` |
| `MilvusSyncServiceImpl` | `schemaplexai-context/src/main/java/com/schemaplexai/context/service/impl/MilvusSyncServiceImpl.java` | Active implementation when `milvus.enabled=true` |
| `NoOpMilvusSyncServiceImpl` | `schemaplexai-context/src/main/java/com/schemaplexai/context/service/impl/NoOpMilvusSyncServiceImpl.java` | No-op fallback when Milvus is disabled |

### Sync Flow

```
syncToMilvus(docId)
  ├─ Validate document exists
  ├─ Validate status ∈ {UPLOADED, PENDING, FAILED}
  ├─ Extract text (simulateExtractText)
  ├─ Chunk → List<TextChunk>
  ├─ Embed batch → List<float[]>
  ├─ Insert chunks + embeddings into Milvus
  └─ Update doc status → SYNCED (or FAILED on exception)
```

### Conditional Wiring

| Implementation | Condition | Behavior |
|----------------|-----------|----------|
| `MilvusSyncServiceImpl` | `@ConditionalOnProperty(name="milvus.enabled", havingValue="true")` | Full vector sync pipeline |
| `NoOpMilvusSyncServiceImpl` | `@ConditionalOnMissingBean(MilvusSyncService.class)` | Logs warning, skips sync |

### Milvus Insert Schema

Fields inserted per chunk: `id` (UUID), `doc_id`, `chunk_index`, `content`, `embedding` (List<Float>), `tenant_id`, `created_at`

### Key Code

```java
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true", matchIfMissing = false)
public class MilvusSyncServiceImpl implements MilvusSyncService { ... }
```

## Known Issues

- **Simulated text extraction** — `simulateExtractText()` generates placeholder paragraphs. Production should use Apache Tika or similar.
- **No actual retry logic** — failures set status to FAILED; manual re-sync required.

## Dependencies / Collaborators

| Component | Role |
|-----------|------|
| `SfKnowledgeDoc` / chunks | Source document and chunks |
| `MilvusClient` | Milvus vector database client |
| `EmbeddingService` | Embedding generation |
| `DocumentChunker` | Text chunking for RAG |

## Backlinks

- Related: [[services/knowledge-doc-service]] — triggers sync after upload
- Related: [[services/embedding-service]] — provides embeddings to sync
- Related: [[services/document-chunker]] — produces chunks that become Milvus records
- Related: [[services/rag-search-service]] — queries the synced vectors
- Related: [[services/rag-service]] — parent RAG orchestration
- Entity: [[entities/context]]
