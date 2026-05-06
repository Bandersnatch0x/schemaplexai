---
title: QualityIssueService
type: service
source: schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/QualityIssueService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, quality, issue, defect, tracking]
confidence: high
---

# QualityIssueService

> One-sentence summary: Manages quality issue tracking for defects, drift, and anomalies detected during agent execution or code review.

## Responsibilities

1. Persist quality issues via MyBatis-Plus `IService`
2. Serve as the base service for quality issue entity operations

## Key Methods

This service extends `IService<SfQualityIssue>` and inherits standard MyBatis-Plus CRUD operations:
- `save`, `saveBatch` — create quality issues
- `getById`, `list`, `page` — query quality issues
- `updateById`, `removeById` — modify quality issues

## Dependencies / Collaborators

- **Entity**: `SfQualityIssue` — quality issue persistence via MyBatis-Plus `IService`

## Related

- [[services/quality-gate-service]] — quality gates may generate issues on failure
- [[services/review-service]] — reviews may identify quality issues
- [[services/audit-event-service]] — issue lifecycle changes may be audited
- [[entities/quality]] — quality domain entities
