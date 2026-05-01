---
title: SpecTemplateController
type: controller
source: schemaplexai-spec/src/main/java/com/schemaplexai/spec/controller/SpecTemplateController.java
creation_date: 2026-05-01
tags: [controller, spec, template]
confidence: high
---

# SpecTemplateController

> Standard CRUD for spec templates used as starting points for new specifications.

## Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /spec/templates | Create a new template | Required |
| PUT | /spec/templates/{id} | Update a template | Required |
| DELETE | /spec/templates/{id} | Delete a template | Required |
| GET | /spec/templates/{id} | Get template by ID | Required |
| GET | /spec/templates/page | Paginated list of templates | Required |

## Request/Response Types

### SfSpecTemplate

| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Template name |
| `content` | String | Template content |
| `category` | String | Template category |

### Common Wrappers

- `Result<Long>` — Returns created template ID
- `Result<Boolean>` — Update/delete success flag
- `Result<SfSpecTemplate>` — Single template response
- `Result<PageResult<SfSpecTemplate>>` — Paginated list response
- `PageParam` — Pagination params (`current`, `size`)

## Service Dependencies

| Service | Role |
|---------|------|
| `SpecTemplateService` | MyBatis-Plus `IService<SfSpecTemplate>` providing standard CRUD and pagination |

## Notes

- Standard MyBatis-Plus CRUD controller pattern.
- `GET /{id}` returns `404 NOT_FOUND` if the template does not exist.
