---
title: SpecController
type: controller
source: schemaplexai-spec/src/main/java/com/schemaplexai/spec/controller/SpecController.java
creation_date: 2026-05-01
tags: [controller, spec]
confidence: high
---

# SpecController

> Standard CRUD for specification documents.

## Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /spec/specs | Create a new spec | Required |
| PUT | /spec/specs/{id} | Update a spec | Required |
| DELETE | /spec/specs/{id} | Delete a spec | Required |
| GET | /spec/specs/{id} | Get spec by ID | Required |
| GET | /spec/specs/page | Paginated list of specs | Required |

## Request/Response Types

### SfSpec

| Field | Type | Description |
|-------|------|-------------|
| `title` | String | Spec title |
| `type` | String | Spec type |
| `status` | String | Spec status (e.g., draft, published) |
| `content` | String | Spec content |

### Common Wrappers

- `Result<Long>` — Returns created spec ID
- `Result<Boolean>` — Update/delete success flag
- `Result<SfSpec>` — Single spec response
- `Result<PageResult<SfSpec>>` — Paginated list response
- `PageParam` — Pagination params (`current`, `size`)

## Service Dependencies

| Service | Role |
|---------|------|
| `SpecService` | MyBatis-Plus `IService<SfSpec>` providing standard CRUD and pagination |

## Notes

- Standard MyBatis-Plus CRUD controller pattern.
- `GET /{id}` returns `404 NOT_FOUND` if the spec does not exist.
