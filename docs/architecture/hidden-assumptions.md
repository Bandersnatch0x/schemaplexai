---
topic: hidden-assumptions
stage: architecture
version: v1.0
date: 2026-05-08
author: architecture-team
source: v1-readiness-review
---

# Hidden Assumptions Inventory

> Assumptions discovered during v1 readiness review that are not explicitly documented or validated. Each assumption carries risk if violated.

---

## 1. Tenant Context Availability in Async/MQ Context

**Assumption**: `TenantContextHolder` (ThreadLocal) is available in all execution paths.

**Reality**: ThreadLocal does not propagate across threads. MQ consumers (`schemaplexai-task`) and `@Async` methods lose tenant context unless explicitly copied.

**Risk**: MQ consumers process messages without tenant isolation, potentially writing data to wrong tenant or failing with NPE.

**Mitigation**:
- Verify `Task` module copies tenant context from message headers to ThreadLocal before processing.
- Add integration test: publish MQ message with `X-Tenant-Id` header, assert consumer sees correct tenant.

**Status**: Partially mitigated -- `schemaplexai-task` uses message headers, but `@Async` callers in agent-engine need audit.

---

## 2. JWT_SECRET Always Present at Startup

**Assumption**: `JWT_SECRET` environment variable is set before any service starts.

**Reality**: Previously, missing `JWT_SECRET` caused silent fallback to a dev-only default, which could leak into production.

**Risk**: Authentication bypass if dev secret is used in production.

**Mitigation**:
- ADR-011 mandates fail-fast: services refuse to start if JWT key is missing or invalid.
- JWKS endpoint validates key algorithm (RS256) and expiry.
- **Action**: Confirm all services have `@ConfigurationProperties` validation that fails on missing `jwt.secret`.

**Status**: Mitigated by ADR-011 (approved 2026-05-08).

---

## 3. ClickHouse Available for Cost Analytics

**Assumption**: ClickHouse cluster is deployed and accessible for cost analytics queries.

**Reality**: ClickHouse deployment is deferred 3-4 weeks per ops team. ADR-012 approved PostgreSQL short-path for v1.

**Risk**: Code paths that reference ClickHouse (e.g., `TimelineClickHouseService`, `ClickHouseCostSyncService`) will throw connection errors if ClickHouse is not available.

**Mitigation**:
- v1 cost analytics uses PostgreSQL `sf_budget` aggregation (ADR-012).
- ClickHouse-dependent code must be behind feature flags or `@ConditionalOnProperty`.
- **Action**: Audit `schemaplexai-ops` and `schemaplexai-agent-engine` for unconditional ClickHouse references.

**Status**: Partially mitigated -- ADR-012 approved, but code-level guards not yet verified.

---

## 4. Milvus Always Available for Vector Operations

**Assumption**: Milvus 2.3.5 cluster is running and accessible for all RAG and memory vector operations.

**Reality**: Milvus is a stateful dependency with its own failure modes (OOM, compaction lag, collection load failures).

**Risk**: Agent execution fails entirely if Milvus is temporarily unavailable. No fallback for vector search.

**Mitigation**:
- `MilvusIsolationService` exists in agent-engine (per wiki gaps scan), but fallback strategy is undocumented.
- **Action**: Define degradation strategy: (a) return empty results on Milvus timeout, (b) log warning, (c) continue execution without RAG context.
- Add circuit breaker (Resilience4j) around Milvus calls.

**Status**: Not mitigated -- no circuit breaker or fallback documented.

---

## 5. Single-Region Deployment

**Assumption**: All services, databases, and infrastructure run in a single data center / cloud region.

**Reality**: No multi-region architecture, no cross-region replication, no geo-routing.

**Risk**: Region outage takes down entire platform. No disaster recovery plan.

**Mitigation**:
- Accept single-region for v1 (startup phase).
- **Action**: Document in ADR that multi-region is out of scope for v1. Plan for v1.1: PostgreSQL streaming replication, Redis Sentinel cross-region, MinIO bucket replication.

