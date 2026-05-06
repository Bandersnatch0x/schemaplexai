---
title: ContextService
type: service
source: schemaplexai-context/src/main/java/com/schemaplexai/context/service/ContextService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, context, workspace, ingestion, search]
confidence: high
---

# ContextService

> One-sentence summary: Interface for context lifecycle management within workspaces, supporting ingestion, search, refresh, and conversation linkage.

## Responsibilities

1. Ingest new contexts into a workspace
2. Search contexts by keyword or type within the current tenant
3. Refresh context metadata (re-index, update timestamps)
4. Retrieve a context by its associated conversation identifier
5. Extend MyBatis-Plus `IService<SfContext>` for standard CRUD operations

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `ingestContext(Long workspaceId, String name, String type)` | Ingest a new context into a workspace | `workspaceId` — target workspace ID; `name` — context name; `type` — context type | `SfContext` |
| `searchContext(String keyword, String type)` | Search contexts by name/type within tenant | `keyword` — search term; `type` — optional type filter | `List<SfContext>` |
| `refreshContext(Long contextId)` | Refresh metadata for a context | `contextId` — context primary key | void |
| `getContextByConversation(Long conversationId)` | Retrieve context linked to a conversation | `conversationId` — conversation ID | `SfContext` |

## Key Code

```java
public interface ContextService extends IService<SfContext> {

    SfContext ingestContext(Long workspaceId, String name, String type);

    List<SfContext> searchContext(String keyword, String type);

    void refreshContext(Long contextId);

    SfContext getContextByConversation(Long conversationId);
}
```

## Dependencies / Collaborators

| Component | Role |
|-----------|------|
| `SfContext` | Context entity (workspace-scoped knowledge unit) |
| `IService<SfContext>` | MyBatis-Plus base CRUD interface |

## Backlinks

- Related: [[services/workspace-service]] — manages workspaces that contain contexts
- Related: [[services/context-snapshot-service]] — snapshots context state
- Related: [[services/knowledge-doc-service]] — manages documents within contexts
- Related: [[services/rag-service]] — consumes contexts for retrieval-augmented generation
- Entity: [[entities/context]]
