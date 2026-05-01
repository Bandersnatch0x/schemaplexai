---
title: Frontend Structure
type: architecture
source: schemaplexai-ui/src/
creation_date: 2026-04-30
update_date: 2026-05-01
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

| Route | Component | Lazy | Layout |
|-------|-----------|------|--------|
| `/login` | Login | Yes | — |
| `/dashboard` | Dashboard | Yes | Progressive |
| `/cockpit` | Cockpit | Yes | Immersive |
| `/canvas` | AgentCanvas | Yes | Immersive |
| `/workflows` | WorkflowMonitor | Yes | Progressive |
| `/agents` | AgentDetail | Yes | Progressive |
| `/agents/executor` | AgentExecutor | Yes | Progressive |
| `/specs` | SpecCenter | Yes | Progressive |
| `/workflows-old` | WorkflowCenter | Yes | Progressive |
| `/contexts` | ContextCenter | Yes | Progressive |
| `/quality` | QualityCenter | Yes | Progressive |
| `/integrations` | IntegrationCenter | Yes | Progressive |
| `/ops` | OpsCenter | Yes | Progressive |
| `/settings` | SystemSettings | Yes | Progressive |

## Design System

Abyss Hive design system — see [[frontend/abyss-hive-design]] for full spec.

**Key tokens**:
- Background: `#0a0e1a` (abyss), `#0d1117` (sidebar), `#111827` (card)
- Primary: `#00d4aa` (cyan), `#ff9f43` (amber), `#ff4757` (red)
- Text: `#e2e8f0` (primary), `#64748b` (secondary)
- Font: Inter + Noto Sans SC + JetBrains Mono
- Border radius: 8px standard, 12px max
- Layout modes: Immersive (borderless, hidden nav) vs Progressive (structured, persistent nav)

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
