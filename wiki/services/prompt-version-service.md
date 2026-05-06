---
title: PromptVersionService
type: service
source: schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/PromptVersionService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, agent, prompt, version, config]
confidence: high
---

# PromptVersionService

> One-sentence summary: Interface for versioned prompt management associated with agent configurations, supporting creation, retrieval by label, and listing.

## Responsibilities

1. Create new prompt versions linked to a config and agent
2. Retrieve a specific version by its label
3. List all versions for a given configuration
4. Extend MyBatis-Plus `IService<SfPromptVersion>` for standard CRUD operations

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `createVersion(Long configId, Long agentId, String content, String label, String changeNote)` | Create a new prompt version | `configId` — parent config ID; `agentId` — owning agent ID; `content` — prompt text; `label` — version label; `changeNote` — change description | `SfPromptVersion` |
| `getByLabel(Long configId, String label)` | Retrieve a version by config and label | `configId` — parent config ID; `label` — version label | `Optional<SfPromptVersion>` |
| `listVersions(Long configId)` | List all versions for a config | `configId` — parent config ID | `List<SfPromptVersion>` |

## Key Code

```java
public interface PromptVersionService extends IService<SfPromptVersion> {

    SfPromptVersion createVersion(Long configId, Long agentId, String content,
                                   String label, String changeNote);

    Optional<SfPromptVersion> getByLabel(Long configId, String label);

    List<SfPromptVersion> listVersions(Long configId);
}
```

## Dependencies / Collaborators

| Component | Role |
|-----------|------|
| `SfPromptVersion` | Prompt version entity (configId, agentId, content, label, changeNote) |
| `IService<SfPromptVersion>` | MyBatis-Plus base CRUD interface |

## Backlinks

- Related: [[services/agent-config-service]] — manages the parent agent configurations
- Entity: [[entities/agent]]
