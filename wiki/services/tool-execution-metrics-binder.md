---
title: ToolExecutionMetricsBinder
type: service
source: com.schemaplexai.agent.engine.metrics.ToolExecutionMetricsBinder
creation_date: 2026-05-06
tags: [agent-engine, metrics, prometheus, observability]
confidence: high
---

# ToolExecutionMetricsBinder

Prometheus `MeterBinder` for tool execution observability. Uses in-memory `ConcurrentHashMap` counters (no DB polling) to avoid IO overhead impacting P99 latency.

## Metrics Exposed

| Metric Name | Type | Labels | Description |
|-------------|------|--------|-------------|
| `agent_tool_execution_total` | Counter | `status=success\|failure\|blocked` | Total tool executions |
| `agent_tool_execution_latency_seconds` | Timer | none | Execution latency distribution (P50/P95/P99) |
| `agent_tool_keep_rate` | Gauge | none | Success rate = success / total |
| `agent_tool_blocked_rate` | Gauge | none | Blocked rate = blocked / total |
| `agent_tool_error_by_category` | Counter | `errorCategory` | Failures grouped by `ToolErrorCategory` |
| `agent_tool_retry_total` | Counter | none | Total retry attempts |

## Cardinality Control

- **Top-N scaffolding**: `getTopNToolNames()` exposes the top 10 tools by total count for callers, but per-tool labels are not currently attached to registered meters (future enhancement)
- **In-memory counters**: `LongAdder` in `ConcurrentHashMap` — lock-free, high throughput
- **No DB polling**: All metrics derived from runtime events, not database queries

## Recording API

| Method | When to Call |
|--------|-------------|
| `recordSuccess(String toolName, long latencyMs)` | After successful tool execution |
| `recordFailure(String toolName, String errorCategory)` | After failed tool execution |
| `recordBlocked(String toolName)` | After tool is blocked by safety guard |
| `recordRetry(String toolName)` | On each retry attempt |

## Grafana Dashboard

Grafana dashboard JSON skeleton available at `resources/grafana/agent-engine-dashboard.json`.

## See Also

- [[services/agent-runtime-orchestrator]] — orchestrates tool execution
- [[services/tool-registry]] — provides tool names for metrics labels
