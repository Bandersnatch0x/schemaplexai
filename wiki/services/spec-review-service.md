---
title: SpecReviewService
type: service
source: schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecReviewService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, spec, review, collaboration]
confidence: high
---

# SpecReviewService

> One-sentence summary: Manages spec review submissions, enabling reviewers to provide approval status and comments on specification documents.

## Responsibilities

1. Submit reviews for specifications with status and comment
2. Track reviewer feedback per spec via CRUD operations inherited from `IService<SfSpecReview>`
3. Support approval workflows by recording reviewer decisions

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `submitReview` | Submit a review for a spec with status and comment | `specId` — target spec ID; `reviewerId` — reviewer user ID; `status` — review status (e.g. "approved", "rejected", "pending"); `comment` — review comment | `SfSpecReview` — the created review record |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `SpecReviewService` | `schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecReviewService.java` | Service interface extending `IService<SfSpecReview>` |
| `SfSpecReview` | `schemaplexai-spec/src/main/java/com/schemaplexai/spec/entity/SfSpecReview.java` | Entity: `specId`, `reviewerId`, `status`, `comment` |

## Dependencies / Collaborators

- **MyBatis-Plus** — `IService<SfSpecReview>` provides CRUD, pagination, and query helpers
- **SfSpecReview entity** — stores review records in `sf_spec_review` table
- **SpecService** — spec lifecycle management (publish, archive)

## Backlinks

- [[services/spec-service]] — manages spec lifecycle that triggers reviews
- [[services/spec-version-service]] — version snapshots reviewed via this service
- [[entities/spec]] — specification entity
