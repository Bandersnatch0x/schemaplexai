# Phase 0.1: Schema Owner & Migration Strategy

## Decision

**Schema Owner: `schemaplexai-agent-engine`**

## Rationale

- `schemaplexai-agent-engine/pom.xml` already declares `flyway-core` dependency (line 60-61)
- 5 existing Flyway migrations already live in `schemaplexai-agent-engine/src/main/resources/db/migration/`
- No other module currently has Flyway dependency
- Agent-engine is the core execution service; it naturally boots first in local dev

## Status Quo

### Existing Flyway Migrations (agent-engine)

| Version | File | Content |
|---------|------|---------|
| V2026_05_01 | `V2026_05_01__add_skill_role_tables.sql` | Skill role tables |
| V2026_05_02 | `V2026_05_02__add_execution_skill_role_columns.sql` | Execution skill/role columns |
| V2026_05_03 | `V2026_05_03__extend_mcp_server.sql` | MCP server extension |
| V2026_05_08 | `V2026_05_08__add_skill_tier.sql` | Skill tier |
| V2026_05_08 | `V2026_05_08__add_snapshot_hash.sql` | Snapshot hash |

### Legacy Init Scripts (docker/postgres/init/)

| File | Purpose | Status |
|------|---------|--------|
| `01-init-schema.sql` | Core tables (tenant, user, role, permission) | **Baseline — do not modify** |
| `02-init-schema-agent.sql` | Agent tables | **Baseline — do not modify** |
| `03-init-schema-others.sql` | Other domain tables | **Baseline — do not modify** |
| `04-notification.sql` | Notification tables | **Baseline — do not modify** |
| `009_observability.sql` | Observability tables | **Baseline — do not modify** |

## Migration Boundary Rules

### Rule 1: Flyway manages NEW tables only

All tables introduced by this pivot (`sf_execution_event`, `sf_execution_outbox`, `sf_processed_event`, `sf_approval_ticket`) are managed by Flyway in `schemaplexai-agent-engine`.

### Rule 2: Legacy init scripts are FROZEN

The 5 docker init scripts remain as-is for fresh environment setup. Flyway `baseline` will be set to skip these pre-existing tables.

### Rule 3: Flowable tables are FLOWABLE-OWNED

All `act_*` tables are created and managed by Flowable engine. Flyway MUST NOT touch them.

### Rule 4: ALTER to existing tables via Flyway

Adding `version` and `last_event_seq` to `sf_agent_execution` goes through Flyway migration.

### Rule 5: Version numbering

Next migration: `V2026_05_09__execution_control_plane_tables.sql`

## Flyway Configuration (agent-engine application.yml)

No explicit Flyway config needed — Spring Boot auto-configuration works. Default behavior:
- `spring.flyway.enabled=true` (default when flyway-core present)
- `spring.flyway.locations=classpath:db/migration`
- `spring.flyway.baseline-on-migrate=true` (for existing databases)

## Future Evaluation

> **Note:** This is a tactical choice for the current pivot phase. If the service split stabilizes and agent-engine is no longer the natural "first boot" service, evaluate extracting a dedicated `schemaplexai-migration` module.

## Verification

- [ ] `mvn flyway:migrate -pl schemaplexai-agent-engine` succeeds
- [ ] Restart agent-engine does not re-run migrations
- [ ] Flowable tables (`act_re_procdef`, etc.) untouched by Flyway
