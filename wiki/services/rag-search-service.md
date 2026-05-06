---
title: RagSearchService
type: service
source: schemaplexai-context/src/main/java/com/schemaplexai/context/service/RagSearchService.java
creation_date: 2026-05-06
update_date: 2026-05-06
tags: [service, rag, search, milvus, vector, similarity, context]
confidence: high
---

# RagSearchService

> One-sentence summary: Performs vector similarity search over knowledge documents in Milvus using COSINE similarity, scoped by tenant, and returns ranked knowledge chunks for RAG context injection.

## Responsibilities

1. Convert search queries into embedding vectors
2. Execute ANN (approximate nearest neighbor) search against Milvus collection
3. Apply tenant-scoped filter expressions for multi-tenant isolation
4. Map Milvus search results to `KnowledgeChunk` domain objects
5. Handle errors gracefully by returning empty results (no exceptions to caller)

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `RagSearchService` | `schemaplexai-context/src/main/java/com/schemaplexai/context/service/RagSearchService.java` | Interface: `search(String query, String tenantId, int topK)` |
| `RagSearchServiceImpl` | `schemaplexai-context/src/main/java/com/schemaplexai/context/service/impl/RagSearchServiceImpl.java` | MilvusClientV2 implementation |
| `KnowledgeChunk` | `schemaplexai-context/src/main/java/com/schemaplexai/context/entity/KnowledgeChunk.java` | Result DTO: docId, chunkIndex, content, score, tenantId |

### Search Flow

```
search(query, tenantId, topK)
  ├─ Validate query (null/blank → empty list)
  ├─ Generate query embedding (currently simulated)
  ├─ Build SearchReq with COSINE similarity
  ├─ Apply tenant filter: "tenant_id == 'xxx'" (if tenantId provided)
  ├─ Execute MilvusClientV2.search()
  └─ Map SearchResult → List<KnowledgeChunk>
```

### Tenant Scoping

```java
if (tenantId != null && !tenantId.isBlank()) {
    searchBuilder.filter("tenant_id == '" + tenantId + "'");
}
```

The filter expression is passed directly to Milvus as a string. This relies on the `tenant_id` field being indexed in the Milvus collection schema.

### Conditional Wiring

```java
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true", matchIfMissing = false)
public class RagSearchServiceImpl implements RagSearchService { ... }
```

When Milvus is disabled, no bean is registered and callers should handle `NoSuchBeanDefinitionException` or use an optional dependency pattern.

### Key Code

```java
public List<KnowledgeChunk> search(String query, String tenantId, int topK) {
    // ... embedding generation, Milvus search, result mapping
}
```

## Known Issues

- **Simulated query embedding** — `generateSimulatedEmbedding()` produces random vectors. In production this must call the real [[services/embedding-service]].
- **String-concatenated filter** — tenant filter uses simple string concatenation. This is safe for internal tenant IDs but should use parameterized expressions if user-supplied values are ever allowed.

## Related

- [[services/milvus-sync-service]] — inserts the vectors this service searches
- [[services/embedding-service]] — should be used for query embedding (currently simulated)
- [[services/rag-service]] — parent RAG orchestration service
- [[controllers/RagController]] — HTTP endpoint exposing search
- [[entities/context]] — knowledge document and chunk entities
