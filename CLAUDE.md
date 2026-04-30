# CLAUDE.md

SchemaPlexAI project constitution — auto-loaded by Claude Code at session start.
Keep this file lean to preserve context window for actual work.
For full workflow details, see `.claude/workflow/GUIDE.md`.

---

## Project

Enterprise AI R&D collaboration platform (AI 研发协作平台): Spec → Workflow → Agent execution → Quality gating → Cost analysis.

**Current state**: Scaffolding complete (Gateway, auth, base classes, DB init, frontend framework). Many business modules are stubs. `schemaplexai-admin` is empty. No tests yet.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.2.5 + Java 21, Spring Cloud Gateway, MyBatis-Plus 3.5.5, Flowable 7, LangChain4j 0.31.0 |
| Data | PostgreSQL 16, ClickHouse 24, Redis 7, RabbitMQ 3.12, Milvus 2.3.5, MinIO |
| Frontend | React 18.3 + TS 5.5 + Vite + Ant Design 5 + Zustand 4.5.4 |
| Observability | Prometheus + Grafana + ELK + Jaeger |
| API Docs | Knife4j 4.4.0 at `http://localhost:8082/doc.html` |

> Full dependency matrix: `wiki/dependencies.md`

## Quick Commands

```bash
# Backend
mvn clean compile
mvn spring-boot:run -pl schemaplexai-agent-engine
mvn test

# Frontend
cd schemaplexai-ui && npm install && npm run dev

# Infrastructure
cd docker && docker-compose up -d
```

## Service Map

| Service | Port | Prefix | Role |
|---------|------|--------|------|
| gateway | 8080 | — | JWT, tenant, rate limit |
| system | 8081 | `/system/**`, `/auth/**` | Tenant, user, role, permission |
| web | 8082 | `/web/**`, `/sse/**`, `/ws/**` | Controllers, SSE, WS, Knife4j |
| agent-config | 8083 | `/agent-config/**` | Agent definitions |
| agent-engine | 8084 | `/agent/**` | **Core** — LLM orchestration, execution |
| context | 8085 | `/context/**` | RAG, vector search, ingestion |
| spec | 8086 | `/spec/**` | Spec docs, reviews |
| workflow | 8087 | `/workflow/**` | Flowable BPMN, AI nodes |
| integration | 8088 | `/integration/**` | GitHub/GitLab/Jenkins, MCP |
| ops | 8089 | `/ops/**` | Artifacts, cost analytics |
| quality | 8090 | `/quality/**` | Drift detection, security |
| task | 8091 | `/task/**` | MQ consumers, jobs |

> `schemaplexai-web` hosts most Controllers/SSE/WebSocket endpoints as the primary web layer.

## Module Chain

```
schemaplexai-common (no internal deps)
schemaplexai-model → common
schemaplexai-dao   → common, model
schemaplexai-task  → common, model, dao
All other services → common, model, dao
```

## Key Patterns

- **Entities** extend `BaseEntity` (id, tenantId, createdAt, updatedAt, createdBy, updatedBy, deleted). Never add manually.
- **Mappers** extend `BaseMapperX<T>` (MyBatis-Plus `BaseMapper<T>`).
- **Controllers** extend `BaseController` with `success()` / `error()` helpers returning `Result<T>`.
- **API Response**: Every endpoint returns `Result<T>`.
- **Multi-tenant**: `X-Tenant-Id` header → `TenantContextHolder` → `TenantLineInterceptor` auto-injects `tenant_id` filters. Global tables (`sf_tenant`, `act_*`) excluded.
- **Exceptions**: Use `BaseException` with integer code. No raw `RuntimeException` for domain errors.

## Frontend Quick Reference

```
schemaplexai-ui/src/
  api/         # Axios per domain + interceptors, auth refresh, SSE
  components/  # ChatMemory, Layout, SseViewer, TenantSelector
  pages/       # Route-level pages
  stores/      # Zustand stores
  types/       # Shared TS types
```

Axios base URL `/api` (proxied to `:8080`). Auto-injects `Authorization` + `X-Tenant-Id`. 401 triggers refresh, failure → `/login`.

## Context Layer Map

```
Layer 1: CLAUDE.md                       ← This file (always loaded)
Layer 2: .claude/CLAUDE-DEVELOPER.md     ← Workflow protocol (load on demand)
Layer 3: wiki/*.md                       ← Domain knowledge (load on demand)
Layer 4: .claude/changes/<feat>/spec.md  ← Task spec context (sub-agents must read)
Layer 5: .claude/changes/<feat>/context.md ← Task execution context
```

## Where to Look

| Question | Check |
|----------|-------|
| Data models | `wiki/data-model.md` + `wiki/entities/*.md` |
| APIs/Routing | `wiki/routes.md` + `wiki/controllers/*.md` |
| Architecture | `wiki/architecture.md` + `wiki/decisions.md` |
| Current work | `wiki/active-areas.md` + `wiki/plans-and-initiatives.md` |
| What's missing | `wiki/gaps.md` |
| Dev workflow | `.claude/workflow/GUIDE.md` |
| Standards | `docs/standards/` |

## Document Boundaries

- **`docs/`** — Project formal docs (PRD, specs, designs, ADR). Follow SDD process.
- **`wiki/`** — AI knowledge base. Independent YAML front-matter format.
- **`.claude/changes/`** — Execution workspace. Outputs sink to `docs/` after review.

## Rules

- >50 lines change → `docs/specs/` or `docs/designs/` required.
- New module → `docs/designs/` review before coding.
- TDD mandatory: RED → GREEN → REFACTOR.
- No tests, no commit.
- Plugin output → `.claude/outputs/` → human review → `docs/`. Never direct to `docs/`.
