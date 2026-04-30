---
title: User, Role, Permission (RBAC)
type: model
source: docker/postgres/init/01-init-schema.sql
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [entity, rbac, auth, tenant]
confidence: high
---

# User, Role, Permission (RBAC)

> One-sentence summary: Classic RBAC with many-to-many relationships — users have roles, roles have permissions; all scoped by tenant_id.

## Tables

### sf_user
- id (BIGSERIAL PK), tenant_id, username, password, email, phone, status
- UNIQUE(tenant_id, username)

### sf_role
- id (BIGSERIAL PK), tenant_id, name, code, status
- UNIQUE(tenant_id, code)

### sf_permission
- id (BIGSERIAL PK), tenant_id, name, code, type (MENU/BUTTON/API), parent_id, path, sort_order

### sf_user_role (junction)
- id, tenant_id, user_id, role_id
- UNIQUE(tenant_id, user_id, role_id)

### sf_role_permission (junction)
- id, tenant_id, role_id, permission_id
- UNIQUE(tenant_id, role_id, permission_id)

## Relationships

```
sf_user --(sf_user_role)--> sf_role --(sf_role_permission)--> sf_permission
```

## Backlinks

- Auth controllers in [[controllers/system-controllers]]
- See [[data-model]] for full schema
