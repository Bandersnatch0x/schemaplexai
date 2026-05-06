---
title: KnowledgeDocController
type: controller
source: schemaplexai-context/src/main/java/com/schemaplexai/context/controller/KnowledgeDocController.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [controller, knowledge, document, ingestion, rag, vectorization]
confidence: high
---

# KnowledgeDocController

> One-sentence summary: Knowledge document upload and ingestion pipeline controller managing document CRUD and automatic vectorization for RAG retrieval.

## Base Path

`/context/knowledge-docs` (routed via Gateway to `schemaplexai-context` port 8085)

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Upload a new document and trigger automatic vectorization |
| PUT | `/{id}` | Update document metadata |
| DELETE | `/{id}` | Delete a document |
| GET | `/{id}` | Get document by ID |
| GET | `/page` | Paginated list of documents |

## Key Request/Response DTOs

### SfKnowledgeDoc (Request/Response Entity)
```java
@TableName("sf_knowledge_doc")
public class SfKnowledgeDoc extends BaseEntity {
    private String title;      // Document title
    private String fileName;   // Original uploaded file name
    private String fileUrl;    // Storage URL (MinIO)
    private Long fileSize;     // File size in bytes
    private String status;     // Processing status (e.g., PENDING, INDEXED, FAILED)
    private String docType;    // Document type/category
}
```

### Page Response
- `Result<PageResult<SfKnowledgeDoc>>` — paginated document list

## Dependencies

- `KnowledgeDocService knowledgeDocService` — handles document persistence, file storage, and vectorization pipeline

## Notes

- Returns `Result<T>` wrapper (see [[architecture]])
- **Create** (`POST /`) triggers `uploadAndVectorize()` which stores the file and generates vector embeddings
- Uses `PageParam` for pagination (current page + size)

## Backlinks

- RAG retrieval: [[controllers/RagController]]
- Context workspace: [[controllers/WorkspaceController]]
- See [[routes]] for routing
