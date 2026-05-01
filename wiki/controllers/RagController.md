---
title: RagController
type: controller
source: schemaplexai-context/src/main/java/com/schemaplexai/context/controller/RagController.java
creation_date: 2026-05-01
tags: [controller, context, rag]
confidence: high
---

# RagController

> Exposes RAG (Retrieval-Augmented Generation) query endpoints for vector-based document retrieval.

## Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /context/rag/retrieve | Retrieve top-K relevant document chunks for a query | Required |

## Request/Response Types

### RetrieveRequest (inner class)

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `query` | String | — | User query text |
| `topK` | int | 5 | Number of top results to return |

### Response

`Result<List<String>>` — List of retrieved document chunk texts.

## Service Dependencies

| Service | Role |
|---------|------|
| `RagService` | Executes vector similarity search and returns matching chunks |

## Notes

- This is a thin controller delegating directly to `RagService`.
- The actual embedding and vector store interaction happens in the service layer.
