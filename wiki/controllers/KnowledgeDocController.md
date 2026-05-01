---
title: KnowledgeDocController
type: controller
source: schemaplexai-context/src/main/java/com/schemaplexai/context/controller/KnowledgeDocController.java
creation_date: 2026-05-01
tags: [controller, context, knowledge]
confidence: high
---

# KnowledgeDocController

> Manages knowledge document upload, vectorization, and lifecycle.

## Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /context/knowledge-docs | Upload and vectorize a new document | Required |
| PUT | /context/knowledge-docs/{id} | Update document metadata | Required |
| DELETE | /context/knowledge-docs/{id} | Delete a document | Required |
| GET | /context/knowledge-docs/{id} | Get document by ID | Required |
| GET | /context/knowledge-docs/page | Paginated list of documents | Required |

## Request/Response Types

### SfKnowledgeDoc

| Field | Type | Description |
|-------|------|-------------|
| `title` | String | Document title |
| `fileName` | String | Original file name |
| `fileUrl` | String | Storage URL |
| `fileSize` | Long | File size in bytes |
| `status` | String | Processing status |
| `docType` | String | Document type |

### Common Wrappers

- `Result<Long>` — Returns created document ID
- `Result<Boolean>` — Update/delete success flag
- `Result<SfKnowledgeDoc>` — Single document response
- `Result<PageResult<SfKnowledgeDoc>>` — Paginated list response
- `PageParam` — Pagination params (`current`, `size`)

## Service Dependencies

| Service | Role |
|---------|------|
| `KnowledgeDocService` | Handles document upload, vectorization, and standard CRUD |

## Notes

- `POST` triggers `uploadAndVectorize()` which persists the doc and initiates embedding pipeline.
- `GET /{id}` returns `404 NOT_FOUND` if the document does not exist.
