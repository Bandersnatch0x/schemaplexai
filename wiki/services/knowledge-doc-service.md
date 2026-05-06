---
title: KnowledgeDocService
type: service
source: schemaplexai-context/src/main/java/com/schemaplexai/context/service/KnowledgeDocService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, knowledge, document, vectorization, rag, context]
confidence: high
---

# KnowledgeDocService

> One-sentence summary: Interface for knowledge document management with integrated upload and vectorization pipeline for RAG ingestion.

## Responsibilities

1. Upload knowledge documents into the system
2. Trigger vectorization (chunking + embedding + vector store sync) for uploaded documents
3. Extend MyBatis-Plus `IService<SfKnowledgeDoc>` for standard CRUD operations

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `uploadAndVectorize(SfKnowledgeDoc doc)` | Upload a document and initiate the vectorization pipeline | `doc` — knowledge document entity | void |

## Key Code

```java
public interface KnowledgeDocService extends IService<SfKnowledgeDoc> {

    void uploadAndVectorize(SfKnowledgeDoc doc);
}
```

## Vectorization Pipeline

The `uploadAndVectorize` method typically orchestrates:
1. **Persist** — Save document metadata to database
2. **Chunk** — Split document content via [[services/document-chunker]]
3. **Embed** — Generate embeddings via [[services/embedding-service]]
4. **Sync** — Store vectors in Milvus via [[services/milvus-sync-service]]

## Dependencies / Collaborators

| Component | Role |
|-----------|------|
| `SfKnowledgeDoc` | Knowledge document entity |
| `IService<SfKnowledgeDoc>` | MyBatis-Plus base CRUD interface |
| `DocumentChunker` | Text chunking for RAG |
| `EmbeddingService` | Vector embedding generation |
| `MilvusSyncService` | Vector store synchronization |

## Backlinks

- Related: [[services/document-chunker]] — splits documents into chunks
- Related: [[services/embedding-service]] — generates vector embeddings
- Related: [[services/milvus-sync-service]] — persists embeddings to Milvus
- Related: [[services/rag-service]] — searches over vectorized documents
- Related: [[services/context-service]] — contexts contain knowledge documents
- Entity: [[entities/context]]
