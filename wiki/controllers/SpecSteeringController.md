---
title: SpecSteeringController
type: controller
source: schemaplexai-spec/src/main/java/com/schemaplexai/spec/controller/SpecSteeringController.java
creation_date: 2026-05-01
tags: [controller, spec, steering]
confidence: high
---

# SpecSteeringController

> Standard CRUD for spec steering records that define direction, constraints, and acceptance criteria.

## Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /spec/steerings | Create a new steering record | Required |
| PUT | /spec/steerings/{id} | Update a steering record | Required |
| DELETE | /spec/steerings/{id} | Delete a steering record | Required |
| GET | /spec/steerings/{id} | Get steering record by ID | Required |
| GET | /spec/steerings/page | Paginated list of steering records | Required |

## Request/Response Types

### SfSpecSteering

| Field | Type | Description |
|-------|------|-------------|
| `specId` | Long | Associated spec ID |
| `direction` | String | Steering direction |
| `constraints` | String | Constraints text |
| `acceptanceCriteria` | String | Acceptance criteria |

### Common Wrappers

- `Result<Long>` — Returns created steering ID
- `Result<Boolean>` — Update/delete success flag
- `Result<SfSpecSteering>` — Single steering response
- `Result<PageResult<SfSpecSteering>>` — Paginated list response
- `PageParam` — Pagination params (`current`, `size`)

## Service Dependencies

| Service | Role |
|---------|------|
| `SpecSteeringService` | MyBatis-Plus `IService<SfSpecSteering>` providing standard CRUD and pagination |

## Notes

- Standard MyBatis-Plus CRUD controller pattern.
- `GET /{id}` returns `404 NOT_FOUND` if the steering record does not exist.
