---
title: BaseEntity
type: model
source: schemaplexai-model/src/main/java/com/schemaplexai/model/entity/BaseEntity.java
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [entity, base-class, mybatis-plus]
confidence: high
---

# BaseEntity

> One-sentence summary: Abstract base class for all domain entities providing id, tenantId, audit timestamps, createdBy/updatedBy, and logic delete via MyBatis-Plus annotations.

## Fields

| Field | Type | Annotation | Description |
|-------|------|------------|-------------|
| id | Long | @TableId(type = IdType.ASSIGN_ID) | Snowflake-style distributed ID |
| tenantId | String | @TableField(fill = INSERT) | Tenant isolation key |
| createdAt | LocalDateTime | @TableField(fill = INSERT) | Auto-filled on insert |
| updatedAt | LocalDateTime | @TableField(fill = INSERT_UPDATE) | Auto-filled on insert/update |
| createdBy | Long | @TableField(fill = INSERT) | User ID of creator |
| updatedBy | Long | @TableField(fill = INSERT_UPDATE) | User ID of last updater |
| deleted | Integer | @TableLogic, @TableField(fill = INSERT) | 0 = active, 1 = deleted |

## Rules

- **Never** manually add these fields to entity classes
- **Never** use `@TableId` with other types on subclass entities
- Global tables (`sf_tenant`, `act_*`) bypass `tenant_id` injection via `TenantLineInterceptor` exclusion list

## Backlinks

- All entities extend this class — see [[data-model]] for the full entity list
- Tenant isolation in [[architecture]]
