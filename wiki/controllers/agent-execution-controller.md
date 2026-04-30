---
title: AgentExecutionController
type: controller
source: schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/controller/AgentExecutionController.java
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [controller, agent, execution, lifecycle]
confidence: high
---

# AgentExecutionController

> One-sentence summary: Core REST API for agent execution lifecycle — start, pause, resume, cancel, and snapshot retrieval.

## Base Path

`/agents` (routed via Gateway to `schemaplexai-agent-engine` port 8084)

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/{id}/execute` | Start execution with tenantId and prompt in body |
| POST | `/{id}/executions/{execId}/pause` | Pause execution with reason param |
| POST | `/{id}/executions/{execId}/resume` | Resume paused execution |
| POST | `/{id}/executions/{execId}/cancel` | Cancel execution |
| GET | `/{id}/executions/{execId}/snapshot` | Get latest execution snapshot |

## Dependencies

- `AgentExecutionEngine executionEngine` — starts executions
- `AgentExecutionLifecycleService lifecycleService` — pause/resume/cancel/snapshot

## Notes

- Returns `Result<T>` wrapper (see [[architecture]])
- `tenantId` passed in request body rather than header for this endpoint
- See [[services/agent-execution-engine]] for engine internals

## Backlinks

- Agent entities: [[entities/agent]]
- See [[routes]] for routing
