---
title: ContextController
type: controller
source: schemaplexai-context/src/main/java/com/schemaplexai/context/controller/ContextController.java
creation_date: 2026-05-01
tags: [controller, context]
confidence: high
---

# ContextController

> Standard CRUD for conversation contexts within a workspace.

## Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /context/contexts | Create a new context | Required |
| PUT | /context/contexts/{id} | Update a context | Required |
| DELETE | /context/contexts/{id} | Delete a context | Required |
| GET | /context/contexts/{id} | Get context by ID | Required |
| GET | /context/contexts/page | Paginated list of contexts | Required |

## Request/Response Types

### SfContext

| Field | Type | Description |
|-------|------|-------------|
| `workspaceId` | Long | Parent workspace ID |
| `name` | String | Context name |
| `type` | String | Context type |

### Common Wrappers

- `Result<Long>` — Returns created context ID
- `Result<Boolean>` — Update/delete success flag
- `Result<SfContext>` — Single context response
- `Result<PageResult<SfContext>>` — Paginated list response
- `PageParam` — Pagination params (`current`, `size`)

## Service Dependencies

| Service | Role |
|---------|------|
| `ContextService` | MyBatis-Plus `IService<SfContext>` providing standard CRUD and pagination |

## Notes

- Standard MyBatis-Plus CRUD controller pattern.
- `GET /{id}` returns `404 NOT_FOUND` if the context does not exist.
