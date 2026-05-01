---
title: McpServerController
type: controller
source: schemaplexai-integration/src/main/java/com/schemaplexai/integration/controller/McpServerController.java
creation_date: 2026-05-01
tags: [integration, mcp, server, controller, crud]
confidence: high
---

# McpServerController

> CRUD controller for MCP (Model Context Protocol) server registrations.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/integration/mcp-servers` | Register a new MCP server |
| PUT | `/integration/mcp-servers/{id}` | Update an existing MCP server by ID |
| DELETE | `/integration/mcp-servers/{id}` | Unregister an MCP server by ID |
| GET | `/integration/mcp-servers/{id}` | Get a single MCP server by ID |
| GET | `/integration/mcp-servers` | List all registered MCP servers |

## DTO / Entity

- **Request/Response**: `SfMcpServer` entity
  - `name` (String): Server name
  - `endpoint` (String): MCP server endpoint URL
  - `transport` (String): Transport protocol (e.g., HTTP, SSE, stdio)
  - `status` (Integer): Server status
  - Inherits `BaseEntity`

## Service Dependencies

- `McpServerService` — MyBatis-Plus `IService` for `SfMcpServer`

## Notes

- MCP servers provide tools and resources to agents via the Model Context Protocol.
- Registered servers are discovered and invoked by the agent engine at runtime.
