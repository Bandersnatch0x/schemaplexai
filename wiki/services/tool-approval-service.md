---
title: ToolApprovalService
type: service
source: schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/ToolApprovalService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, quality, tool, approval, governance]
confidence: high
---

# ToolApprovalService

> One-sentence summary: Manages tool approval amendments, tracking governance decisions for which tools are permitted in agent execution.

## Responsibilities

1. Persist tool approval amendments via MyBatis-Plus `IService`
2. Serve as the base service for tool approval entity operations

## Key Methods

This service extends `IService<SfToolApprovalAmendment>` and inherits standard MyBatis-Plus CRUD operations:
- `save`, `saveBatch` — create tool approval amendments
- `getById`, `list`, `page` — query tool approval amendments
- `updateById`, `removeById` — modify tool approval amendments

## Dependencies / Collaborators

- **Entity**: `SfToolApprovalAmendment` — tool approval persistence via MyBatis-Plus `IService`

## Related

- [[services/tool-execution-service]] — tool execution may check approvals before running
- [[services/security-policy-service]] — security policies may constrain tool approvals
- [[services/audit-event-service]] — approval decisions may be audited
- [[services/review-service]] — tool approvals may require review workflow
- [[entities/quality]] — quality domain entities
