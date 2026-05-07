<!-- AUTO-GENERATED: manual-fix at 2026-05-07T19:00:17Z -->
---
title: SfAgentShadowConfig
type: entity
source: schemaplexai-model/src/main/java/com/schemaplexai/model/entity/agent/SfAgentShadowConfig.java
creation_date: 2026-05-08
update_date: 2026-05-08
tags: [entity, agent, shadow-mode, self-improvement, config]
confidence: high
---

# SfAgentShadowConfig (sf_agent_shadow_config)

> Self-improvement configuration for agents — defines feedback actions and shadow execution behavior.

## Schema

```sql
CREATE TABLE sf_agent_shadow_config (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    agent_id        BIGINT NOT NULL,
    feedback_actions_json TEXT,
    enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);
```

## Fields

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | BIGSERIAL | No | auto | Primary key |
| tenant_id | BIGINT | No | — | Tenant identifier |
| agent_id | BIGINT | No | — | Reference to `sf_agent.id` |
| feedback_actions_json | TEXT | Yes | — | JSON array of feedback action definitions |
| enabled | BOOLEAN | No | FALSE | Whether shadow mode is active |
| created_at | TIMESTAMP | No | CURRENT_TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | No | CURRENT_TIMESTAMP | Last update time |
| deleted | INT | No | 0 | Soft-delete flag |

## Java Entity

- `SfAgentShadowConfig` in `schemaplexai-model`
  - `agentId` (Long)
  - `feedbackActionsJson` (String)
  - `enabled` (Boolean)

## Relationships

- **Many-to-one** with `sf_agent` via `agent_id`

## Notes

- Shadow mode allows an agent to run alternative strategies in the background and compare results.
- `feedback_actions_json` defines how the agent should incorporate human or automated feedback.
- Managed by `AgentShadowConfigController` at `/agent-config/shadow-configs`.
