---
title: AuditEventController
type: controller
source: schemaplexai-quality/src/main/java/com/schemaplexai/quality/controller/AuditEventController.java
creation_date: 2026-05-01
tags: [quality, audit, event, controller, crud]
confidence: high
---

# AuditEventController

> CRUD controller for audit events (security and compliance logging).

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/quality/audit-events` | Create a new audit event |
| PUT | `/quality/audit-events/{id}` | Update an existing audit event by ID |
| DELETE | `/quality/audit-events/{id}` | Delete an audit event by ID |
| GET | `/quality/audit-events/{id}` | Get a single audit event by ID |
| GET | `/quality/audit-events` | List all audit events |

## DTO / Entity

- **Request/Response**: `SfAuditEvent` entity
  - `eventType` (String): Type of audit event
  - `resourceType` (String): Type of resource affected
  - `resourceId` (Long): ID of the affected resource
  - `action` (String): Action performed
  - `detailsJson` (String): JSON-serialized event details
  - `userId` (Long): ID of the user who performed the action
  - Inherits `BaseEntity`

## Service Dependencies

- `AuditEventService` — MyBatis-Plus `IService` for `SfAuditEvent`

## Notes

- Audit events are typically created automatically by interceptors or aspect-oriented logging, not via this controller.
- This controller provides manual audit event management and querying.
