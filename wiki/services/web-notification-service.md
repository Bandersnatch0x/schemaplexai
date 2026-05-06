---
title: WebNotificationService
type: service
source: schemaplexai-web/src/main/java/com/schemaplexai/web/service/notification/NotificationService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, web, notification, user]
confidence: high
---

# WebNotificationService

> One-sentence summary: Web-layer notification service managing user notifications with paginated queries, read status tracking, and delivery via the web module.

## Responsibilities

1. Query user notifications with pagination and read status filtering
2. Mark individual notifications as read
3. Mark all notifications as read for a user
4. Send new notifications to users

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `pageQuery` | Paginated query of user notifications with read filter | `userId` — target user ID; `page` — page number; `size` — page size; `read` — filter by read status (nullable) | `IPage<NotificationVO>` — paginated notification list |
| `markAsRead` | Mark a single notification as read | `id` — notification ID; `userId` — user ID for ownership check | `boolean` — true if marked successfully |
| `markAllAsRead` | Mark all notifications as read for a user | `userId` — target user ID | `int` — number of notifications marked |
| `sendNotification` | Send a new notification | `notification` — `Notification` entity to deliver | `void` |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `NotificationService` | `schemaplexai-web/src/main/java/com/schemaplexai/web/service/notification/NotificationService.java` | Service interface |
| `Notification` | `schemaplexai-model/src/main/java/com/schemaplexai/model/entity/notification/Notification.java` | Entity: `userId`, `title`, `content`, `type`, `read` |
| `NotificationVO` | `schemaplexai-web/src/main/java/com/schemaplexai/web/vo/notification/NotificationVO.java` | View object for notification responses |

## Dependencies / Collaborators

- **MyBatis-Plus** — `IPage` for paginated results
- **Notification entity** — stores notifications in `sf_notification` table
- **UserService** — user context for notification ownership

## Backlinks

- [[services/user-service]] — notification recipients
- [[services/notification-service]] — ops module notification service (different implementation)
- [[entities/notification]] — notification entity
