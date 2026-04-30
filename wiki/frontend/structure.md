---
title: Frontend Structure
type: architecture
source: schemaplexai-ui/src/
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [frontend, react, vite, typescript, ant-design]
confidence: high
---

# Frontend Structure

> One-sentence summary: React 18.3 SPA with Vite build, Ant Design 5 UI, Zustand 4.5.4 state management, and Axios per-domain API clients.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | React 18.3 + TypeScript 5.5 |
| Build | Vite |
| UI | Ant Design 5 |
| State | Zustand 4.5.4 |
| Routing | React Router (lazy-loaded routes) |
| HTTP | Axios |

## Directory Structure

```
src/
  api/          # Axios instances per domain + request.ts (interceptors, auth refresh, SSE)
  components/   # Shared: ChatMemory, Layout, SseViewer, TenantSelector
  pages/        # Route-level pages
  router/       # React Router config
  stores/       # Zustand stores
  types/        # Shared TypeScript types
  utils/        # token helpers
```

## Pages

| Route | Component | Lazy |
|-------|-----------|------|
| `/login` | Login | Yes |
| `/dashboard` | Dashboard | Yes |
| `/agents` | AgentManager | Yes |
| `/agents/executor` | AgentExecutor | Yes |
| `/specs` | SpecCenter | Yes |
| `/workflows` | WorkflowCenter | Yes |
| `/contexts` | ContextCenter | Yes |
| `/quality` | QualityCenter | Yes |
| `/integrations` | IntegrationCenter | Yes |
| `/ops` | OpsCenter | Yes |
| `/settings` | SystemSettings | Yes |

## Auth Flow

- Token stored in `localStorage` as `schemaplexai_token`
- Axios interceptors attach `Authorization: Bearer <token>` and `X-Tenant-Id`
- 401 → trigger token refresh; refresh failure → redirect `/login`
- `RequireAuth` wrapper on all routes except `/login`

## SSE

- `EventSource` with credentials
- Base URL from `VITE_WS_BASE_URL` (default `ws://localhost:8080`)
- See [[controllers/sse-controller]] for backend

## Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `VITE_API_BASE_URL` | `/api` | REST API base |
| `VITE_WS_BASE_URL` | `ws://localhost:8080` | WebSocket/SSE base |

## Backlinks

- Backend routes: [[routes]]
- See [[architecture]] for full system topology
