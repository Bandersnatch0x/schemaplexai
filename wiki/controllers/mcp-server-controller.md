---
title: McpServerController
type: controller
source: schemaplexai-integration/src/main/java/com/schemaplexai/integration/controller/McpServerController.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [controller, mcp, server, integration, tool-provider]
confidence: high
---

# McpServerController

> One-sentence summary: MCP (Model Context Protocol) server management controller for registering, configuring, and managing external tool providers that agents can invoke.

## Base Path

`/integration/mcp-servers` (routed via Gateway to `schemaplexai-integration` port 8088)

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Register a new MCP server |
| PUT | `/{id}` | Update an existing MCP server |
| DELETE | `/{id}` | Remove an MCP server |
| GET | `/{id}` | Get MCP server by ID |
| GET | `/` | List all registered MCP servers |

## Key Request/Response DTOs

### SfMcpServer (Request/Response Entity)
```java
@TableName("sf_mcp_server")
public class SfMcpServer extends BaseEntity {
    private String name;       // Server name / label
    private String endpoint;   // MCP server endpoint URL
    private String transport;  // Transport protocol (e.g., HTTP, SSE, stdio)
    private Integer status;    // Server status (active/inactive)
}
```

### Responses
- `Result<Long>` — created server ID
- `Result<Boolean>` — update/delete success
- `Result<SfMcpServer>` — single server retrieval
- `Result<List<SfMcpServer>>` — list all servers

## Dependencies

- `McpServerService mcpServerService` — handles MCP server persistence and health checks

## Notes

- Returns `Result<T>` wrapper (see [[architecture]])
- MCP servers expose tools that agents can discover and invoke at runtime
- `transport` indicates the communication protocol (HTTP REST, Server-Sent Events, or stdio)
- 404 returned as `ResultCode.NOT_FOUND` when server not found

## Backlinks

- Third-party integrations: [[controllers/IntegrationController]]
- Skill registry: [[controllers/SkillController]]
- Agent tool bindings: [[entities/agent]]
- See [[routes]] for routing
