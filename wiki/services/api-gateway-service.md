---
title: ApiGatewayService
type: service
source: schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/ApiGatewayService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, integration, api-gateway, routing, rate-limit]
confidence: high
---

# ApiGatewayService

> One-sentence summary: Manages dynamic API gateway routes and rate-limiting configurations for third-party service integrations.

## Responsibilities

1. Upsert (add or update) gateway routes mapping paths to target URLs
2. List all configured routes for a given gateway
3. Delete routes by route ID
4. Configure rate limiting (requests per second, burst capacity)
5. Perform health checks on gateway base URLs

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `upsertRoute` | Add or update a route for the given gateway configuration | `gatewayId` (Long), `routeId` (String), `path` (String), `targetUrl` (String), `priority` (Integer) | `void` |
| `listRoutes` | List all routes for the given gateway configuration | `gatewayId` (Long) | `List<Map<String, Object>>` |
| `deleteRoute` | Delete a route by its route ID | `gatewayId` (Long), `routeId` (String) | `void` |
| `updateRateLimit` | Update rate limit configuration for the gateway | `gatewayId` (Long), `requestsPerSecond` (Integer), `burstCapacity` (Integer) | `void` |
| `healthCheck` | Perform health check on the gateway base URL | `gatewayId` (Long) | `boolean` |

## Dependencies / Collaborators

- **Entity**: `SfApiGatewayConfig` — gateway configuration persistence via MyBatis-Plus `IService`
- **Related**: Gateway routing likely integrates with Spring Cloud Gateway for runtime route registration

## Related

- [[services/integration-service]] — manages broader integration configurations including webhooks
- [[services/mcp-server-service]] — another integration endpoint management service
- [[entities/integration]] — integration domain entities
