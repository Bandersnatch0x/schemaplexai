---
title: AgentConfigService
type: service
source: schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/AgentConfigService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, agent, config, crud, tool-binding]
confidence: high
---

# AgentConfigService

> One-sentence summary: CRUD service for agent definitions, agent configurations, and agent-tool bindings within the agent-config module.

## Responsibilities

1. Manage agent definitions (SfAgent) — create, read, update, delete
2. Manage agent configurations (SfAgentConfig) — retrieve by agentId, save (upsert)
3. Manage agent-tool bindings (SfAgentToolBinding) — list by agentId, replace-all save
4. Enforce existence checks on read operations (throws BaseException if not found)

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `getAgent(Long id)` | Retrieve an agent by ID; throws if missing | `id` — agent primary key | `SfAgent` |
| `listAgents()` | List all agents | none | `List<SfAgent>` |
| `createAgent(SfAgent agent)` | Insert a new agent | `agent` — agent entity | void |
| `updateAgent(SfAgent agent)` | Update an existing agent | `agent` — agent entity with ID | void |
| `deleteAgent(Long id)` | Delete an agent by ID | `id` — agent primary key | void |
| `getAgentConfig(Long agentId)` | Retrieve configuration for a given agent | `agentId` — agent primary key | `SfAgentConfig` |
| `saveAgentConfig(SfAgentConfig config)` | Insert or update agent config | `config` — config entity | void |
| `listToolBindings(Long agentId)` | List tool bindings for an agent | `agentId` — agent primary key | `List<SfAgentToolBinding>` |
| `saveToolBindings(Long agentId, List<SfAgentToolBinding> bindings)` | Replace all tool bindings for an agent | `agentId`, `bindings` — new binding list | void |

## Key Code

```java
@Transactional(rollbackFor = Exception.class)
public void saveToolBindings(Long agentId, List<SfAgentToolBinding> bindings) {
    toolBindingMapper.delete(
        new LambdaQueryWrapper<SfAgentToolBinding>()
            .eq(SfAgentToolBinding::getAgentId, agentId)
    );
    if (bindings != null) {
        for (SfAgentToolBinding binding : bindings) {
            binding.setAgentId(agentId);
            toolBindingMapper.insert(binding);
        }
    }
}
```

## Dependencies / Collaborators

| Component | Role |
|-----------|------|
| `SfAgentMapper` | Agent definition persistence |
| `SfAgentConfigMapper` | Agent configuration persistence |
| `SfAgentToolBindingMapper` | Agent-tool binding persistence |

## Backlinks

- Entity: [[entities/agent]]
- Related: [[services/prompt-version-service]] — manages prompt versions for agent configs
- Related: [[services/shadow-config-service]] — manages shadow-mode configs for agents
- Related: [[services/tool-registry]] — tools referenced by bindings
