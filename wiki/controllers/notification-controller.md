---
title: NotificationController
type: controller
source: schemaplexai-web/src/main/java/com/schemaplexai/web/controller/notification/NotificationController.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [controller, notification, messaging, user, inbox]
confidence: high
---

# NotificationController

> One-sentence summary: Notification delivery controller providing paginated inbox queries, single notification read marking, and bulk "mark all as read" operations.

## Base Path

`/web/notification` (routed via Gateway to `schemaplexai-web` port 8082)

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/page` | Paginated query of current user's notifications with read filter |
| PUT | `/{id}/read` | Mark a single notification as read |
| PUT | `/read-all` | Mark all unread notifications as read for the current user |

## Key Request/Response DTOs

### NotificationVO (Response)
```java
@Data
public class NotificationVO {
    private Long id;
    private String title;         // Notification headline
    private String content;       // Notification body
    private String type;          // Category (e.g., SYSTEM, ALERT, TASK)
    private Boolean read;         // Read status
    private LocalDateTime createdAt; // Creation timestamp
}
```

### Query Parameters (GET /page)
- `page` — page number, default 1
- `size` — page size, default 20
- `read` — optional boolean filter for read/unread status

### Headers
- `X-User-Id` — current user identifier (required for all endpoints)

### Responses
- `Result<IPage<NotificationVO>>` — paginated notification list
- `Result<Boolean>` — single mark-read success
- `Result<Integer>` — count of notifications marked as read (bulk)

## Dependencies

- `NotificationService notificationService` — handles notification persistence, querying, and status updates

## Notes

- Returns `Result<T>` wrapper via `BaseController.success()` (see [[architecture]])
- All endpoints require `X-User-Id` header for user-scoped access
- Pagination uses MyBatis-Plus `IPage` interface

## Backlinks

- Notification entity: [[entities/notification]]
- User management: [[controllers/UserController]]
- See [[routes]] for routing
