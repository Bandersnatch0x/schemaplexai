---
title: SpecReviewController
type: controller
source: schemaplexai-spec/src/main/java/com/schemaplexai/spec/controller/SpecReviewController.java
creation_date: 2026-05-01
tags: [controller, spec, review]
confidence: high
---

# SpecReviewController

> Manages spec reviews with submit workflow for reviewer feedback.

## Endpoints

### Standard CRUD

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /spec/reviews | Create a new review record | Required |
| PUT | /spec/reviews/{id} | Update a review record | Required |
| DELETE | /spec/reviews/{id} | Delete a review record | Required |
| GET | /spec/reviews/{id} | Get review by ID | Required |
| GET | /spec/reviews/page | Paginated list of reviews | Required |

### Review Workflow

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /spec/reviews/submit | Submit a review decision | Required |

## Request/Response Types

### SfSpecReview

| Field | Type | Description |
|-------|------|-------------|
| `specId` | Long | Associated spec ID |
| `reviewerId` | Long | Reviewer user ID |
| `status` | String | Review status (e.g., approved, rejected, pending) |
| `comment` | String | Review comment |

### Submit Review

**Parameters:**
- `specId` — Spec ID being reviewed
- `reviewerId` — Reviewer user ID
- `status` — Review decision status
- `comment` (optional) — Review comment

**Response:** `Result<SfSpecReview>` — The submitted review record.

### Common Wrappers

- `Result<Long>` — Returns created review ID
- `Result<Boolean>` — Update/delete success flag
- `Result<SfSpecReview>` — Single review response
- `Result<PageResult<SfSpecReview>>` — Paginated list response
- `PageParam` — Pagination params (`current`, `size`)

## Service Dependencies

| Service | Role |
|---------|------|
| `SpecReviewService` | CRUD and review submission workflow |

## Notes

- `POST /submit` uses query parameters (not request body) for the review payload.
- Standard MyBatis-Plus CRUD controller pattern for the base operations.
- `GET /{id}` returns `404 NOT_FOUND` if the review does not exist.