**Status**: Accepted risk -- needs explicit ADR documenting this decision.

---

## 6. Inter-Service Network Latency < 10ms

**Assumption**: Service-to-service HTTP calls via OpenFeign complete in < 10ms (same datacenter).

**Reality**: ADR-001 states OpenFeign adds +3-5ms per hop. Chained calls (web → agent-engine → context → Milvus) can accumulate 15-25ms latency.

**Risk**: SSE/WebSocket updates delayed; user-perceived latency exceeds acceptable thresholds for agent execution streaming.

**Mitigation**:
- ADR-001 acknowledges the latency increase.
- **Action**: Set OpenFeign timeouts explicitly (connect: 2s, read: 5s). Add latency metrics per service pair in Prometheus. Define SLO: p99 inter-service latency < 50ms.

**Status**: Partially mitigated -- acknowledged but SLOs not defined.

---

## 7. Database Connection Pool Adequate

**Assumption**: Default HikariCP connection pool (10 connections) is sufficient for all services.

**Reality**: Under load, services with concurrent requests (agent-engine executing multiple agents, web serving SSE connections) may exhaust the pool.

**Risk**: Connection pool exhaustion causes request queuing, timeouts, and cascading failures.

**Mitigation**:
- **Action**: Configure per-service pool sizes based on expected concurrency:
  - agent-engine: maxPoolSize=50 (LLM calls are long-lived)
  - web: maxPoolSize=30
  - system: maxPoolSize=20
  - Others: default 10
- Add Prometheus metric: `hikaricp_connections_active` with alert at 80% threshold.

**Status**: Not mitigated -- default pool sizes assumed adequate.

---

## 8. RabbitMQ Always Available (No Circuit Breaker)

**Assumption**: RabbitMQ cluster has 100% uptime and never loses messages.

**Reality**: RabbitMQ can experience network partitions, disk alarms, memory alarms, and queue corruption.

**Risk**: MQ consumer failures silently drop tasks. Producer failures cause data loss if no outbox pattern.

**Mitigation**:
- ADR-001 specifies manual ack + dead-letter exchange, which prevents message loss during processing.
- **Action**: (a) Enable publisher confirms for all producers, (b) implement outbox pattern for critical events (agent execution completion), (c) add RabbitMQ health check to actuator, (d) alert on queue depth > 10,000.

**Status**: Partially mitigated -- ack mode and DLX configured, but outbox pattern not implemented.

---

## 9. File Upload Size < 10MB

**Assumption**: All file uploads (context ingestion, spec attachments) are under 10MB.

**Reality**: No explicit file size validation documented. Large files (design docs with images, ML models) can exceed 10MB.

**Risk**: OOM errors in services parsing large files (Tika). MinIO upload timeouts. Gateway request body size limits may reject silently.

**Mitigation**:
- **Action**: (a) Set `spring.servlet.multipart.max-file-size=50MB` and `max-request-size=50MB` explicitly, (b) add streaming upload for files > 10MB in context service, (c) set Nginx/Gateway `client_max_body_size` consistently, (d) add file size validation in controller layer with clear error message.

**Status**: Not mitigated -- no explicit size limits documented or enforced.

---

## 10. Concurrent Agent Executions < 100 per Tenant

**Assumption**: Each tenant will have fewer than 100 concurrent agent executions.

**Reality**: No per-tenant concurrency limit exists in agent-engine. A single tenant could trigger thousands of concurrent executions.

**Risk**: Resource exhaustion (LLM API rate limits, DB connections, memory) affects all tenants. LLM provider may throttle or ban the API key.

**Mitigation**:
- `SubAgentQuotaService` exists (per gaps scan) but scope is sub-agent only, not top-level executions.
- **Action**: (a) Implement per-tenant execution semaphore (configurable, default 100), (b) add queue depth metric per tenant, (c) return 429 when limit exceeded, (d) make limit configurable per tenant tier.

