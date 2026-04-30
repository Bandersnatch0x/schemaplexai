# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SchemaPlexAI is an enterprise AI R&D collaboration platform (AI 研发协作平台) covering the full lifecycle: Spec definition → Workflow orchestration → Agent execution → Quality gating → Cost analysis.

**Current state**: Scaffolding is complete (Gateway routing, auth, global filters, base classes, DB init scripts, frontend framework), but many business modules are still stubs. `schemaplexai-admin` is an empty placeholder. No tests have been written yet.

## Tech Stack

- **Backend**: Spring Boot 3.2.5 + Java 21, Spring Cloud Gateway, MyBatis-Plus 3.5.5, Flowable 7, LangChain4j 0.31.0
- **Data**: PostgreSQL 16 (OLTP), ClickHouse 24 (analytics), Redis 7, RabbitMQ 3.12, Milvus 2.3.5 (vectors), MinIO (object storage)
- **Frontend**: React 18.3 + TypeScript 5.5 + Vite + Ant Design 5 + Zustand 4.5.4
- **Observability**: Prometheus + Grafana + ELK + Jaeger
- **API Docs**: Knife4j 4.4.0 at `http://localhost:8082/doc.html`

## Build & Development Commands

### Backend

```bash
# Compile entire project
mvn clean compile

# Run a single service (example: agent-engine)
mvn spring-boot:run -pl schemaplexai-agent-engine

# Run tests (currently none exist)
mvn test

# Static analysis
mvn spotbugs:check
mvn checkstyle:check
```

### Frontend

```bash
cd schemaplexai-ui
npm install
npm run dev        # Dev server on http://localhost:3000
npm run build      # Production build
npm run lint       # ESLint
```

### Infrastructure

```bash
cd docker
docker-compose up -d
```

This starts: PostgreSQL, Redis, RabbitMQ, MinIO, Milvus, ClickHouse, Elasticsearch, Prometheus, Grafana. Database init scripts are in `docker/postgres/init/`.

## Service Architecture

All traffic enters through `schemaplexai-gateway` (port 8080), which routes to domain services:

| Service | Port | Gateway Prefix | Responsibility |
|---------|------|----------------|----------------|
| gateway | 8080 | — | JWT validation, tenant resolution, rate limiting, CORS |
| system | 8081 | `/system/**`, `/auth/**` | Tenant, user, role, permission, AI model management |
| web | 8082 | `/web/**` | Controllers, SSE, WebSocket, Knife4j docs |
| agent-config | 8083 | `/agent-config/**` | Agent definitions, configurations, execution records |
| agent-engine | 8084 | `/agents/**`, `/agent-engine/**` | **Core** — Agent execution engine, LLM orchestration, token budgeting |
| context | 8085 | `/context/**` | RAG, knowledge docs, vector search, MinIO/Tika ingestion |
| spec | 8086 | `/spec/**` | Spec documents, templates, reviews, change tracking |
| workflow | 8087 | `/workflow/**` | Flowable BPMN workflows, AI node engine |
| integration | 8088 | `/integration/**` | GitHub/GitLab/Jenkins integrations, MCP servers, tools |
| ops | 8089 | `/ops/**` | Artifacts, notifications, ClickHouse cost analytics |
| quality | 8090 | `/quality/**` | Spec drift detection, security audit, quality gating |
| task | 8091 | `/task/**` | MQ consumers, scheduled jobs |

`schemaplexai-web` acts as the primary web接入层 — most Controller/SSE/WebSocket endpoints live here rather than in individual domain services. Domain services expose their own REST APIs and are consumed internally.

## Module Dependency Chain

```
schemaplexai-common  (no internal deps)
schemaplexai-model   → common
schemaplexai-dao     → common, model
schemaplexai-task    → common, model, dao

All other services → common, model, dao
```

`schemaplexai-admin` is a placeholder aggregator module with no code yet.

## Key Backend Patterns

### Base Classes

- **All Entities** extend `BaseEntity` (`schemaplexai-model`): provides `id` (ASSIGN_ID), `tenantId`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deleted` (logic delete). Never add these fields manually.
- **All Mappers** extend `BaseMapperX<T>` (`schemaplexai-dao`): extends MyBatis-Plus `BaseMapper<T>`.
- **All Controllers** extend `BaseController` (`schemaplexai-web`): provides `success()`, `success(T)`, `error(String)`, `error(Integer, String)` helpers that wrap responses in `Result<T>`.

### API Response Format

Every REST endpoint returns `Result<T>` (`schemaplexai-common`):

```java
Result.success(data)     // code 200
Result.error(message)    // code 500
Result.error(ResultCode.NOT_FOUND)
```

### Multi-Tenant Isolation

Tenant ID is extracted from the `X-Tenant-Id` header at the Gateway and propagated via `TenantContextHolder` (ThreadLocal). `TenantLineInterceptor` (`schemaplexai-dao`) automatically injects `tenant_id` filters into SQL queries. Global tables (`sf_tenant`, `act_*`) are excluded from tenant filtering.

### Exception Handling

Use `BaseException` (`schemaplexai-common`) for business errors. It carries an integer `code` and message. Do not throw raw `RuntimeException` for domain errors.

## Frontend Structure

```
schemaplexai-ui/src/
  api/          # Axios instances per domain + request.ts (interceptors, auth refresh, SSE)
  components/   # Shared components (ChatMemory, Layout, SseViewer, TenantSelector)
  pages/        # Route-level pages (Dashboard, AgentManager, AgentExecutor, etc.)
  router/       # React Router configuration
  stores/       # Zustand stores (userStore, agentStore, sseStore)
  types/        # Shared TypeScript types
  utils/        # token helpers, etc.
```

- Axios base URL defaults to `/api`; Vite dev server proxies `/api` to `http://localhost:8080`.
- Every request automatically attaches `Authorization: Bearer <token>` and `X-Tenant-Id` headers.
- 401 responses trigger token refresh; refresh failure redirects to `/login`.
- SSE uses `EventSource` with credentials; base URL from `VITE_WS_BASE_URL`.

## Environment & Configuration

Backend services read database/Redis/RabbitMQ credentials from `application.yml`. Defaults point to `localhost` with docker-compose credentials. Override via environment variables:

- `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`
- `JWT_SECRET`

Frontend env vars (prefix with `VITE_`):
- `VITE_API_BASE_URL` (default: `/api`)
- `VITE_WS_BASE_URL` (default: `ws://localhost:8080`)

## CI / Quality Gates

GitHub Actions (`.github/workflows/ci.yml`) runs:
1. `mvn clean compile -DskipTests`
2. `mvn test`
3. `npm ci && npm run lint && npm run build`
4. `mvn spotbugs:check` (non-blocking)
5. `mvn checkstyle:check` (non-blocking)

## Reference Documentation

- `README.md` — quick start, service list, access URLs
- `AGENTS.md` — detailed agent development guide for this project
- `docs/design/DESIGN_REVISED.md` — architecture design document (v1.1)
- `docs/PROJECT_PLAN_REVISED.md` — project plan
