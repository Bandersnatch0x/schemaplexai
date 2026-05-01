---
title: ArtifactController
type: controller
source: schemaplexai-ops/src/main/java/com/schemaplexai/ops/controller/ArtifactController.java
creation_date: 2026-05-01
tags: [ops, artifact, controller, crud]
confidence: high
---

# ArtifactController

> CRUD controller for build artifacts and deliverables.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/ops/artifacts` | Create a new artifact record |
| PUT | `/ops/artifacts/{id}` | Update an existing artifact by ID |
| DELETE | `/ops/artifacts/{id}` | Delete an artifact by ID |
| GET | `/ops/artifacts/{id}` | Get a single artifact by ID |
| GET | `/ops/artifacts` | List all artifacts |

## DTO / Entity

- **Request/Response**: `SfArtifact` entity
  - `name` (String): Artifact name
  - `version` (String): Artifact version
  - `fileUrl` (String): URL to the stored artifact file
  - `artifactType` (String): Type of artifact (e.g., JAR, Docker image, document)
  - `status` (Integer): Artifact status
  - Inherits `BaseEntity`

## Service Dependencies

- `ArtifactService` — MyBatis-Plus `IService` for `SfArtifact`

## Notes

- Artifacts are typically produced by CI/CD pipelines and stored in MinIO or similar object storage.
- `fileUrl` points to the actual artifact location.
