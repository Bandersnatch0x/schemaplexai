# ADR-004: Unified Tool Call Parsing Strategy

## Status

Accepted

## Date

2026-05-04

## Context

Agent Engine needs to parse LLM tool call responses from multiple providers. OpenAI uses JSON `tool_calls` format while Anthropic uses XML `<tool_use>` format. The existing `ToolCallingStateHandler.parseToolCalls()` uses a heuristic approach (matching `"calling "` prefix) that does not support either format.

We need a parsing strategy that:
1. Supports OpenAI (JSON) and Anthropic (XML) out of the box
2. Is extensible to future LLM providers
3. Does not duplicate functionality that may already exist in LangChain4j 0.31.0

## Options Considered

### Option A: Unified Abstraction (Selected)

Create a `ToolCallParser` interface with provider-specific implementations. ToolRegistry routes to the correct parser based on `LlmProvider.getProviderName()`.

**Pros:**
- Clean separation of concerns — each parser handles one format
- Extensible — new providers require only a new parser implementation
- Provider-agnostic at the call site — ToolCallingStateHandler calls `toolRegistry.parse()` without knowing the format
- Testable — each parser can be unit-tested independently

**Cons:**
- May duplicate LangChain4j's built-in tool invocation parsing (if available in 0.31.0)
- Two implementations to maintain instead of one

### Option B: Independent Parsers Without Abstraction

Each StateHandler directly uses provider-specific parsing logic.

**Pros:**
- No interface overhead
- Direct coupling to provider

**Cons:**
- Code duplication across handlers
- Hard to extend — each new provider requires modifying multiple handlers
- Violates Open/Closed Principle
- Harder to test in isolation

## Decision

**Option A (Unified Abstraction)** is selected.

Implementation:
- `ToolCallParser` interface with `parse(String content, LlmProvider provider)` and `getProviderName()`
- `OpenAiToolCallParser` — parses `{"tool_calls": [{"function": {"name": "...", "arguments": "..."}}]}`
- `AnthropicToolCallParser` — parses `<tool_use><name>...</name><parameter name="...">...</parameter></tool_use>`
- `ToolRegistry` auto-discovers all `ToolCallParser` beans and routes by provider name

## Consequences

- Two parser implementations to maintain (manageable for the current 2 providers)
- Adding a new LLM provider requires only implementing `ToolCallParser` and registering as a Spring bean
- If LangChain4j 0.31.0 provides equivalent functionality, the parsers can be refactored to delegate to LangChain4j's API while keeping the ToolCallParser interface for abstraction
- All existing tests that mock `parseToolCalls()` need to be updated to mock `toolRegistry.parse()` instead
