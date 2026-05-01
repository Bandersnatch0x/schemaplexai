---
title: WorkspaceController
type: controller
source: schemaplexai-context/src/main/java/com/schemaplexai/context/controller/WorkspaceController.java
creation_date: 2026-05-01
tags: [controller, context, workspace]
confidence: high
---

# WorkspaceController

> Standard CRUD for workspaces that group contexts and knowledge documents.

## Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /context/workspaces | Create a new workspace | Required |
| PUT | /context/workspaces/{id} | Update a workspace | Required |
| DELETE | /context/workspaces/{id} | Delete a workspace | Required |
| GET | /context/workspaces/{id} | Get workspace by ID | Required |
| GET | /context/workspaces/page | Paginated list of workspaces | Required |

## Request/Response Types

### SfWorkspace

| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Workspace name |
| `description` | String | Workspace description |
| `parentId` | Long | Parent workspace ID (supports nesting) |

### Common Wrappers

- `Result<Long>` — Returns created workspace ID
- `Result<Boolean>` — Update/delete success flag
- `Result<SfWorkspace>` — Single workspace response
- `Result<PageResult<SfWorkspace>>` — Paginated list response
- `PageParam` — Pagination params (`current`, `size`)

## Service Dependencies

| Service | Role |
|---------|------|
| `WorkspaceService` | MyBatis-Plus `IService<SfWorkspace>` providing standard CRUD and pagination |

## Notes

- Supports hierarchical workspaces via `parentId`.
- Standard MyBatis-Plus CRUD controller pattern.
- `GET /{id}` returns `404 NOT_FOUND` if the workspace does not exist.
