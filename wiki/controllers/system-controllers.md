---
title: System Controllers
type: controller
source: schemaplexai-system/src/main/java/com/schemaplexai/system/controller/
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [controller, system, auth, rbac]
confidence: medium
---

# System Controllers

> One-sentence summary: Authentication, tenant, user, role, permission, AI model, and system config management — all under `/system/**` and `/auth/**` routes.

## Controllers

| Controller | Path | Purpose |
|------------|------|---------|
| AuthController | `/auth/**` | Login, JWT issuance, token refresh |
| TenantController | `/system/tenant/**` | Tenant CRUD |
| UserController | `/system/user/**` | User CRUD |
| RoleController | `/system/role/**` | Role CRUD |
| PermissionController | `/system/permission/**` | Permission tree CRUD |
| AiModelController | `/system/ai-model/**` | AI model management |
| ModelProviderController | `/system/model-provider/**` | Provider registry |
| ConfigController | `/system/config/**` | System configuration |

## Notes

- All extend `BaseController`
- JWT validation happens at Gateway before reaching these controllers
- Tenant ID from `X-Tenant-Id` header auto-injected into SQL

## Backlinks

- RBAC entities: [[entities/user]]
- See [[routes]] for routing
