---
title: ToolRegistry
type: service
source: com.schemaplexai.agent.engine.tool.registry.ToolRegistry
creation_date: 2026-05-06
tags: [agent-engine, tool, registry, discovery]
confidence: high
---

# ToolRegistry

Central tool registry for discovery, resolution, and parsing. Sits upstream of `ToolSandbox` — handles registration/discovery/parsing, while `ToolSandbox` handles sandboxed execution safety.

## Call Chain

```
ToolCallingStateHandler → ToolRegistry.resolve() → ToolSandbox.execute()
```

Each tool MUST pass `ToolSafetyGuard.check()` before execution.

## Responsibilities

| Concern | Implementation |
|---------|---------------|
| **Discovery** | Auto-discovers all `ToolAdapter` and `ToolCallParser` Spring beans at startup |
| **Registration** | Maps `toolName → ToolAdapter` in `ConcurrentHashMap` |
| **Resolution** | Looks up adapter by tool name; returns `null` for unregistered tools (whitelist violation) |
| **Parsing** | Routes LLM response to the correct `ToolCallParser` based on provider name |
| **Whitelist** | Filters parsed tool calls against registered adapters; unregistered tools are skipped with a warning |

## Key Methods

| Method | Purpose |
|--------|---------|
| `register(ToolAdapter)` | Register a tool adapter (overwrites existing with same name) |
| `resolve(String toolName)` | Look up adapter by name; returns `null` if not registered |
| `isRegistered(String)` | Check if a tool is in the whitelist |
| `parse(String content, LlmProvider)` | Parse tool calls from LLM response; routes to provider-specific parser |
| `getRegisteredToolNames()` | Unmodifiable list of registered tool names (for diagnostics) |

## Parser Routing

| Provider | Parser |
|----------|--------|
| OpenAI | `OpenAiToolCallParser` — parses `tool_calls` JSON format |
| Anthropic | `AnthropicToolCallParser` — parses `<tool_use>` XML format |

## Adapters

| Adapter | Tool Name | Description |
|---------|-----------|-------------|
| `FileReadAdapter` | `file_read` | File I/O with path traversal prevention |
| `HttpCallAdapter` | `http_call` | HTTP client with SSRF protection |

## Thread Safety

- `adapters` and `parsers` are `ConcurrentHashMap` — safe for concurrent reads
- Writes only happen at startup (bean construction) — no runtime registration mutation

## See Also

- [[services/agent-state-machine]] — state machine that invokes ToolRegistry
- [[services/agent-execution-lifecycle-service]] — pause/resume/cancel lifecycle
