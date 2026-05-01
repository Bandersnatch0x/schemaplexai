---
title: QualityIssueController
type: controller
source: schemaplexai-quality/src/main/java/com/schemaplexai/quality/controller/QualityIssueController.java
creation_date: 2026-05-01
tags: [quality, issue, controller, crud]
confidence: high
---

# QualityIssueController

> CRUD controller for quality issues discovered during quality gate evaluation.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/quality/issues` | Create a new quality issue |
| PUT | `/quality/issues/{id}` | Update an existing issue by ID |
| DELETE | `/quality/issues/{id}` | Delete an issue by ID |
| GET | `/quality/issues/{id}` | Get a single issue by ID |
| GET | `/quality/issues` | List all quality issues |

## DTO / Entity

- **Request/Response**: `SfQualityIssue` entity
  - `executionId` (Long): Associated agent execution
  - `issueType` (String): Rule name that generated the issue
  - `severity` (String): Issue severity level
  - `description` (String): Human-readable issue description
  - `status` (Integer): Issue status (0 = open)
  - Inherits `BaseEntity`

## Service Dependencies

- `QualityIssueService` — MyBatis-Plus `IService` for `SfQualityIssue`

## Notes

- Issues are typically created automatically by `QualityOrchestrator` during gate evaluation, not via this controller.
- This controller provides manual issue management capabilities.
