---
title: QualityGateService
type: service
source: schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/QualityGateService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, quality, quality-gate, pipeline, gating]
confidence: high
---

# QualityGateService

> One-sentence summary: Manages quality gate configurations that enforce pass/fail criteria in CI/CD and agent execution pipelines.

## Responsibilities

1. Persist quality gate definitions via MyBatis-Plus `IService`
2. Serve as the base service for quality gate entity operations

## Key Methods

This service extends `IService<SfQualityGate>` and inherits standard MyBatis-Plus CRUD operations:
- `save`, `saveBatch` — create quality gates
- `getById`, `list`, `page` — query quality gates
- `updateById`, `removeById` — modify quality gates

## Dependencies / Collaborators

- **Entity**: `SfQualityGate` — quality gate persistence via MyBatis-Plus `IService`

## Related

- [[services/quality-issue-service]] — quality gates may raise issues on failure
- [[services/review-service]] — reviews may be gated by quality gate status
- [[services/evaluation-service]] — evaluation results may feed into quality gate decisions
- [[services/tool-approval-service]] — tool approvals may be subject to quality gates
- [[entities/quality]] — quality domain entities
