---
title: SpecService
type: service
source: schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, spec, document, version, collaboration]
confidence: high
---

# SpecService

> One-sentence summary: Core specification lifecycle service that manages creation, publishing, archiving, versioning, and template-based spec generation.

## Responsibilities

1. Publish specs by changing status and creating version snapshots
2. Archive specs to retire them from active use
3. Retrieve and compare spec versions
4. Create new specs from templates
5. Provide CRUD operations via `IService<SfSpec>`

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `publishSpec` | Publish a spec by changing its status to "published" and creating a version snapshot | `specId` — the spec ID | `SfSpecVersion` — the created version snapshot |
| `archiveSpec` | Archive a spec by changing its status to "archived" | `specId` — the spec ID | `boolean` — true if archived successfully |
| `getLatestVersion` | Get the latest version for a spec | `specId` — the spec ID | `Optional<SfSpecVersion>` — latest version if exists |
| `compareVersions` | Compare two versions of a spec by version strings | `specId` — the spec ID; `versionA` — first version string; `versionB` — second version string | `List<SfSpecVersion>` — pair of version contents |
| `createFromTemplate` | Create a new spec from an existing template | `templateId` — source template ID; `title` — new spec title; `type` — new spec type | `SfSpec` — the created spec |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `SpecService` | `schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecService.java` | Service interface extending `IService<SfSpec>` |
| `SfSpec` | `schemaplexai-spec/src/main/java/com/schemaplexai/spec/entity/SfSpec.java` | Entity: `title`, `type`, `status`, `content` |
| `SfSpecVersion` | `schemaplexai-spec/src/main/java/com/schemaplexai/spec/entity/SfSpecVersion.java` | Entity: `specId`, `version`, `content`, `changeLog` |

## Dependencies / Collaborators

- **MyBatis-Plus** — `IService<SfSpec>` provides CRUD, pagination, and query helpers
- **SpecVersionService** — delegates version creation and diff operations
- **SpecTemplateService** — provides templates for `createFromTemplate`
- **SfSpec entity** — stores specs in `sf_spec` table
- **SfSpecVersion entity** — stores version snapshots in `sf_spec_version` table

## Backlinks

- [[services/spec-review-service]] — submits reviews on published specs
- [[services/spec-version-service]] — manages version snapshots
- [[services/spec-template-service]] — provides templates for spec creation
- [[services/spec-steering-service]] — applies steering rules to spec content
- [[entities/spec]] — specification entity
