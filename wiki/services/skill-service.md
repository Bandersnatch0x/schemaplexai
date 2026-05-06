---
title: SkillService
type: service
source: schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/SkillService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, integration, skill, versioning, tool]
confidence: high
---

# SkillService

> One-sentence summary: Manages reusable skill definitions with versioning, validation, and execution dispatch.

## Responsibilities

1. Create new versions of existing skills with change tracking
2. List all versions of a skill by its code
3. Validate skill syntax (frontmatter + body structure)
4. Trigger skill execution via the tool execution service

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `createVersion` | Create a new version of an existing skill | `skillId` (Long), `content` (String), `changeNote` (String) | `SfSkill` |
| `listVersions` | List all versions of a skill by its code | `skillCode` (String) | `List<SfSkill>` |
| `validateSkill` | Validate skill syntax (frontmatter + body structure) | `content` (String) | `boolean` |
| `executeSkill` | Trigger skill execution with parameters | `skillId` (Long), `parameters` (Map<String, Object>) | `String` |

## Dependencies / Collaborators

- **Entity**: `SfSkill` — skill definition persistence via MyBatis-Plus `IService`
- **ToolExecutionService** — dispatches skill execution to registered tool executors

## Related

- [[services/tool-execution-service]] — executes skills and other tools
- [[services/mcp-server-service]] — MCP servers may provide skill backends
- [[services/integration-service]] — broader integration management
