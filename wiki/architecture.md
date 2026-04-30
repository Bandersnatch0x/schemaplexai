---
title: System Architecture
type: architecture
source: CLAUDE.md, docs/designs/system-architecture.md, pom.xml
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [architecture, microservices, spring-boot, patterns]
confidence: high
---

# System Architecture

> One-sentence summary: SchemaPlexAI is a Spring Cloud microservices platform with 16 Maven modules, Gateway routing, multi-tenant PostgreSQL, and a React SPA frontend.

## Service Topology

```
                      +-------------+
                      |   Client    |
                      +------+------+
                             |
                      +------v------+
                      |  Gateway    |  Port 8080
                      |  (JWT/Rate  |  Spring Cloud Gateway
                      |   Limit/CORS|
                      +------+------+
                             |
        +--------+-----------+-----------+--------+
        |        |           |           |        |
   +----v----+ +-v-----+ +--v---+ +----v----+ +--v---+
   | system  | |  web  | |agent-| | context | | spec |
   | 8081    | | 8082  | |engine| |  8085   | | 8086 |
   +---------+ +-------+ +------+ +---------+ +------+
   | tenant  | |controllers|     | |  RAG    | | docs |
   | auth    | | SSE/WS  |      | | vectors | |review|
   | RBAC    | | Knife4j |      | | MinIO   | |      |
   +---------+ +-------+ +------+ +---------+ +------+
        |        |           |           |        |
   +----v----+ +-v-----+ +--v---+ +----v----+ +--v---+
   | workflow| |agent- | |quality| |integration| | ops  |
   |  8087   | |config | | 8090  | |  8088   | | 8089 |
   +---------+ +-------+ +------+ +---------+ +------+
   | Flowable| |        | | gates | | GitHub  | |cost  |
   | BPMN    | |        | | audit | | Jenkins | |notify|
   +---------+ +-------+ +------+ +---------+ +------+
        |
   +----v----+
   |  task   |  8091
   +---------+
   | MQ jobs |
   +---------+
```

## Module Dependency Chain

```
schemaplexai-common  (no internal deps)
    ↓
schemaplexai-model   → common
    ↓
schemaplexai-dao     → common, model
    ↓
schemaplexai-task    → common, model, dao

All other services → common, model, dao
```

`schemaplexai-admin` is a placeholder aggregator module with no code yet.

## Communication Patterns

1. **Gateway → Services**: HTTP REST via Spring Cloud Gateway (load-balanced `lb://` in Java Config)
2. **Web → Domain Services**: Internal REST calls (schemaplexai-web hosts most controllers)
3. **Services → DB**: MyBatis-Plus via `schemaplexai-dao`
4. **Async**: RabbitMQ for MQ consumers (task module)
5. **Real-time**: SSE (`/sse/subscribe/{clientId}`) and WebSocket (`/ws/**`)
6. **Cache**: Redis (session, rate limit, chat memory L1)
7. **Vector**: Milvus for RAG embeddings
8. **Object**: MinIO for file storage
9. **Analytics**: ClickHouse for cost analytics
10. **Search**: Elasticsearch for logs/audit

## Key Patterns

- **BaseEntity**: All entities extend `BaseEntity` with `id` (ASSIGN_ID), `tenantId`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deleted`
- **BaseMapperX**: All mappers extend MyBatis-Plus `BaseMapper<T>`
- **BaseController**: All controllers extend `BaseController` with `success()`/`error()` helpers returning `Result<T>`
- **TenantLineInterceptor**: Auto-injects `tenant_id` into SQL; global tables excluded
- **BaseException**: Business errors carry integer code + message

## Infrastructure Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| OLTP DB | PostgreSQL 16 | Primary transactional data |
| Cache | Redis 7 | Sessions, rate limit, chat memory L1 |
| MQ | RabbitMQ 3.12 | Async job processing |
| Vector DB | Milvus 2.3.5 | RAG embeddings |
| Object Store | MinIO | File/document storage |
| Analytics | ClickHouse 24 | Cost analytics warehouse |
| Search | Elasticsearch 8 | Logs and audit search |
| Metrics | Prometheus + Grafana | Observability |
| Tracing | Jaeger | Distributed tracing |
| BPMN | Flowable 7 | Workflow engine |

## Backlinks

- Data model in [[data-model]]
- Routes in [[routes]]
- Dependencies in [[dependencies]]
- Decisions in [[decisions]]
