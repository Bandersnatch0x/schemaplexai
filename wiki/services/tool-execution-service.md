---
title: ToolExecutionService
type: service
source: schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/ToolExecutionService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, integration, tool, execution, registry]
confidence: high
---

# ToolExecutionService

> One-sentence summary: Dynamic tool execution registry that routes tool calls to registered `ToolExecutor` implementations by name.

## Responsibilities

1. Auto-discover and register all `ToolExecutor` beans at startup
2. Route tool execution requests to the appropriate executor by tool name
3. Parse JSON parameters into a parameter map
4. Handle execution failures with structured error codes
5. Gracefully fall back to raw string parameters when JSON parsing fails

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `executeTool` | Execute a tool by name with JSON parameters | `toolName` (String), `parametersJson` (String) | `String` (execution result) |
| `init` | Post-construct initialization: builds executor registry from all `ToolExecutor` beans | — | `void` |

## Dependencies / Collaborators

- **List<ToolExecutor>** — all discovered tool executor implementations (injected by Spring)
- **ObjectMapper** — JSON parameter parsing

## Key Code

```java
@PostConstruct
public void init() {
    this.executorRegistry = executors.stream()
            .collect(Collectors.toMap(ToolExecutor::getToolName, Function.identity()));
}

public String executeTool(String toolName, String parametersJson) {
    ToolExecutor executor = executorRegistry.get(toolName);
    if (executor == null) {
        throw new BaseException(ResultCode.INTEGRATION_NOT_FOUND, "Tool not found: " + toolName);
    }
    Map<String, Object> parameters = parseParameters(parametersJson);
    return executor.execute(parameters);
}
```

## Known Issues

- **No retry logic** — tool execution failures are thrown immediately without retry
- **Synchronous only** — all tool execution is blocking; async execution is not yet supported

## Related

- [[services/skill-service]] — dispatches skill execution through this service
- [[services/git-integration-service]] — Git operations may be exposed as tools
- [[services/jenkins-integration-service]] — Jenkins operations may be exposed as tools
- [[services/mcp-server-service]] — MCP tools may be registered as executors
