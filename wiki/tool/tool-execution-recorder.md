---
title: ToolExecutionRecorder
type: tool
source: schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/ToolExecutionRecorder.java
creation_date: 2026-05-01
update_date: 2026-05-01
tags: [tool, audit, observability, agent-engine]
confidence: high
---

# ToolExecutionRecorder

> One-sentence summary: Audit logger that persists every tool invocation outcome to durable storage for compliance, debugging, and evaluation metrics.

## Responsibilities

1. **Persist tool results** — Save tool name, output, latency, token count per execution
2. **Fail on audit loss** — Throw `ToolExecutionAuditException` if write fails (security events cannot be silently dropped)
3. **Support evaluation metrics** — Provide data for keep-rate, latency P99, and error-rate calculations

## Key Code

```java
@Component
public class ToolExecutionRecorder {

    public void record(Long executionId, ToolExecutionResult result) {
        // Persist to audit log store
        // On failure: throw ToolExecutionAuditException
    }
}
```

## Recorded Fields

| Field | Source | Purpose |
|-------|--------|---------|
| `executionId` | SfAgentExecution | Correlates with agent execution |
| `toolName` | ToolCall | Identifies which tool was invoked |
| `success` | ToolExecutionResult | Pass/fail status |
| `blocked` | ToolExecutionResult | Whether safety guard blocked it |
| `errorCategory` | ToolExecutionResult | Classification for analysis |
| `errorMessage` | ToolExecutionResult | Human-readable failure reason |
| `latencyMs` | Stopwatch / System.currentTimeMillis() | Performance metric |
| `tokenCount` | `estimateTokens()` | Cost metric |

## Failure Behavior

Audit log writes are **critical path** for security events:

- Normal execution: log and continue
- Audit write failure: throw `ToolExecutionAuditException`
- Rationale: A blocked irreversible operation that is not recorded is an undetected security incident

## Evaluation Metrics Support

The recorder feeds the Evaluation-First metrics pipeline:

| Metric | Calculation |
|--------|-------------|
| **Keep Rate** (north star) | `success_count / total_invocations` |
| **Latency P99** | Percentile of `latencyMs` across recent window |
| **Error Rate by Category** | Group by `errorCategory`, count per window |
| **Blocked Rate** | `blocked_count / total_invocations` |

## Dependencies

| Component | Role |
|-----------|------|
| `ToolExecutionResult` | Data to persist |
| `ToolExecutionAuditException` | Thrown on audit write failure |

## Backlinks

- Called by: [[services/agent-state-machine]] (TOOL_CALLING handler)
- Related: [[tool/tool-safety-guard]], [[tool/tool-error-category]]