**Status**: Not mitigated -- quota exists for sub-agents only.

---

## 11. LLM API Key Never Exhausted or Rate-Limited

**Assumption**: LLM provider API keys (OpenAI, Anthropic, etc.) have unlimited throughput and never hit rate limits.

**Reality**: All LLM providers enforce rate limits (RPM, TPM). Budget exhaustion or provider outages are real scenarios.

**Risk**: Agent executions fail with 429 errors. No retry strategy or provider fallback.

**Mitigation**:
- ADR-010 covers token budget per execution but not provider-level rate limiting.
- **Action**: (a) Implement exponential backoff retry (3 attempts), (b) add provider health check, (c) consider multi-provider fallback (primary: OpenAI, fallback: Anthropic), (d) add rate limit metrics in Prometheus.

**Status**: Partially mitigated -- token budget exists, but provider rate limiting and retry strategy not implemented.

---

## 12. Redis Never Loses Data

**Assumption**: Redis cache (chat memory L1, rate limit counters, session data) never loses data.

**Reality**: Redis can lose data on restart (if AOF/RDB not configured), network partition, or failover.

**Risk**: Chat memory lost mid-conversation. Rate limit counters reset, allowing burst traffic. Session data lost, forcing re-authentication.

**Mitigation**:
- **Action**: (a) Enable AOF with `appendfsync everysec` for Redis, (b) chat memory is L1 only -- agent-engine should have L2 (PostgreSQL) fallback, (c) rate limit counters losing data is acceptable (brief burst), (d) session loss triggers re-auth which is acceptable.

**Status**: Partially mitigated -- L1/L2 memory design exists, but Redis persistence config not verified.

---

## 13. All Services Share the Same PostgreSQL Instance

**Assumption**: All 16 modules connect to a single PostgreSQL instance (different schemas or same schema with tenant isolation).

**Reality**: No database-per-service pattern. All services share one PostgreSQL instance.

**Risk**: Noisy neighbor: agent-engine heavy queries slow down system module. Schema migrations affect all services simultaneously.

**Mitigation**:
- **Action**: (a) Ensure `TenantLineInterceptor` correctly filters all queries, (b) add connection-level resource limits (statement_timeout per role), (c) plan for read replicas if query load exceeds single-instance capacity, (d) use schema-per-service if scaling demands it (v1.1+).

**Status**: Accepted risk for v1 -- needs documentation.

---

## 14. Flowable Database Schema Coexists with Business Schema

**Assumption**: Flowable BPMN engine tables (`ACT_*`) can coexist in the same PostgreSQL database as business tables (`sf_*`).

**Reality**: Flowable creates its own schema with specific naming conventions. Schema migration tools (Flyway/Liquibase) may conflict.

**Risk**: Migration ordering issues. Table name collisions. Tenant isolation may not apply to Flowable tables (global tables excluded per TenantLineInterceptor).

**Mitigation**:
- TenantLineInterceptor already excludes `act_*` tables (global).
- **Action**: (a) Verify Flyway migration ordering: business tables first, Flowable tables second, (b) ensure Flowable `DatabaseSchemaUpdate` runs after business migrations, (c) add integration test: deploy Flowable + business schema together.

**Status**: Partially mitigated -- global table exclusion exists, migration ordering not verified.

---

## 15. MinIO Bucket Access Is Flat (No Per-Tenant Isolation)

**Assumption**: MinIO object storage uses a flat bucket structure. All tenants share the same bucket with path-based isolation (`/{tenantId}/{fileId}`).

**Reality**: No bucket-per-tenant pattern. Path-based isolation depends on correct prefix handling in code.

**Risk**: Bug in path construction leaks files across tenants. Bucket policy misconfiguration exposes all data.

**Mitigation**:
- **Action**: (a) Add unit tests for `MinioFileStorageService` covering tenant isolation, (b) set bucket policy to deny anonymous access, (c) consider bucket-per-tenant for v1.1 if compliance demands it, (d) add pre-signed URL expiry (max 1 hour).

