---
title: IntegrationController
type: controller
source: schemaplexai-integration/src/main/java/com/schemaplexai/integration/controller/IntegrationController.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [controller, integration, third-party, connector, github, gitlab, jenkins]
confidence: high
---

# IntegrationController

> One-sentence summary: Third-party integration controller managing external service connectors such as GitHub, GitLab, and Jenkins for CI/CD and source control workflows.

## Base Path

`/integration/integrations` (routed via Gateway to `schemaplexai-integration` port 8088)

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Create a new integration connector |
| PUT | `/{id}` | Update an existing integration |
| DELETE | `/{id}` | Delete an integration |
| GET | `/{id}` | Get integration by ID |
| GET | `/` | List all integrations |

## Key Request/Response DTOs

### SfIntegration (Request/Response Entity)
```java
@TableName("sf_integration")
public class SfIntegration extends BaseEntity {
    private String name;       // Integration name / label
    private String type;       // Integration type (e.g., GITHUB, GITLAB, JENKINS)
    private String configJson; // Connector configuration (JSON: tokens, URLs, webhooks)
    private Integer status;    // Integration status (active/inactive)
}
```

### Responses
- `Result<Long>` — created integration ID
- `Result<Boolean>` — update/delete success
- `Result<SfIntegration>` — single integration retrieval
- `Result<List<SfIntegration>>` — list all integrations

## Dependencies

- `IntegrationService integrationService` — handles integration persistence and connector lifecycle

## Notes

- Returns `Result<T>` wrapper (see [[architecture]])
- `configJson` stores connector-specific settings (API tokens, base URLs, webhook endpoints)
- 404 returned as `ResultCode.NOT_FOUND` when integration not found
- Related API gateway routing handled by [[controllers/ApiGatewayController]]

## Backlinks

- MCP server management: [[controllers/McpServerController]]
- Skill registry: [[controllers/SkillController]]
- API gateway: [[controllers/ApiGatewayController]]
- See [[routes]] for routing
