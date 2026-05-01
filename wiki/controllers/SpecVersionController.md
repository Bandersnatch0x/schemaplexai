---
title: SpecVersionController
type: controller
source: schemaplexai-spec/src/main/java/com/schemaplexai/spec/controller/SpecVersionController.java
creation_date: 2026-05-01
tags: [controller, spec, version]
confidence: high
---

# SpecVersionController

> Manages spec versions with diff comparison and publishing capabilities.

## Endpoints

### Standard CRUD

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /spec/versions | Create a new version record | Required |
| PUT | /spec/versions/{id} | Update a version record | Required |
| DELETE | /spec/versions/{id} | Delete a version record | Required |
| GET | /spec/versions/{id} | Get version by ID | Required |
| GET | /spec/versions/page | Paginated list of versions | Required |

### Version Operations

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | /spec/versions/diff | Compare two spec versions | Required |
| POST | /spec/versions/publish | Publish a new version for a spec | Required |

## Request/Response Types

### SfSpecVersion

| Field | Type | Description |
|-------|------|-------------|
| `specId` | Long | Associated spec ID |
| `version` | String | Version string (e.g., "1.0.0") |
| `content` | String | Version content |
| `changeLog` | String | Change log description |

### SpecDiffResult

| Field | Type | Description |
|-------|------|-------------|
| `specId` | Long | Spec ID |
| `versionAId` | Long | First version ID |
| `versionBId` | Long | Second version ID |
| `hunks` | List<DiffHunk> | Diff hunks |

### DiffHunk

| Field | Type | Description |
|-------|------|-------------|
| `oldStart` | int | Start line in old version |
| `oldLines` | int | Lines in old version |
| `newStart` | int | Start line in new version |
| `newLines` | int | Lines in new version |
| `lines` | List<LineChange> | Line-level changes |

### DiffHunk.LineChange

| Field | Type | Description |
|-------|------|-------------|
| `type` | String | `ADDED`, `REMOVED`, or `UNCHANGED` |
| `content` | String | Line content |

### Common Wrappers

- `Result<Long>` — Returns created version ID
- `Result<Boolean>` — Update/delete success flag
- `Result<SfSpecVersion>` — Single version response
- `Result<PageResult<SfSpecVersion>>` — Paginated list response
- `Result<SpecDiffResult>` — Diff comparison result
- `PageParam` — Pagination params (`current`, `size`)

## Service Dependencies

| Service | Role |
|---------|------|
| `SpecVersionService` | CRUD, diff generation, and version publishing |

## Notes

- `GET /diff` requires `versionAId` and `versionBId` query parameters.
- `POST /publish` accepts `specId`, `version`, `content`, and optional `changeLog` as query parameters.
- Diff output uses a structured hunk format suitable for rendering inline diffs.
