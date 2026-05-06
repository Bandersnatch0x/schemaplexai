---
title: RagController
type: controller
source: schemaplexai-context/src/main/java/com/schemaplexai/context/controller/RagController.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [controller, rag, retrieval, vector-search, context]
confidence: high
---

# RagController

> One-sentence summary: Retrieval-Augmented Generation (RAG) controller exposing vector search retrieval endpoints for semantic document querying.

## Base Path

`/context/rag` (routed via Gateway to `schemaplexai-context` port 8085)

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/retrieve` | Perform semantic retrieval given a query string and top-K limit |

## Key Request/Response DTOs

### RetrieveRequest
```java
@Data
public static class RetrieveRequest {
    private String query;   // Search query text
    private int topK = 5;   // Number of top results to return (default: 5)
}
```

### Response
- `Result<List<String>>` — list of retrieved document chunks or passages

## Dependencies

- `RagService ragService` — handles vector embedding generation and similarity search against the knowledge base

## Notes

- Returns `Result<T>` wrapper (see [[architecture]])
- Default `topK` is 5 if not specified in request
- Actual retrieval powered by Milvus vector store via the context service

## Backlinks

- Knowledge document management: [[controllers/KnowledgeDocController]]
- Context workspace: [[controllers/WorkspaceController]]
- See [[routes]] for routing
