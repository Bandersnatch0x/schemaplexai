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

> See `wiki/dependencies.md` for full dependency matrix with versions.

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
| web | 8082 | `/web/**`, `/sse/**`, `/ws/**` | Controllers, SSE, WebSocket, Knife4j docs |
| agent-config | 8083 | `/agent-config/**` | Agent definitions, configurations, execution records |
| agent-engine | 8084 | `/agent/**` | **Core** — Agent execution engine, LLM orchestration, token budgeting |
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

> See `wiki/frontend/structure.md` for full frontend architecture details.

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

## Development Workflow / 开发流程规范

### SDD + TDD 双轨流程

本项目采用 **Specification-Driven Development（规格驱动开发）** + **Test-Driven Development（测试驱动开发）**。

```
需求(requirements) → 规格(specs) → 设计(designs) → 计划(plans) → 编码+测试 → 评审
                                    ↑___________________________|
                                          TDD: RED → GREEN → REFACTOR
```

**核心规则**：
- 任何超过 50 行的代码变更必须有对应的 `docs/specs/` 或 `docs/designs/` 文档支撑
- 任何新模块必须有 `docs/designs/` 设计评审通过后才能编码
- 所有代码必须通过 TDD 流程（先写测试，再写实现，后重构）
- 无测试的代码不允许提交

> 详细规范见 `wiki/plans-and-initiatives.md` 和 `wiki/roadmap.md`。项目文档目录结构见 `wiki/index.md` > Project Management。

### 文档目录规范

所有项目正式文档（`docs/`）按统一结构存放：

```
docs/
├── requirements/     # 产品需求（PRD）
├── specs/            # 技术规格说明书
├── designs/          # 架构设计文档
├── plans/            # 实施计划
├── ui/               # UI/UX 设计文档
├── decisions/        # ADR（架构决策记录）
├── standards/        # 开发规范与流程
├── archive/          # 已归档旧文档
└── README.md         # 文档总览
```

> 完整规范（命名规则、front-matter 要求、变更流程、模板）见 `wiki/index.md` > Project Management 下的相关页面。

### 文档边界：docs/ vs wiki/

本项目存在两套并行文档体系，职责不同、规范不同：

- **`docs/`** — **项目正式文档**（面向研发团队），遵循 SDD 流程。包括 PRD、技术规格、架构设计、ADR、开发规范等。受文档目录规范严格约束。
- **`wiki/`** — **Claude Code 知识库**（面向 AI 助手），遵循 LLM Wiki 模式。包括实体系谱、代码映射、架构速查、技术债务、知识缺口等。**不受 `docs/` 文档规范约束**，使用独立的 YAML front-matter 格式。

### Plugin 输出约束

- **禁止** plugin 在 `docs/` 根目录或创建 plugin 专属子目录（如 `docs/superpowers/`、`docs/ecc/`）直接输出
- **`wiki/` 除外** — Claude Code 可直接读取和更新 `wiki/` 中的知识库页面，作为会话持久记忆
- Plugin 生成的 plan/spec 先输出到临时位置（如 `.claude/outputs/`）
- 经人工评审后，按统一规范重命名并移入对应的 `docs/` 子目录
- 旧版 plugin 输出归档到 `docs/archive/`

## LLM Wiki (Claude Code Knowledge Base)

This project maintains a self-updating LLM Wiki at `wiki/` for persistent cross-session knowledge.

**Key rule**: Always check `wiki/` before answering questions about this project's architecture, patterns, or decisions.

### Wiki Structure

- `wiki/index.md` — Catalog of all pages
- `wiki/log.md` — Update history and discoveries
- `wiki/gaps.md` — Undocumented elements and open questions
- `wiki/data-model.md` — Database schema overview
- `wiki/routes.md` — Gateway and frontend routing
- `wiki/architecture.md` — Service topology and patterns
- `wiki/dependencies.md` — Tech stack and versions
- `wiki/decisions.md` — Architectural decisions (ADRs)
- `wiki/entities/` — One page per database entity
- `wiki/controllers/` — Key controller documentation
- `wiki/services/` — Core service documentation
- `wiki/frontend/` — Frontend architecture
- `wiki/plans-and-initiatives.md` — Active plans
- `wiki/technical-debt.md` — Known debt and deferred tasks
- `wiki/roadmap.md` — Development vision
- `wiki/active-areas.md` — Current hotspots

### Wiki 元数据规范

Wiki 页面使用独立的 YAML front-matter，与 `docs/` 规范不同：

```yaml
---
title: Page Title
type: model|controller|service|architecture|decision|project|index|log
source: file path
creation_date: YYYY-MM-DD
update_date: YYYY-MM-DD
tags: [tag1, tag2]
confidence: high|medium|low
---
```

每页开头必须有一行 TLDR summary。页面间使用 `[[backlinks]]` 交叉引用。

### How to Use

When asked about:
- **Data models** → Read `wiki/data-model.md` + relevant `wiki/entities/*.md`
- **APIs/Routing** → Read `wiki/routes.md` + relevant `wiki/controllers/*.md`
- **Architecture** → Read `wiki/architecture.md` + `wiki/decisions.md`
- **Current work** → Read `wiki/active-areas.md` + `wiki/plans-and-initiatives.md`
- **What's missing** → Read `wiki/gaps.md`

If the wiki doesn't have the answer, update it after researching the code.

> 所有参考文档（设计文档、计划、规范、模板）统一通过 `wiki/index.md` > Project Management > Reference Documentation 查阅。
