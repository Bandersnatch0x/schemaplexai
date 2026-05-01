---
title: ReviewController
type: controller
source: schemaplexai-quality/src/main/java/com/schemaplexai/quality/controller/ReviewController.java
creation_date: 2026-05-01
tags: [quality, review, controller, crud]
confidence: high
---

# ReviewController

> CRUD controller for review records (human review of specs, code, or other resources).

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/quality/reviews` | Create a new review record |
| PUT | `/quality/reviews/{id}` | Update an existing review by ID |
| DELETE | `/quality/reviews/{id}` | Delete a review by ID |
| GET | `/quality/reviews/{id}` | Get a single review by ID |
| GET | `/quality/reviews` | List all review records |

## DTO / Entity

- **Request/Response**: `SfReviewRecord` entity
  - `resourceType` (String): Type of resource being reviewed
  - `resourceId` (Long): ID of the resource being reviewed
  - `reviewerId` (Long): ID of the reviewer user
  - `status` (Integer): Review status
  - `comment` (String): Review comment text
  - Inherits `BaseEntity`

## Service Dependencies

- `ReviewService` — MyBatis-Plus `IService` for `SfReviewRecord`

## Notes

- Generic review system that can attach to any resource type (spec, code, artifact, etc.).
- Distinct from `SpecReviewController` which is spec-domain specific.
