---
title: SpecVersionService
type: service
source: schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecVersionService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, spec, version, diff, document]
confidence: high
---

# SpecVersionService

> One-sentence summary: Manages specification version snapshots and provides diff capabilities for comparing different versions of a spec.

## Responsibilities

1. Create version snapshots of specs with change logs
2. Compute diffs between two spec versions
3. Provide CRUD operations via `IService<SfSpecVersion>`

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `diff` | Compute the difference between two spec versions | `versionAId` — first version ID; `versionBId` — second version ID | `SpecDiffResult` — structured diff result with hunks |
| `createVersion` | Create a new version snapshot for a spec | `specId` — the spec ID; `version` — version string; `content` — version content; `changeLog` — change description | `SfSpecVersion` — the created version record |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `SpecVersionService` | `schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecVersionService.java` | Service interface extending `IService<SfSpecVersion>` |
| `SfSpecVersion` | `schemaplexai-spec/src/main/java/com/schemaplexai/spec/entity/SfSpecVersion.java` | Entity: `specId`, `version`, `content`, `changeLog` |
| `SpecDiffResult` | `schemaplexai-spec/src/main/java/com/schemaplexai/spec/dto/SpecDiffResult.java` | DTO: `specId`, `versionAId`, `versionBId`, `hunks` |

## Dependencies / Collaborators

- **MyBatis-Plus** — `IService<SfSpecVersion>` provides CRUD, pagination, and query helpers
- **SfSpecVersion entity** — stores version snapshots in `sf_spec_version` table
- **SpecDiffResult DTO** — structured representation of version differences
- **SpecService** — delegates version creation during publish operations

## Backlinks

- [[services/spec-service]] — triggers version creation on publish
- [[services/spec-review-service]] — reviews are often tied to specific versions
- [[entities/spec]] — specification entity
