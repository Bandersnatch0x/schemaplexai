---
title: SpecTemplateService
type: service
source: schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecTemplateService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, spec, template, document]
confidence: high
---

# SpecTemplateService

> One-sentence summary: Manages specification templates, enabling template-based spec creation, category organization, and template cloning for standardized document authoring.

## Responsibilities

1. Apply templates to create or update specs with predefined structure
2. Retrieve default templates by category
3. List templates filtered by category
4. Clone existing templates for customization
5. Provide CRUD operations via `IService<SfSpecTemplate>`

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `applyTemplate` | Apply a template to create or update a spec | `templateId` — template ID; `specId` — target spec ID (nullable to create new); `title` — spec title; `type` — spec type | `SfSpec` — the resulting spec |
| `getDefaultTemplate` | Get the default template for a category | `category` — template category | `Optional<SfSpecTemplate>` — default template if exists |
| `listTemplatesByCategory` | List templates by category | `category` — template category | `List<SfSpecTemplate>` — list of matching templates |
| `cloneTemplate` | Clone an existing template | `templateId` — source template ID; `newName` — cloned template name | `SfSpecTemplate` — the cloned template |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `SpecTemplateService` | `schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecTemplateService.java` | Service interface extending `IService<SfSpecTemplate>` |
| `SfSpecTemplate` | `schemaplexai-spec/src/main/java/com/schemaplexai/spec/entity/SfSpecTemplate.java` | Entity: `name`, `content`, `category` |
| `SfSpec` | `schemaplexai-spec/src/main/java/com/schemaplexai/spec/entity/SfSpec.java` | Target entity created from templates |

## Dependencies / Collaborators

- **MyBatis-Plus** — `IService<SfSpecTemplate>` provides CRUD, pagination, and query helpers
- **SfSpecTemplate entity** — stores templates in `sf_spec_template` table
- **SfSpec entity** — target spec created when applying templates
- **SpecService** — consumes templates via `createFromTemplate`

## Backlinks

- [[services/spec-service]] — creates specs from templates
- [[services/spec-steering-service]] — templates may include steering configurations
- [[entities/spec]] — specification entity
