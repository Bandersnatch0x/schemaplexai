---
title: McpServerService
type: service
source: schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/McpServerService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, integration, mcp, server, endpoint]
confidence: high
---

# McpServerService

> One-sentence summary: Manages Model Context Protocol (MCP) server configurations with health checking and endpoint validation.

## Responsibilities

1. Manage MCP server entity CRUD operations via MyBatis-Plus
2. Perform health checks on MCP server endpoints
3. Validate MCP server endpoint URLs

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `healthCheck` | Check if the MCP server endpoint is reachable | `serverId` (Long) | `boolean` |
| `validateEndpoint` | Validate the MCP server endpoint URL format and connectivity | `endpoint` (String) | `void` |

## Dependencies / Collaborators

- **Entity**: `SfMcpServer` — MCP server configuration persistence via MyBatis-Plus `IService`

## Related

- [[services/api-gateway-service]] — gateway route management
- [[services/integration-service]] — broader integration management
- [[services/skill-service]] — skill definitions that may invoke MCP servers
- [[services/tool-execution-service]] — executes tools that may use MCP endpoints
