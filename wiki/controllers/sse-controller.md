---
title: SseController
type: controller
source: schemaplexai-web/src/main/java/com/schemaplexai/web/controller/SseController.java
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [controller, sse, realtime]
confidence: high
---

# SseController

> One-sentence summary: SSE subscription and event push endpoint for real-time agent output streaming.

## Base Path

`/sse` (routed via Gateway to `schemaplexai-web` port 8082)

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/subscribe/{clientId}` | Create SseEmitter for client; accepts optional Authorization header |
| POST | `/send/{clientId}` | Push event to specific client (event + data params) |

## Dependencies

- `AgentSseEmitter agentSseEmitter` — emitter lifecycle management

## Notes

- Used by frontend `SseViewer` component
- See `schemaplexai-web/sse/` package for implementation details

## Backlinks

- Frontend: [[frontend/structure]]
- See [[routes]] for routing
