---
title: IntegrationController
type: controller
source: schemaplexai-integration/src/main/java/com/schemaplexai/integration/controller/IntegrationController.java
creation_date: 2026-05-01
tags: [integration, controller, crud]
confidence: high
---

# IntegrationController

> CRUD controller for third-party system integrations (GitHub, GitLab, Jenkins, etc.).

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/integration/integrations` | Create a new integration |
| PUT | `/integration/integrations/{id}` | Update an existing integration by ID |
| DELETE | `/integration/integrations/{id}` | Delete an integration by ID |
| GET | `/integration/integrations/{id}` | Get a single integration by ID |
| GET | `/integration/integrations` | List all integrations |

## DTO / Entity

- **Request/Response**: `SfIntegration` entity
  - `name` (String): Integration name
  - `type` (String): Integration type (e.g., GITHUB, GITLAB, JENKINS)
  - `configJson` (String): JSON-serialized connection configuration
  - `status` (Integer): Integration status
  - Inherits `BaseEntity`

## Service Dependencies

- `IntegrationService` — MyBatis-Plus `IService` for `SfIntegration`

## Notes

- `configJson` stores connection parameters such as API endpoints, tokens, and webhooks.
- Integrations are consumed by the `schemaplexai-integration` service for CI/CD and external tool connectivity.
