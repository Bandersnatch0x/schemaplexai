---
title: AuditEventService
type: service
source: schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/AuditEventService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, quality, audit, compliance, logging]
confidence: high
---

# AuditEventService

> One-sentence summary: Provides CRUD operations for audit events via MyBatis-Plus, serving as the foundation for compliance and security audit trails.

## Responsibilities

1. Persist audit events to the database via MyBatis-Plus `IService`
2. Serve as the base service for audit event entity operations

## Key Methods

This service extends `IService<SfAuditEvent>` and inherits standard MyBatis-Plus CRUD operations:
- `save`, `saveBatch` — create audit events
- `getById`, `list`, `page` — query audit events
- `updateById`, `removeById` — modify audit events

## Dependencies / Collaborators

- **Entity**: `SfAuditEvent` — audit event persistence via MyBatis-Plus `IService`

## Related

- [[services/security-policy-service]] — security policies may generate audit events
- [[services/review-service]] — review actions may be logged as audit events
- [[services/quality-issue-service]] — quality issue lifecycle events may be audited
- [[entities/quality]] — quality domain entities
