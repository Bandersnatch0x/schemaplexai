---
title: NotificationService
type: service
source: schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/NotificationService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, ops, notification, messaging, user]
confidence: high
---

# NotificationService

> One-sentence summary: User notification system supporting creation, read tracking, unread queries, and batch operations.

## Responsibilities

1. Send notifications to users with type, title, and content
2. Mark individual notifications as read
3. List unread notifications for a user
4. Batch mark multiple notifications as read

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `sendNotification` | Send a notification to a user | `userId` (Long), `type` (String), `title` (String), `content` (String) | `SfNotification` |
| `markAsRead` | Mark a single notification as read | `notificationId` (Long) | `SfNotification` |
| `listUnread` | List all unread notifications for a user | `userId` (Long) | `List<SfNotification>` |
| `batchMarkAsRead` | Mark multiple notifications as read in one operation | `notificationIds` (List<Long>) | `int` (count marked) |

## Dependencies / Collaborators

- **Entity**: `SfNotification` — notification persistence via MyBatis-Plus `IService`

## Related

- [[services/cost-service]] — may dispatch budget alert notifications
- [[services/delivery-service]] — may notify on delivery status changes
- [[services/quality-gate-service]] — may notify on quality gate failures
- [[entities/ops]] — ops domain entities
