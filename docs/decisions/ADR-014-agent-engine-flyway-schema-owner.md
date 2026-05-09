---
topic: agent-engine-flyway-schema-owner
stage: decision
version: v1.0
status: 已批准
supersedes: ""
---

# ADR-014: agent-engine as Flyway Schema Owner

> **日期**: 2026-05-09
> **决策人**: 架构评审委员会
> **状态**: 已批准

---

## 背景

SchemaPlexAI pivot to "Agent Execution Control Plane" introduces new PostgreSQL tables:
- `sf_execution_event` — immutable event stream
- `sf_execution_outbox` — transactional outbox for MQ publishing
- `sf_processed_event` — inbox deduplication table
- `sf_approval_ticket` — approval ticket aggregate
- ALTER on `sf_agent_execution` — add `version` and `last_event_seq` columns

A decision is needed on which module owns the Flyway migration responsibility for these tables.

Current state:
- `schemaplexai-agent-engine/pom.xml` already declares `flyway-core` dependency
- 5 existing Flyway migrations already live in `schemaplexai-agent-engine/src/main/resources/db/migration/`
- No other module has Flyway dependency
- 5 legacy Docker init scripts exist in `docker/postgres/init/` for baseline schema

## 决策

**`schemaplexai-agent-engine` is designated the Flyway schema owner** for all new tables introduced by the control plane pivot.

### 实现细节

1. **Migration boundary rules**
   - Flyway manages NEW tables only (`sf_execution_event`, `sf_execution_outbox`, `sf_processed_event`, `sf_approval_ticket`, ALTER on `sf_agent_execution`)
   - Legacy Docker init scripts are FROZEN — never modified, treated as baseline
   - Flowable tables (`act_*`) are Flowable-owned — Flyway MUST NOT touch them
   - Version numbering: `V2026_05_09__execution_control_plane_tables.sql` (done)

2. **Flyway configuration**
   - `spring.flyway.baseline-on-migrate=true` for existing databases
   - Default locations: `classpath:db/migration`
   - No explicit configuration needed — Spring Boot auto-configuration handles it

3. **Future evolution**
   - This is a tactical choice for the current pivot phase
   - If service split stabilizes and agent-engine is no longer the "first boot" service, evaluate extracting a dedicated `schemaplexai-migration` module

## 理由

- **Already configured**: `flyway-core` dependency and migration directory already exist in agent-engine; zero configuration cost
- **Boot order**: agent-engine naturally boots first in local dev as the core execution service
- **Ownership clarity**: execution control plane tables are conceptually owned by agent-engine (state machine, event stream, outbox)
- **Simplicity**: single migration owner avoids cross-module migration ordering conflicts

## 影响

- **对现有代码的影响**
  - New migration file: `V2026_05_09__execution_control_plane_tables.sql` (already created)
  - No changes to existing migrations or init scripts
  - Baseline flag ensures existing databases are not re-migrated

- **对开发流程的影响**
  - All new table DDL goes through Flyway migrations in agent-engine
  - PR reviews must verify migration version numbering is sequential
  - `mvn flyway:migrate -pl schemaplexai-agent-engine` is the canonical migration command

- **对运维的影响**
  - Fresh environment: Docker init scripts create baseline, then Flyway applies migrations on first boot
  - Existing environment: Flyway baselines existing tables, applies new migrations
  - Rollback: Flyway undo migrations not used; forward-only migration strategy

## 替代方案

| 方案 | 优点 | 缺点 | 结果 |
|------|------|------|------|
| Dedicated migration module | Clean separation, no boot-order dependency | Premature extraction for 4 tables, adds module complexity | 拒绝 |
| SQL init scripts only | No Flyway dependency | No versioning, no repeatability, rollback impossible | 拒绝 |
| **agent-engine as owner (本方案)** | Zero config, existing infra, clear ownership | Couples migrations to agent-engine lifecycle | **采纳** |
| Each module owns its tables | Domain-aligned ownership | Migration ordering nightmare, Flyway dependency sprawl | 拒绝 |

## 相关文档

- `.claude/outputs/phase-0/schema-ownership.md`
- `wiki/architecture.md`
- `docker/postgres/init/` (baseline scripts)