---
title: TenantLineInterceptor
type: service
source: schemaplexai-dao/src/main/java/com/schemaplexai/dao/config/TenantLineInterceptor.java
creation_date: 2026-05-01
update_date: 2026-05-01
tags: [service, tenant, multi-tenant, mybatis-plus, interceptor, security]
confidence: high
---

# TenantLineInterceptor

> One-sentence summary: MyBatis-Plus tenant line handler that auto-injects `tenant_id` filters into SQL queries, with global table exclusions for shared data.

## Responsibilities

1. **Extract tenant ID** — Read from `TenantContextHolder` (populated by Gateway from JWT)
2. **Inject SQL filter** — Append `tenant_id = ?` to all queries via JSQLParser
3. **Global table bypass** — Skip tenant filtering for shared tables (`sf_tenant`, `act_*`)

## Key Code

```java
@Component
public class TenantLineInterceptor implements TenantLineHandler {

    @Override
    public Expression getTenantId() {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            return new NullValue();  // No tenant filter applied
        }
        return new StringValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // Global tables — no tenant isolation
        return tableName.equals("sf_tenant") || tableName.startsWith("act_");
    }
}
```

## How It Works

```
Gateway (JWT) -> X-Tenant-Id header
       |
       v
TenantContextHolder.setTenantId()
       |
       v
MyBatis-Plus intercepts SQL
       |
       v
TenantLineInterceptor.getTenantId() -> "tenant_123"
       |
       v
JSQLParser injects: WHERE tenant_id = 'tenant_123'
```

## Global Tables (No Tenant Isolation)

| Table | Reason |
|-------|--------|
| `sf_tenant` | Tenant registry itself |
| `act_*` | Flowable BPMN runtime tables (managed by Flowable engine) |

## TenantContextHolder

Located in `schemaplexai-common`:
- `setTenantId(String)` — Called by Gateway filter or request interceptor
- `getTenantId()` — Called by `TenantLineInterceptor`
- Uses `ThreadLocal` for per-request isolation

## Data Isolation Model

SchemaPlexAI uses **shared database, shared schema** multi-tenancy:
- All tenant data in same PostgreSQL database
- `tenant_id` column on every entity (via `BaseEntity`)
- SQL-level filtering via MyBatis-Plus plugin
- No physical separation between tenants

## Dependencies

| Component | Role |
|-----------|------|
| `TenantContextHolder` | ThreadLocal tenant storage |
| `TenantLineHandler` | MyBatis-Plus extension interface |
| `JSQLParser` | SQL AST manipulation |

## Security Notes

- **Null tenant handling** — Returns `NullValue()` when no tenant; query runs unfiltered (intentional for system operations)
- **Global table whitelist** — Hardcoded; new global tables must be added here
- **No row-level security** — Relies on application-layer filtering; database RLS not used

## Backlinks

- Architecture: [[architecture]]
- Base entity: [[entities/base-entity]]
- Gateway filter: [[services/jwt-auth-filter]]
- Data model: [[data-model]]
