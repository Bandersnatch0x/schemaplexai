---
title: AgentConfigController
type: controller
source: schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/controller/AgentConfigController.java
creation_date: 2026-05-01
tags: [controller, agent-config]
confidence: high
---

# AgentConfigController

> Manages agent definitions, runtime configurations, tool bindings, and self-improvement shadow configs.

## Endpoints

### Agent Management

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | /agent-config/agents | List all agents | Required |
| GET | /agent-config/agents/{id} | Get agent by ID | Required |
| POST | /agent-config/agents | Create a new agent | Required |
| PUT | /agent-config/agents/{id} | Update an agent | Required |
| DELETE | /agent-config/agents/{id} | Delete an agent | Required |

### Agent Config

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | /agent-config/agents/{agentId}/config | Get runtime config for an agent | Required |
| POST | /agent-config/agents/{agentId}/config | Save runtime config for an agent | Required |

### Tool Bindings

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | /agent-config/agents/{agentId}/tools | List tool bindings for an agent | Required |
| POST | /agent-config/agents/{agentId}/tools | Save tool bindings for an agent | Required |

### Shadow Config (Self-Improvement)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | /agent-config/shadow-configs | List all shadow configs | Required |
| GET | /agent-config/shadow-configs/{agentId} | Get shadow config by agent ID | Required |
| POST | /agent-config/shadow-configs | Create shadow config | Required |
| PUT | /agent-config/shadow-configs/{id} | Update shadow config | Required |
| DELETE | /agent-config/shadow-configs/{id} | Delete shadow config | Required |

## Request/Response Types

### Entities

- `SfAgent` — Agent definition (`name`, `type`, `status`, `description`)
- `SfAgentConfig` — Runtime config (`agentId`, `maxRounds`, `maxTools`, `maxInputTokens`, `maxOutputTokens`, `systemPrompt`, `modelId`, `temperature`, `executionMode`)
- `SfAgentToolBinding` — Tool binding (`agentId`, `toolName`, `toolType`, `configJson`)
- `SfAgentShadowConfig` — Self-improvement config (`agentId`, `feedbackActionsJson`, `enabled`)

### Common Wrappers

- `Result<T>` — Standard API response wrapper
- `Result<List<T>>` — List responses
- `Result<Void>` — Empty success response

## Service Dependencies

| Service | Role |
|---------|------|
| `AgentConfigService` | CRUD for agents, agent configs, and tool bindings |
| `ShadowConfigService` | CRUD for shadow/self-improvement configs |

## Notes

- Tool bindings are replaced entirely on save (delete-all then insert).
- Agent config uses upsert logic (insert if `id` is null, otherwise update).
- Shadow config enables agent self-improvement via feedback action loops.
