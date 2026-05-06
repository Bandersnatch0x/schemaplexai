---
title: IntegrationService
type: service
source: schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/IntegrationService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, integration, webhook, health-check]
confidence: high
---

# IntegrationService

> One-sentence summary: Manages third-party integration registrations, webhook configurations, and aggregate health monitoring.

## Responsibilities

1. Register webhooks for integrations with event type filtering
2. List all webhooks configured for a given integration
3. Delete webhooks by ID
4. Aggregate health status across all registered integrations

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `registerWebhook` | Register a webhook URL for a specific event type | `integrationId` (Long), `webhookUrl` (String), `eventType` (String) | `void` |
| `listWebhooks` | List all webhooks for the given integration | `integrationId` (Long) | `List<Map<String, Object>>` |
| `deleteWebhook` | Delete a webhook by its ID | `webhookId` (Long) | `void` |
| `aggregateHealthStatus` | Aggregate health status across all integrations | — | `Map<String, Object>` |

## Dependencies / Collaborators

- **Entity**: `SfIntegration` — integration configuration persistence via MyBatis-Plus `IService`

## Related

- [[services/git-integration-service]] — Git-specific integration operations
- [[services/jenkins-integration-service]] — Jenkins-specific integration operations
- [[services/api-gateway-service]] — gateway route management for integrations
- [[services/mcp-server-service]] — MCP server endpoint management
