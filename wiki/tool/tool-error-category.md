---
title: ToolErrorCategory
type: tool
source: schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/ToolErrorCategory.java
creation_date: 2026-05-01
update_date: 2026-05-01
tags: [tool, error-handling, taxonomy, agent-engine]
confidence: high
---

# ToolErrorCategory

> One-sentence summary: Enumeration that classifies tool execution failures into a fixed taxonomy, enabling programmatic retry decisions and security alerting.

## Categories

```java
public enum ToolErrorCategory {
    INVALID_ARGUMENTS    (false, false),  // Parameters malformed or missing required fields
    UNEXPECTED_ENVIRONMENT(false, true),  // Runtime environment issue (network, disk, etc.)
    PROVIDER_ERROR       (false, true),   // Upstream API failure (rate limit, downtime)
    USER_ABORTED         (false, false),  // User explicitly cancelled the operation
    TIMEOUT              (false, true),   // Operation exceeded time limit
    IRREVERSIBLE_OPERATION(true, false),  // Safety guard: destructive action blocked
    ENVIRONMENT_MISMATCH (true, false);   // Safety guard: cross-environment operation blocked
}
```

## Flags

| Category | `securityRelated` | `retryable` | Meaning |
|----------|-------------------|-------------|---------|
| `INVALID_ARGUMENTS` | No | No | Fix parameters, then retry manually |
| `UNEXPECTED_ENVIRONMENT` | No | Yes | Transient infra issue, safe to retry |
| `PROVIDER_ERROR` | No | Yes | External dependency issue, safe to retry with backoff |
| `USER_ABORTED` | No | No | User intent, do not retry |
| `TIMEOUT` | No | Yes | May succeed on retry if load decreases |
| `IRREVERSIBLE_OPERATION` | **Yes** | No | Security policy violation, requires human review |
| `ENVIRONMENT_MISMATCH` | **Yes** | No | Potential cross-env attack, requires human review |

## Usage in Decision Making

```java
if (result.errorCategory().securityRelated()) {
    // Alert security team, do NOT auto-retry
    alertSecurity(result);
} else if (result.errorCategory().retryable()) {
    // Schedule retry with exponential backoff
    retryScheduler.schedule(executionId, result);
} else {
    // Terminal failure, notify user
    stateMachine.transition(AgentExecutionState.FAILED, execution);
}
```

## Source

Taxonomy derived from [Cursor Agent Harness: Evaluation-First](https://yage.ai/share/cursor-agent-harness-evaluation-first-20260501.html) reliability classification:

| Cursor Concept | Mapped Category |
|----------------|-----------------|
| `INVALID_ARGUMENTS` | `INVALID_ARGUMENTS` |
| `UNEXPECTED_ENVIRONMENT` | `UNEXPECTED_ENVIRONMENT` |
| `PROVIDER_ERROR` | `PROVIDER_ERROR` |
| `USER_ABORTED` | `USER_ABORTED` |
| `TIMEOUT` | `TIMEOUT` |
| Safety guard block (irreversible) | `IRREVERSIBLE_OPERATION` |
| Safety guard block (env mismatch) | `ENVIRONMENT_MISMATCH` |

## Dependencies

None. Pure enum with no runtime dependencies.

## Backlinks

- Used by: [[tool/tool-safety-guard]], [[tool/tool-execution-recorder]], [[services/agent-state-machine]]
