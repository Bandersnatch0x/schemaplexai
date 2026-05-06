---
title: ShadowConfigService
type: service
source: schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/ShadowConfigService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, agent, shadow, config, crud]
confidence: high
---

# ShadowConfigService

> One-sentence summary: CRUD service for agent shadow-mode configurations, enabling per-agent shadow settings for safe preview and review workflows.

## Responsibilities

1. Retrieve shadow config by agent ID
2. List all shadow configs
3. Create, update, and delete shadow configurations
4. Enforce existence check on update (throws BaseException if not found)

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `getByAgentId(Long agentId)` | Retrieve shadow config for a specific agent | `agentId` — agent primary key | `SfAgentShadowConfig` |
| `listShadowConfigs()` | List all shadow configs | none | `List<SfAgentShadowConfig>` |
| `createShadowConfig(SfAgentShadowConfig config)` | Insert a new shadow config | `config` — shadow config entity | void |
| `updateShadowConfig(SfAgentShadowConfig config)` | Update an existing shadow config; throws if missing | `config` — shadow config entity with ID | void |
| `deleteShadowConfig(Long id)` | Delete a shadow config by ID | `id` — shadow config primary key | void |

## Key Code

```java
@Transactional(rollbackFor = Exception.class)
public void updateShadowConfig(SfAgentShadowConfig config) {
    if (shadowConfigMapper.selectById(config.getId()) == null) {
        throw new BaseException(ResultCode.NOT_FOUND);
    }
    shadowConfigMapper.updateById(config);
}
```

## Dependencies / Collaborators

| Component | Role |
|-----------|------|
| `SfAgentShadowConfigMapper` | Shadow configuration persistence |

## Backlinks

- Related: [[services/agent-config-service]] — manages parent agent definitions
- Related: [[services/agent-loop-shadow-review-service]] — consumes shadow config for review workflows
- Entity: [[entities/agent]]
