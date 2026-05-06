---
title: RetryingStateHandler
type: service
source: com.schemaplexai.agent.engine.state.RetryingStateHandler
creation_date: 2026-05-06
tags: [agent-engine, state-handler, retry, circuit-breaker]
confidence: high
---

# RetryingStateHandler

Handles the `RETRYING` state with exponential backoff and circuit breaker pattern.

## State Transition Path

```
TOOL_CALLING → (failure) → RETRYING → (backoff) → TOOL_CALLING
                              ↓
                        (max retries exceeded)
                              ↓
                           FAILED
```

## Retry Strategy

| Parameter | Default | Description |
|-----------|---------|-------------|
| `agent.retry.enabled` | `true` | Master switch for retry behavior |
| `agent.retry.max-retries` | `3` | Maximum retry attempts per execution |
| `agent.retry.base-delay-ms` | `100` | Initial delay before first retry |
| `agent.retry.max-delay-ms` | `30000` | Cap on exponential backoff |

### Exponential Backoff Formula

```
delay = min(baseDelayMs * 2^(retryCount - 1), maxDelayMs)
```

| Retry # | Delay (default config) |
|---------|----------------------|
| 1 | 100ms |
| 2 | 200ms |
| 3 | 400ms |
| 4 | 800ms |
| ... | capped at 30s |

## Circuit Breaker

- **Threshold**: 3 consecutive failures open the circuit
- **Effect**: Execution transitions to `FAILED` immediately
- **State**: Per-execution (not global) — prevents one execution from monopolizing retries

## Retryable Categories

Only `ToolErrorCategory.isRetryable() == true` errors trigger retry:

| Category | Retryable | Notes |
|----------|-----------|-------|
| `TIMEOUT` | Yes | Transient network issue |
| `RATE_LIMITED` | Yes | Backpressure from external API |
| `INTERNAL_ERROR` | Yes | External service hiccup |
| `PERMISSION_DENIED` | No | Security issue, retry won't help |
| `INVALID_ARGUMENT` | No | Code bug, retry won't help |
| `RESOURCE_EXHAUSTED` | No | Resource limit, retry won't help |
| `IRREVERSIBLE_OPERATION` | No | Safety block, retry won't help |
| `UNEXPECTED_ENVIRONMENT` | No | Config mismatch |

## Efficiency Note

Retries only replay the **failed tool call**, not the full conversation history. Token cost: ~200-500 tokens per retry vs ~5,000-20,000 for full history replay.

## See Also

- [[services/agent-state-machine]] — manages state transitions including RETRYING
- [[services/tool-registry]] — executes the retried tool call
