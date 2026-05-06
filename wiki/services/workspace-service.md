---
title: WorkspaceService
type: service
source: schemaplexai-context/src/main/java/com/schemaplexai/context/service/WorkspaceService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, workspace, tenant, context, crud]
confidence: high
---

# WorkspaceService

> One-sentence summary: Interface for workspace lifecycle management scoped by tenant, supporting creation, access validation, listing, and archival.

## Responsibilities

1. Create a default workspace for a tenant
2. Validate that the current tenant has access to a workspace
3. List workspaces filtered by tenant
4. Archive (soft-delete) a workspace
5. Extend MyBatis-Plus `IService<SfWorkspace>` for standard CRUD operations

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `createDefaultWorkspace(String tenantId)` | Create a default workspace for a tenant | `tenantId` — tenant identifier | `SfWorkspace` |
| `validateWorkspaceAccess(Long workspaceId)` | Verify current tenant can access the workspace | `workspaceId` — workspace primary key | void (throws on denial) |
| `listWorkspacesByTenant(String tenantId)` | List all workspaces for a tenant | `tenantId` — tenant identifier | `List<SfWorkspace>` |
| `archiveWorkspace(Long workspaceId)` | Soft-delete a workspace | `workspaceId` — workspace primary key | void |

## Key Code

```java
public interface WorkspaceService extends IService<SfWorkspace> {

    SfWorkspace createDefaultWorkspace(String tenantId);

    void validateWorkspaceAccess(Long workspaceId);

    List<SfWorkspace> listWorkspacesByTenant(String tenantId);

    void archiveWorkspace(Long workspaceId);
}
```

## Dependencies / Collaborators

| Component | Role |
|-----------|------|
| `SfWorkspace` | Workspace entity (tenant-scoped container) |
| `IService<SfWorkspace>` | MyBatis-Plus base CRUD interface |
| `TenantContextHolder` | Implicit tenant scoping via multi-tenant infrastructure |

## Backlinks

- Related: [[services/context-service]] — manages contexts within workspaces
- Related: [[services/context-snapshot-service]] — snapshots contexts in workspaces
- Related: [[services/knowledge-doc-service]] — documents live within workspace contexts
- Related: [[services/tenant-line-interceptor]] — auto-injects tenant filters
- Entity: [[entities/context]]
