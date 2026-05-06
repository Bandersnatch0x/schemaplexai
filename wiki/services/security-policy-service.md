---
title: SecurityPolicyService
type: service
source: schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/SecurityPolicyService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, quality, security, policy, compliance]
confidence: high
---

# SecurityPolicyService

> One-sentence summary: Manages security policy definitions that govern access controls, data handling, and compliance rules.

## Responsibilities

1. Persist security policies via MyBatis-Plus `IService`
2. Serve as the base service for security policy entity operations

## Key Methods

This service extends `IService<SfSecurityPolicy>` and inherits standard MyBatis-Plus CRUD operations:
- `save`, `saveBatch` — create security policies
- `getById`, `list`, `page` — query security policies
- `updateById`, `removeById` — modify security policies

## Dependencies / Collaborators

- **Entity**: `SfSecurityPolicy` — security policy persistence via MyBatis-Plus `IService`

## Related

- [[services/security-policy-loader]] — loads and applies security policies at runtime
- [[services/audit-event-service]] — policy violations may generate audit events
- [[services/tool-approval-service]] — tool usage may be constrained by security policies
- [[entities/quality]] — quality domain entities
