---
title: SkillController
type: controller
source: schemaplexai-integration/src/main/java/com/schemaplexai/integration/controller/SkillController.java
creation_date: 2026-05-01
tags: [integration, skill, controller, crud]
confidence: high
---

# SkillController

> CRUD controller for agent skills (reusable tool definitions).

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/integration/skills` | Create a new skill |
| PUT | `/integration/skills/{id}` | Update an existing skill by ID |
| DELETE | `/integration/skills/{id}` | Delete a skill by ID |
| GET | `/integration/skills/{id}` | Get a single skill by ID |
| GET | `/integration/skills` | List all skills |

## DTO / Entity

- **Request/Response**: `SfSkill` entity
  - `name` (String): Skill name
  - `code` (String): Unique skill code
  - `description` (String): Skill description
  - `content` (String): Skill implementation content (prompt template or code)
  - `status` (Integer): Skill status
  - Inherits `BaseEntity`

## Service Dependencies

- `SkillService` — MyBatis-Plus `IService` for `SfSkill`

## Notes

- Skills are reusable capabilities that can be bound to agents via `sf_agent_tool_binding`.
- `content` typically contains prompt templates or function definitions.
