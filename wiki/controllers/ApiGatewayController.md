---
title: ApiGatewayController
type: controller
source: schemaplexai-integration/src/main/java/com/schemaplexai/integration/controller/ApiGatewayController.java
creation_date: 2026-05-01
tags: [integration, api-gateway, controller, crud]
confidence: high
---

# ApiGatewayController

> CRUD controller for external API gateway configurations.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/integration/api-gateways` | Create a new API gateway config |
| PUT | `/integration/api-gateways/{id}` | Update an existing config by ID |
| DELETE | `/integration/api-gateways/{id}` | Delete a config by ID |
| GET | `/integration/api-gateways/{id}` | Get a single config by ID |
| GET | `/integration/api-gateways` | List all API gateway configs |

## DTO / Entity

- **Request/Response**: `SfApiGatewayConfig` entity
  - `name` (String): Config name
  - `baseUrl` (String): Base URL of the external API
  - `authType` (String): Authentication type
  - `authConfig` (String): Authentication configuration details
  - `rateLimit` (Integer): Rate limit (requests per time window)
  - Inherits `BaseEntity`

## Service Dependencies

- `ApiGatewayService` — MyBatis-Plus `IService` for `SfApiGatewayConfig`

## Notes

- Represents configurations for routing to external APIs through the integration layer.
- Distinct from the Spring Cloud Gateway service (`schemaplexai-gateway`) which handles internal routing.
