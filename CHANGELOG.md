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
- 1,586 backend tests (agent-engine, 4 skipped for Docker)
- 129 backend tests (context)
- 111 backend tests (integration)
- 63 backend tests (system)
- 43 backend tests (web)
- 41 backend tests (agent-config)
- 31 backend tests (gateway)
- 98 backend tests (common, model, dao, task)
- 499 backend tests (dao, model, common combined)
- 100 frontend tests (vitest)

### Documentation
- Wiki knowledge base with auto-sync
- Architecture decision records (ADRs)
- API documentation via Knife4j
- Data model documentation

## Release Statistics

| Metric | Value |
|--------|-------|
| Total Backend Tests | 2,601 |
| Total Frontend Tests | 100 |
| Backend Modules | 11 |
| Services | 11 microservices |
| Frontend Test Coverage (Statements) | 78.21% |
| Frontend Test Coverage (Branches) | 75.46% |
| Frontend Test Coverage (Functions) | 68.21% |
| Frontend Test Coverage (Lines) | 78.96% |

### Known Limitations
- Container sandbox tests require Docker and are skipped when unavailable
- `schemaplexai-admin` module is a stub (empty)
- Some modules (ops, quality, spec, workflow) have no test coverage yet
- Frontend function coverage is below 80% target
