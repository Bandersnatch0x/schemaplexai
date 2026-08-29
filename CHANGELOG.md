# Changelog

All notable changes to SchemaPlexAI are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-05-08

### Added

#### Core Platform
- Multi-tenant architecture with `X-Tenant-Id` header isolation and `TenantContextHolder`
- Spring Cloud Gateway with JWT auth, rate limiting, and tenant resolution
- WebSocket and SSE support for real-time agent execution events
- Knife4j 4.4.0 OpenAPI documentation at `/doc.html`

#### Agent Engine (schemaplexai-agent-engine)
- Agent execution engine with state machine (IDLE -> THINKING -> TOOL_CALLING -> COMPLETED/FAILED)
- Token budget management with input/output/tool-call limits
- Execution admission service with rate limiting, concurrency control, and cost budgeting (Redis-backed)
- Composite chat memory store with auto-compaction (SlidingWindow, Summarization strategies)
- Tool registry with structured parsing, safety guard, and sandbox execution
- Loop detection service to prevent infinite tool-calling cycles
- Self-correction engine with generate-critique-refine loop
- Chain-of-Thought visualizer with Markdown export
- Code execution reasoner with safety checks
- Skill and Role registries with Caffeine caching
- Agent shadow config for A/B testing
- Container sandbox provider for isolated tool execution (Docker-backed)
- Observability recorder with OpenTelemetry tracing and PII redaction

#### Integration (schemaplexai-integration)
- Third-party integration management (GitHub, GitLab, Jenkins)
- API gateway configuration and routing
- MCP (Model Context Protocol) server registration and tool discovery
- Skill definition management with YAML frontmatter parsing
- Webhook registration and event handling

#### Context (schemaplexai-context)
- RAG (Retrieval-Augmented Generation) service
- Knowledge document management with Milvus vector sync
- Workspace service for tenant-isolated workspaces

#### Web (schemaplexai-web)
- REST controllers with `Result<T>` envelope pattern
- JWT validation and SSE emitter management
- Notification service

#### System (schemaplexai-system)
- Tenant, user, role, and permission management
- Auth service with BCrypt password encoding and JWT refresh tokens

#### Task (schemaplexai-task)
- Memory consolidation job
- Milvus reconciliation job

#### Frontend (schemaplexai-ui)
- React 18 + TypeScript 5.5 + Vite + Ant Design 5 + Zustand 4.5.4
- Progressive layout with domain navigation and expandable submenus
- Immersive layout with 7 nav icons (6 domains + canvas)
- Kanban board with @dnd-kit drag-and-drop
- Task board, jobs, and detail pages
- Agent canvas for visual agent orchestration
- Login page with tenant selection
- SSE viewer component

#### Infrastructure
- PostgreSQL 16, ClickHouse 24, Redis 7, RabbitMQ 3.12, Milvus 2.3.5, MinIO
- Prometheus + Grafana + ELK + Jaeger observability stack
- Docker Compose for local development

### Security
- Tool safety guard with irreversible operation blocking
- Security policy loader for tenant-aware environment checks
- PII redaction in observability traces
- Rate limiting and concurrency controls on agent execution

### Testing

> **修正 2026-08-30**：本条目原载测试数为发布时点陈旧快照（后端合计 2,601），经仓库构建产物证伪：分项偏差 30%–180%，且 "98 (common, model, dao, task)" 与 "499 (dao, model, common combined)" 两行自相矛盾。现修正为**截至 2026-08-30 的实测/台账口径**——聚合自各模块 `target/surefire-reports`（13 模块 2026-08-30 运行；3 个未重跑模块采用 2026-07-02 记录运行）。来源：`docs/reviews/spec-compliance-2026-08-29/requirements/release-readiness.md` REQ-21/REQ-22。

- 1,874 backend tests (agent-engine, 4 skipped for Docker)
- 300 backend tests (integration)
- 279 backend tests (quality)
- 269 backend tests (ops)
- 242 backend tests (context; last recorded run 2026-07-02)
- 210 backend tests (workflow; last recorded run 2026-07-02)
- 174 backend tests (system)
- 150 backend tests (spec)
- 147 backend tests (web)
- 129 backend tests (common)
- 111 backend tests (admin)
- 103 backend tests (agent-config; last recorded run 2026-07-02)
- 86 backend tests (gateway)
- 27 backend tests (model)
- 20 backend tests (dao)
- 15 backend tests (task)
- 146 frontend tests (vitest, 单元用例静态计数口径; 22 个 `*.test.*` 文件) + 3 Playwright e2e specs

### Documentation
- Wiki knowledge base with auto-sync
- Architecture decision records (ADRs)
- API documentation via Knife4j
- Data model documentation

## Release Statistics

> **修正 2026-08-30**：下表数字修正为截至 2026-08-30 的实测/台账口径；前端覆盖率为 2026-05-08 覆盖率产物记录值（复核重算，原宣称四项均高报 1.6–7.6 个百分点）。来源：`docs/reviews/spec-compliance-2026-08-29/requirements/release-readiness.md` REQ-21/REQ-22。

| Metric | Value |
|--------|-------|
| Total Backend Tests | ≈4,136（13 模块 2026-08-30 运行小计 3,581 + agent-config/context/workflow 2026-07-02 记录运行 555；原载 2,601） |
| Total Frontend Tests | 146 unit cases（静态计数口径）+ 4 e2e cases（3 specs）（原载 100） |
| Backend Modules | 16 |
| Services | 13 backend microservices (+ frontend) |
| Frontend Test Coverage (Statements) | 73.09%（2026-05-08 产物记录值；原载 78.21%） |
| Frontend Test Coverage (Branches) | 68.31%（2026-05-08 产物记录值；原载 75.46%） |
| Frontend Test Coverage (Functions) | 64.68%（2026-05-08 产物记录值；原载 68.21%） |
| Frontend Test Coverage (Lines) | 74.67%（2026-05-08 产物记录值；原载 78.96%） |

### Known Limitations

> **修正 2026-08-30**：原载 "`schemaplexai-admin` module is a stub (empty)" 与 "Some modules (ops, quality, spec, workflow) have no test coverage yet" 两条与实态相反，现予修正。来源：`docs/reviews/spec-compliance-2026-08-29/requirements/release-readiness.md` REQ-23。

- Container sandbox tests require Docker and are skipped when unavailable
- ~~`schemaplexai-admin` module is a stub (empty)~~ → 修正：admin 模块已实现（25 个主代码文件 / 6 个管理服务 + 111 个测试，截至 2026-08-30 实测口径；JaCoCo 指令覆盖记录值 81.6%，2026-07-02 产物）
- ~~Some modules (ops, quality, spec, workflow) have no test coverage yet~~ → 修正：四模块均有测试套件（截至 2026-08-30 实测口径：ops 269 / quality 279 / spec 150 / workflow 210）
- Frontend function coverage is below 80% target（2026-05-08 产物记录值 64.68%）
- 覆盖率门禁（JaCoCo 指令 80%）下 `schemaplexai-model` / `schemaplexai-dao` 最近记录值仍不达标（台账口径，详见 `docs/COVERAGE.md`）
