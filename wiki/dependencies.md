<!-- AUTO-GENERATED: sync-wiki.sh at 2026-05-08T13:00:00Z -->
---
title: Dependencies & Tech Stack
type: architecture
source: pom.xml, schemaplexai-ui/package.json
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [dependencies, versions, tech-stack, maven, npm]
confidence: high
---

# Dependencies & Tech Stack

> One-sentence summary: Backend runs on Spring Boot 3.2.5 + Java 21 with 16 internal modules; frontend uses React 18.3 + Vite + TypeScript 5.5 + Ant Design 5.

## Backend (Maven BOM)

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.2.5 | Core framework |
| Java | 21 | Runtime |
| MyBatis-Plus | 3.5.5 | ORM / SQL builder |
| Knife4j | 4.4.0 | OpenAPI docs (Swagger UI) |
| JJWT | 0.12.5 | JWT token handling |
| LangChain4j | 0.31.0 | LLM orchestration SDK |
| Flowable | 7.0.0 | BPMN workflow engine |
| Milvus SDK | 2.3.5 | Vector database client |
| ClickHouse JDBC | 0.6.0 | Analytics DB driver |
| MinIO | 8.5.7 | Object storage client |
| Apache Tika | 2.9.1 | Document parsing/ingestion |
| Caffeine | 3.1.8 | Local caching |
| Micrometer | 1.12.5 | Metrics instrumentation |
| Testcontainers | 1.19.7 | Integration testing |

## Frontend (npm)

| Dependency | Version | Purpose |
|------------|---------|---------|
| React | 18.3 | UI framework |
| TypeScript | 5.5 | Type safety |
| Vite | — | Build tool / dev server |
| Ant Design | 5 | Component library |
| Zustand | 4.5.4 | State management |
| React Router | — | SPA routing |
| Axios | — | HTTP client |

## Infrastructure

| Component | Version |
|-----------|---------|
| PostgreSQL | 16 |
| Redis | 7 |
| RabbitMQ | 3.12 |
| Milvus | 2.3.5 |
| ClickHouse | 24.3 |
| Elasticsearch | 8.12.0 |
| Prometheus | 2.50.0 |
| Grafana | 10.3.0 |

## Mandatory Environment Variables

> Copy `.env.example` to `.env` and fill in all REQUIRED fields before running `docker-compose up`.

| Variable | Service | Purpose |
|----------|---------|---------|
| `POSTGRES_PASSWORD` | PostgreSQL | Database password — fail-fast if missing |
| `RABBITMQ_DEFAULT_PASS` | RabbitMQ | MQ password — fail-fast if missing |
| `MINIO_ROOT_PASSWORD` | MinIO | Object storage password — fail-fast if missing |
| `GRAFANA_ADMIN_PASSWORD` | Grafana | Dashboard admin password — fail-fast if missing |
| `JWT_SECRET` | Application | Token signing key — min 32 bytes, validated at startup by `JwtSecretStartupValidator` |

Full list of env vars: see `.env.example` at project root.

## Backlinks

- See [[architecture]] for how these fit together
- ADR-003 covers LangChain4j selection in [[decisions]]
