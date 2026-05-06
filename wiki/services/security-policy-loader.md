---
title: SecurityPolicyLoader
type: service
source: com.schemaplexai.agent.engine.config.SecurityPolicyLoader
creation_date: 2026-05-06
tags: [agent-engine, security, tenant, cache]
confidence: high
---

# SecurityPolicyLoader

Loads and caches tenant environment security configuration. Provides deny-by-default security posture with graceful degradation.

## Architecture

```
Tenant Request → Caffeine Cache (L1, 5min TTL) → PostgreSQL (L2)
                                    ↓
                          Cache Miss / DB Error
                                    ↓
                          Default Policy (HIGH security)
```

## Caffeine Cache Configuration

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| `maximumSize` | 1000 | Prevents unbounded memory growth |
| `expireAfterWrite` | 5 minutes | Balance between freshness and DB load |
| Graceful fallback | Yes | Returns last-known or default config on DB error |

## Default Policy (Deny-by-Default)

When no explicit config exists for a tenant:

| Field | Default Value | Effect |
|-------|---------------|--------|
| `environment` | `"unknown"` | Triggers environment mismatch checks |
| `securityLevel` | `"HIGH"` | Most restrictive |
| `allowHttpCalls` | `false` | Blocks HttpCallAdapter |
| `allowFileRead` | `false` | Blocks FileReadAdapter |
| `allowIrreversibleOps` | `false` | Blocks destructive tools |
| `maxConcurrentToolCalls` | `1` | Serializes tool execution |

## Key Methods

| Method | Purpose |
|--------|---------|
| `load(String tenantId)` | Load config from cache or DB; returns default if unavailable |
| `refresh(String tenantId)` | Invalidate cache for a tenant; forces reload on next access |

## Schema

Backed by `sf_tenant_environment_config` (global table, not tenant-isolated):

```sql
sf_tenant_environment_config
  - tenant_id
  - environment
  - security_level
  - allow_http_calls
  - allow_file_read
  - allow_irreversible_ops
  - max_concurrent_tool_calls
```

## See Also

- [[services/tool-registry]] — uses security policy for tool execution gating
- [[services/agent-state-machine]] — transitions to GATE_BLOCKED when policy denies
