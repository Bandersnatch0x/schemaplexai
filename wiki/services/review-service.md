---
title: ReviewService
type: service
source: schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/ReviewService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, quality, review, code-review, approval]
confidence: high
---

# ReviewService

> One-sentence summary: Manages code review and spec review records, tracking approval status and reviewer assignments.

## Responsibilities

1. Persist review records via MyBatis-Plus `IService`
2. Serve as the base service for review record entity operations

## Key Methods

This service extends `IService<SfReviewRecord>` and inherits standard MyBatis-Plus CRUD operations:
- `save`, `saveBatch` — create review records
- `getById`, `list`, `page` — query review records
- `updateById`, `removeById` — modify review records

## Dependencies / Collaborators

- **Entity**: `SfReviewRecord` — review record persistence via MyBatis-Plus `IService`

## Related

- [[services/quality-issue-service]] — reviews may identify quality issues
- [[services/quality-gate-service]] — review approval may be a quality gate criterion
- [[services/audit-event-service]] — review actions may be audited
- [[services/tool-approval-service]] — tool approvals follow similar review patterns
- [[entities/quality]] — quality domain entities
