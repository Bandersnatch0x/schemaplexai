---
title: Notification
type: entity
source: schemaplexai-model/src/main/java/com/schemaplexai/model/entity/notification/Notification.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [entity, notification, user, messaging]
confidence: high
---

# Notification

> One-sentence summary: User-facing notification entity storing titles, content, type, and read status for in-app messaging.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key (auto-assigned Snowflake ID) |
| tenantId | String | Multi-tenant isolation field |
| userId | Long | Recipient user ID (foreign key to user) |
| title | String | Notification title / headline |
| content | String | Notification body content |
| type | String | Notification category (e.g., SYSTEM, ALERT, TASK) |
| read | Boolean | Read status flag (false = unread) |
| createdAt | LocalDateTime | Creation timestamp |
| updatedAt | LocalDateTime | Last update timestamp |
| createdBy | Long | Creator user ID |
| updatedBy | Long | Last updater user ID |
| deleted | Integer | Soft-delete flag (0 = active, 1 = deleted) |

## Relationships

- **userId** → `sf_user.id` — recipient of the notification

## Backlinks

- Notification delivery: [[controllers/NotificationController]]
- User management: [[controllers/UserController]]
- See [[data-model]] for full schema
