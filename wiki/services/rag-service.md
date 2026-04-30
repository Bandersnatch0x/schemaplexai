---
title: RAG Service
type: service
source: schemaplexai-context/src/main/java/com/schemaplexai/context/service/RagService.java
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [service, rag, milvus, vector, embedding]
confidence: medium
---

# RAG Service

> One-sentence summary: Retrieval-Augmented Generation pipeline orchestrating document ingestion (MinIO + Tika), vector embedding, and Milvus semantic search.

## Components

- `RagService` / `RagServiceImpl` — orchestration layer
- `KnowledgeDocService` — document management
- `MilvusSyncService` — vector sync to Milvus
- `WorkspaceService` / `ContextService` — context/scope management

## Data Flow

1. Upload document → `sf_knowledge_doc` (PENDING)
2. Tika parsing → extract text
3. Chunking → text segments
4. Embedding → vector representation
5. Milvus insert → vector index
6. Query → Milvus ANN search → context injection

## Key Entities

- [[entities/context]] — workspace, context, context_item, knowledge_doc

## Notes

- Implementation details not yet fully explored
- MinIO for object storage, Milvus 2.3.5 for vectors
- See LangChain4j integration in `schemaplexai-agent-engine`

## Backlinks

- Context entities: [[entities/context]]
- Dependencies: [[dependencies]]
