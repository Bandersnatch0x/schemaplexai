---
title: NotificationController
type: controller
source: schemaplexai-ops/src/main/java/com/schemaplexai/ops/controller/NotificationController.java
creation_date: 2026-05-01
tags: [ops, notification, controller, crud]
confidence: high
---

# NotificationController

> CRUD controller for system notifications.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/ops/notifications` | Create a new notification |
| PUT | `/ops/notifications/{id}` | Update an existing notification by ID |
| DELETE | `/ops/notifications/{id}` | Delete a notification by ID |
| GET | `/ops/notifications/{id}` | Get a single notification by ID |
| GET | `/ops/notifications` | List all notifications |

## DTO / Entity

- **Request/Response**: `SfNotification` entity
  - `userId` (Long): Target user ID
  - `type` (String): Notification type
  - `title` (String): Notification title
  - `content` (String): Notification body
  - `status` (Integer): Read/unread status
  - Inherits `BaseEntity`

## Service Dependencies

- `NotificationService` — MyBatis-Plus `IService` for `SfNotification`

## Notes

- This is the ops-service notification controller (admin/management view).
- For the user-facing notification API, see the web layer `NotificationController` at `/web/notification`.
