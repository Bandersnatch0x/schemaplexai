---
title: ArtifactService
type: service
source: schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/ArtifactService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, ops, artifact, storage, delivery]
confidence: high
---

# ArtifactService

> One-sentence summary: Manages artifact lifecycle including upload, download, validation, type-based listing, and archival.

## Responsibilities

1. Upload new artifacts with validation
2. Download artifacts by ID
3. Validate artifact integrity
4. List artifacts filtered by type
5. Archive artifacts for long-term storage

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `uploadArtifact` | Upload a new artifact with validation | `artifact` (SfArtifact) | `SfArtifact` |
| `downloadArtifact` | Download an artifact by ID | `artifactId` (Long) | `SfArtifact` |
| `validateArtifact` | Validate an artifact's integrity | `artifactId` (Long) | `boolean` |
| `listArtifactsByType` | List artifacts filtered by type | `artifactType` (String) | `List<SfArtifact>` |
| `archiveArtifact` | Archive an artifact | `artifactId` (Long) | `SfArtifact` |

## Dependencies / Collaborators

- **Entity**: `SfArtifact` — artifact persistence via MyBatis-Plus `IService`

## Related

- [[services/delivery-service]] — delivers artifacts to recipients
- [[services/notification-service]] — may notify on artifact events
- [[entities/ops]] — ops domain entities