**Status**: Not mitigated -- path-based isolation assumed correct, not tested.

---

## 16. SSE Connections Survive Service Restarts

**Assumption**: SSE (Server-Sent Events) connections from frontend to `schemaplexai-web` survive service restarts and redeployments.

**Reality**: SSE connections are stateful (held HTTP connections). Service restart drops all connections.

**Risk**: Users lose real-time agent execution updates during deployments. No automatic reconnection logic.

**Mitigation**:
- **Action**: (a) Implement `EventSource` reconnection with exponential backoff in frontend, (b) use `Last-Event-ID` header for SSE resume, (c) add health check endpoint for SSE connection status, (d) implement blue-green deployment to minimize connection drops.

**Status**: Not mitigated -- reconnection logic not implemented in frontend.

---

## 17. ClickHouse Schema Ready for Cost Analytics Migration

**Assumption**: When ClickHouse is available (v1.1), the migration from PostgreSQL cost data will be straightforward.

**Reality**: No ClickHouse schema defined. No ETL pipeline designed. No data backfill strategy.

**Risk**: v1.1 migration delayed due to schema design and pipeline development.

**Mitigation**:
- **Action**: (a) Define ClickHouse schema for cost analytics in `docs/specs/v1.1-clickhouse-migration-spec.md` now (even before deployment), (b) design ETL pipeline (PG → ClickHouse sync), (c) ensure `sf_budget` table has all fields needed for multi-dimensional OLAP, (d) add `synced_to_ch` flag for incremental sync.

**Status**: Not mitigated -- schema and pipeline not designed.

---

## Summary Matrix

| # | Assumption | Severity | Status |
|---|-----------|----------|--------|
| 1 | Tenant context in async/MQ | HIGH | Partially mitigated |
| 2 | JWT_SECRET always present | CRITICAL | Mitigated (ADR-011) |
| 3 | ClickHouse available | MEDIUM | Partially mitigated (ADR-012) |
| 4 | Milvus always available | HIGH | Not mitigated |
| 5 | Single-region deployment | LOW | Accepted risk |
| 6 | Inter-service latency < 10ms | MEDIUM | Partially mitigated |
| 7 | DB connection pool adequate | MEDIUM | Not mitigated |
| 8 | RabbitMQ always available | HIGH | Partially mitigated |
| 9 | File upload < 10MB | LOW | Not mitigated |
| 10 | Concurrent executions < 100/tenant | HIGH | Not mitigated |
| 11 | LLM API key unlimited | HIGH | Partially mitigated |
| 12 | Redis never loses data | MEDIUM | Partially mitigated |
| 13 | Shared PostgreSQL instance | MEDIUM | Accepted risk |
| 14 | Flowable schema coexistence | MEDIUM | Partially mitigated |
| 15 | MinIO flat bucket isolation | HIGH | Not mitigated |
| 16 | SSE survives restarts | MEDIUM | Not mitigated |
| 17 | ClickHouse schema ready | LOW | Not mitigated |

## How to Use This Document

1. **Before v1 release**: Resolve all CRITICAL items. Mitigate HIGH items or document acceptance.
2. **During development**: Reference assumptions when writing integration tests.
3. **Architecture reviews**: Revisit this document quarterly. Move resolved items to an appendix.
4. **New team members**: Read this document to understand "what could go wrong" that isn't obvious from the code.

## References

- ADR-001: Service decomposition (`docs/decisions/ADR-001-service-decomposition.md`)
- ADR-011: JWT key rotation SLA (`docs/decisions/ADR-011-jwt-key-rotation-sla.md`)
- ADR-012: Cost analytics v1 short-path (`docs/decisions/ADR-012-cost-analytics-v1-short-path.md`)
- ADR-013: Notification v1 channel reduction (`docs/decisions/ADR-013-notification-v1-channel-reduction.md`)
- Architecture overview: `wiki/architecture.md`
- Dependency matrix: `wiki/dependencies.md`
