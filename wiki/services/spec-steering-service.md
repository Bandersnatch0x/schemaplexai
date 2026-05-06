---
title: SpecSteeringService
type: service
source: schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecSteeringService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, spec, steering, guidance, ai]
confidence: high
---

# SpecSteeringService

> One-sentence summary: Evaluates and applies steering rules to spec content, providing AI-guided direction and constraint validation for specification authoring.

## Responsibilities

1. Evaluate steering rules against input content and return match results
2. Apply steering configuration to content and return guided results
3. List active steering rules for a given spec
4. Validate steering configuration for completeness
5. Provide CRUD operations via `IService<SfSpecSteering>`

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `evaluateSteeringRules` | Evaluate steering rules against input content and return match results | `specId` — the spec ID; `content` — content to evaluate | `Map<String, Boolean>` — map of rule field to evaluation result |
| `applySteering` | Apply steering configuration to content and return guided result | `specId` — the spec ID; `content` — original content | `String` — guided content after applying steering rules |
| `listActiveSteerings` | List all active steerings for a spec | `specId` — the spec ID | `List<SfSpecSteering>` — list of active steering records |
| `validateSteeringConfig` | Validate steering configuration for completeness | `steeringId` — the steering ID | `boolean` — true if configuration is valid |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `SpecSteeringService` | `schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecSteeringService.java` | Service interface extending `IService<SfSpecSteering>` |
| `SfSpecSteering` | `schemaplexai-spec/src/main/java/com/schemaplexai/spec/entity/SfSpecSteering.java` | Entity: `specId`, `direction`, `constraints`, `acceptanceCriteria` |

## Dependencies / Collaborators

- **MyBatis-Plus** — `IService<SfSpecSteering>` provides CRUD, pagination, and query helpers
- **SfSpecSteering entity** — stores steering rules in `sf_spec_steering` table
- **SpecService** — the spec being steered

## Backlinks

- [[services/spec-service]] — the spec lifecycle service that steering augments
- [[services/spec-template-service]] — templates may include default steering rules
- [[entities/spec]] — specification entity
