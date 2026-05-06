---
title: AgentShadowConfigController
type: controller
source: schemaplexai-web/src/main/java/com/schemaplexai/web/controller/agent/AgentShadowConfigController.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [controller, agent, shadow-mode, self-improvement, config]
confidence: high
---

# AgentShadowConfigController

> One-sentence summary: Shadow mode configuration controller for managing agent self-improvement feedback loops, enabling agents to learn from execution outcomes.

## Base Path

`/agent-config/shadow-configs` (routed via Gateway to `schemaplexai-web` port 8082)

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Page list shadow configs |
| GET | `/{id}` | Get shadow config by ID |
| GET | `/agent/{agentId}` | Get shadow config by agent ID |
| POST | `/` | Create a new shadow config |
| PUT | `/{id}` | Update shadow config |
| PATCH | `/{id}/toggle` | Toggle enabled status (on/off) |
| DELETE | `/{id}` | Delete shadow config |

## Key Request/Response DTOs

### SfAgentShadowConfig (Request/Response Entity)
```java
@TableName("sf_agent_shadow_config")
public class SfAgentShadowConfig extends BaseEntity {
    private Long agentId;              // Associated agent ID
    private String feedbackActionsJson; // Self-improvement feedback actions (JSON)
    private Boolean enabled;           // Shadow mode enabled flag
}
```

### Page Response
- `Result<IPage<SfAgentShadowConfig>>` — paginated shadow config list

## Dependencies

- `AgentShadowConfigService agentShadowConfigService` — handles shadow config CRUD and toggle operations

## Notes

- Returns `Result<T>` wrapper via `BaseController.success()` (see [[architecture]])
- Shadow mode allows agents to run improvement loops based on execution feedback
- `feedbackActionsJson` stores structured feedback action definitions

## Backlinks

- Agent configuration: [[controllers/AgentConfigController]]
- Agent execution: [[controllers/agent-execution-controller]]
- Agent entities: [[entities/agent]]
- See [[routes]] for routing
