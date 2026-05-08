# SchemaPlexAI v1.0 Deployment Guide

> Last updated: 2026-05-08
> Target audience: DevOps, Platform Engineers, SRE
> Scope: v1.0 release deployment (local, staging, production)

---

## 1. Prerequisites

### Hardware

| Environment | CPU | RAM | Disk |
|-------------|-----|-----|------|
| Local dev   | 4 cores | 16 GB | 50 GB SSD |
| Staging     | 8 cores | 32 GB | 200 GB SSD |
| Production (single-node) | 16 cores | 64 GB | 500 GB SSD |

### Software

- **Java**: OpenJDK 21 (Microsoft Build of OpenJDK 21.0.10 recommended)
- **Maven**: 3.9.6+
- **Node.js**: 20 LTS, npm 10+
- **Docker**: 24.0+
- **Docker Compose**: 2.20+

### Network

- Outbound: HTTPS (443) for LLM providers (OpenAI, Anthropic, Azure)
- Inbound: 8080 (gateway), 3000 (frontend)
- Inter-service: 5432, 6379, 5672, 9000, 19530, 8123, 9200

---

## 2. Quick Start (Local Development)

```bash
# 1. Clone repository
git clone <repo-url>
cd schemaplexai

# 2. Configure environment (REQUIRED)
cp .env.example .env
# Edit .env and fill in all REQUIRED fields (POSTGRES_PASSWORD, JWT_SECRET, etc.)

# 3. Start infrastructure services
cd docker
docker-compose up -d
cd ..

# 4. Wait for services to be healthy (postgres, redis, rabbitmq, milvus)
docker-compose ps

# 5. Build all backend modules
mvn clean compile -DskipTests

# 6. Start core services in separate terminals
mvn spring-boot:run -pl schemaplexai-gateway
mvn spring-boot:run -pl schemaplexai-system
mvn spring-boot:run -pl schemaplexai-web
mvn spring-boot:run -pl schemaplexai-agent-engine

# 7. Start frontend
cd schemaplexai-ui
npm install
npm run dev
```

Access: <http://localhost:3000> (frontend), <http://localhost:8082/doc.html> (API docs).

---

## 3. Environment Variables

Create `application-{profile}.yml` per service or use environment overrides.

### Required (all services)

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/schemaplexai
SPRING_DATASOURCE_USERNAME=sf_user
SPRING_DATASOURCE_PASSWORD=sf_password

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# RabbitMQ
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=sf_user
SPRING_RABBITMQ_PASSWORD=sf_password

# JWT
SECURITY_JWT_SECRET=<change-me-32-byte-base64>
SECURITY_JWT_TTL_MS=7200000
```

### Required (agent-engine)

```bash
# LLM Providers (at least one)
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...
AZURE_OPENAI_ENDPOINT=https://...
AZURE_OPENAI_KEY=...

# Vector Store (Milvus)
MILVUS_HOST=localhost
MILVUS_PORT=19530

# Token Budget Defaults
AGENT_TOKEN_BUDGET_PER_TENANT_DAILY=1000000
AGENT_TOKEN_BUDGET_PER_USER_DAILY=100000
```

### Required (context service)

```bash
# Object Storage
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=sf_admin
MINIO_SECRET_KEY=sf_admin_password
MINIO_BUCKET=schemaplexai

# Milvus collection
MILVUS_COLLECTION_DEFAULT=knowledge_base
```

### Optional (observability)

```bash
MANAGEMENT_TRACING_SAMPLING_PROBABILITY=0.1
MANAGEMENT_OTLP_TRACING_ENDPOINT=http://jaeger:4318/v1/traces
LOGGING_FILE_PATH=/var/log/schemaplexai
```

> **Security**: Never commit `.env` files. Use HashiCorp Vault, AWS Secrets Manager, or k8s sealed-secrets in production.

---

## 4. Service Topology

```
+---------------------------------------------------------+
|                     Browser / API Client                 |
+------------------------+-------------------------------+
                         | HTTPS
                         v
              +----------------------+
              |   Gateway (8080)     |  <- JWT, tenant, rate limit
              +----------+-----------+
                         |
        +----------------+-----------------+-------------+
        v                v                 v             v
+--------------+ +--------------+ +--------------+ +------------+
| system 8081  | | web 8082     | | agent-engine | | workflow   |
| (auth, rbac) | | (controllers | | 8084 (core)  | | 8087       |
|              | |  + sse + ws) | |              | |            |
+------+-------+ +------+-------+ +------+-------+ +----+-------+
       |                |                |              |
       +----------------+--------+-------+--------------+
                                v
                +------------------------------+
                | Infrastructure               |
                | - PostgreSQL 16              |
                | - Redis 7                    |
                | - RabbitMQ 3.12              |
                | - Milvus 2.3.5 (vectors)     |
                | - MinIO (objects)            |
                | - ClickHouse 24 (analytics)  |
                | - Elasticsearch 8 (logs)     |
                +------------------------------+
```

| Service        | Port | Critical Path | Notes |
|----------------|------|---------------|-------|
| gateway        | 8080 | Yes           | Single entry, HA via 2+ replicas |
| system         | 8081 | Yes           | Tenant/user/role |
| web            | 8082 | Yes           | Most controllers + SSE/WS |
| agent-config   | 8083 | No            | Read-mostly, cache-friendly |
| agent-engine   | 8084 | Yes           | LLM orchestration, stateful execution |
| context        | 8085 | Yes (RAG)     | Vector search, ingestion |
| spec           | 8086 | No            | Document collaboration |
| workflow       | 8087 | No            | Flowable BPMN |
| integration    | 8088 | No            | External webhooks |
| ops            | 8089 | No            | Cost analytics |
| quality        | 8090 | No            | Drift detection |
| task           | 8091 | Yes           | MQ consumers, async jobs |

---

## 5. First-Time Setup

### Step 1: Database Schema

```bash
# Auto-applied via docker-entrypoint-initdb.d
# Or manually:
psql -h localhost -U sf_user -d schemaplexai -f docker/postgres/init/01-schema.sql
psql -h localhost -U sf_user -d schemaplexai -f docker/postgres/init/02-seed-data.sql
```

### Step 2: Create Default Tenant

```sql
INSERT INTO sf_tenant (id, name, code, status, created_at)
VALUES (1, 'Default', 'default', 1, NOW());

INSERT INTO sf_user (id, tenant_id, username, password, email, status)
VALUES (1, 1, 'admin', '$2a$10$...bcrypt-hash...', 'admin@example.com', 1);
```

### Step 3: Initialize Milvus Collection

```bash
# Default RAG collection (1536 dim for OpenAI text-embedding-3-small)
curl -X POST http://localhost:19530/v1/vector/collections/create \
  -H 'Content-Type: application/json' \
  -d '{
    "collectionName": "knowledge_base",
    "dimension": 1536,
    "metricType": "COSINE"
  }'
```

### Step 4: MinIO Bucket

```bash
docker exec sf-minio mc alias set local http://localhost:9000 sf_admin sf_admin_password
docker exec sf-minio mc mb local/schemaplexai
```

### Step 5: Verify

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' | jq -r '.data.token')
[ -n "$TOKEN" ] || { echo "Login failed"; exit 1; }

# Use token + tenant header
curl http://localhost:8080/system/users \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Tenant-Id: 1'
```

---

## 6. Health Checks

Each Spring Boot service exposes Actuator at `/actuator/health` (within service port).

```bash
curl http://localhost:8080/actuator/health   # gateway
curl http://localhost:8081/actuator/health   # system
curl http://localhost:8082/actuator/health   # web
curl http://localhost:8084/actuator/health   # agent-engine
```

Healthy response: `{"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"},"rabbit":{"status":"UP"}}}`.

### Liveness vs Readiness

- **Liveness** (`/actuator/health/liveness`): Process is alive; restart if down.
- **Readiness** (`/actuator/health/readiness`): Service can accept traffic; depends on DB/Redis/MQ being reachable.

For Kubernetes:

```yaml
livenessProbe:
  httpGet: { path: /actuator/health/liveness, port: 8084 }
  initialDelaySeconds: 60
  periodSeconds: 10
readinessProbe:
  httpGet: { path: /actuator/health/readiness, port: 8084 }
  initialDelaySeconds: 30
  periodSeconds: 5
```

---

## 7. Smoke Tests

After deployment, run end-to-end smoke tests:

```bash
# 1. Gateway is up
curl -f http://localhost:8080/actuator/health

# 2. Login flow works
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' | jq -r '.data.token')
[ -n "$TOKEN" ] || { echo "Login failed"; exit 1; }

# 3. Agent execution end-to-end
curl -f -X POST http://localhost:8080/agent/execution \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"agentId":1,"input":"hello"}'

# 4. SSE streaming works
curl -N http://localhost:8080/sse/agent/execution/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: 1" &
SSE_PID=$!
sleep 5 && kill $SSE_PID

# 5. Frontend loads
curl -f http://localhost:3000 -o /dev/null
```

---

## 8. Troubleshooting

### Gateway returns 401 on every request

- Verify `SECURITY_JWT_SECRET` matches across gateway and downstream services
- Check token TTL has not expired (`SECURITY_JWT_TTL_MS`)
- Confirm `Authorization: Bearer <token>` header is present

### `X-Tenant-Id` not honored, queries return data from all tenants

- Verify `TenantLineInterceptor` is registered in `MyBatisConfig`
- Confirm entity extends `BaseEntity` (auto-fills `tenant_id`)
- Check global tables (`sf_tenant`, `act_*`) are correctly excluded

### Agent execution stuck in SUBMITTED state

- Check RabbitMQ consumers are connected: `rabbitmqctl list_consumers`
- Verify task service is running on port 8091
- Inspect dead-letter queue: `agent.execution.dlq`

### Token budget rejection (HTTP 429)

- Check current daily usage:
  ```sql
  SELECT tenant_id, SUM(prompt_tokens + completion_tokens)
  FROM sf_agent_execution
  WHERE DATE(created_at) = CURRENT_DATE
  GROUP BY tenant_id;
  ```
- Adjust `AGENT_TOKEN_BUDGET_PER_TENANT_DAILY` env var

### Milvus connection refused

- Ensure etcd is up before milvus: `docker-compose up -d etcd && sleep 5 && docker-compose up -d milvus-standalone`
- Verify network: `docker network inspect docker_sf-network`

### Out of memory in agent-engine

- Increase JVM heap: `JAVA_OPTS="-Xms2g -Xmx4g"`
- Inspect heap dump: `jcmd <pid> GC.heap_dump /tmp/dump.hprof`

---

## 9. Production Considerations

### High Availability

- **Gateway**: 2+ replicas behind L4/L7 load balancer (nginx, HAProxy, AWS ALB)
- **Stateless services** (system, web, agent-engine): 2+ replicas, sticky session via Redis
- **PostgreSQL**: Primary + 1 standby (streaming replication) or managed (RDS, Aurora, Cloud SQL)
- **Redis**: Sentinel (3 nodes) or managed cluster
- **RabbitMQ**: 3-node cluster with quorum queues
- **Milvus**: Use Milvus Cluster mode (not standalone) for >1M vectors

### Resource Limits (per replica)

| Service       | CPU    | RAM   | JVM Heap |
|---------------|--------|-------|----------|
| gateway       | 1 vCPU | 1 GB  | 512 MB   |
| system        | 1 vCPU | 1 GB  | 512 MB   |
| web           | 2 vCPU | 2 GB  | 1 GB     |
| agent-engine  | 4 vCPU | 4 GB  | 3 GB     |
| context       | 2 vCPU | 4 GB  | 2 GB     |
| task          | 2 vCPU | 2 GB  | 1 GB     |

### Security Hardening

- Enable TLS at gateway (terminate at LB)
- Enable mTLS between services in production
- Rotate JWT secret quarterly; use `kid` rotation pattern
- Enable Spring Security CSRF for browser-facing endpoints
- Audit `act_*` (Flowable) tables for admin-only access
- Review `sf_audit_log` retention (default 90 days)

### Observability

- **Metrics**: Prometheus scrapes `/actuator/prometheus` every 15s
- **Logs**: Logback -> ELK via filebeat sidecar
- **Traces**: OpenTelemetry -> Jaeger; sample 10% in prod
- **Alerts**: configure on
  - Agent execution failure rate > 5%
  - LLM API error rate > 1%
  - Token budget exhaustion (per tenant)
  - DB connection pool > 80% utilized

---

## 10. Backup & Disaster Recovery

### Daily Backups

```bash
# PostgreSQL (full)
docker exec sf-postgres pg_dump -U sf_user schemaplexai | \
  gzip > /backup/postgres-$(date +%Y%m%d).sql.gz

# MinIO (incremental via mc mirror)
docker exec sf-minio mc mirror local/schemaplexai s3/backup-bucket/

# Milvus (snapshot via pymilvus or backup tool)
# See: https://milvus.io/docs/milvus-backup.md
```

### Retention

- DB: 30 days daily + 12 weekly + 12 monthly
- MinIO: 7 days hot + 30 days cold (S3 Glacier)
- Logs: 30 days hot in ES + 1 year archived

### RTO / RPO Targets (v1.0)

- **RTO**: 4 hours (single-region restore)
- **RPO**: 1 hour (DB log shipping)

---

## 11. Upgrade Procedure

### Backend (zero-downtime, stateless services)

```bash
# 1. Build new image
mvn clean package -DskipTests
docker build -t schemaplexai/agent-engine:v1.1.0 ./schemaplexai-agent-engine

# 2. Rolling update (k8s)
kubectl set image deployment/agent-engine agent-engine=schemaplexai/agent-engine:v1.1.0
kubectl rollout status deployment/agent-engine

# 3. Smoke test
curl -f http://gateway:8080/actuator/health
```

### Database Migrations

- Use Flyway (recommended) or manual scripts in `docker/postgres/migrations/`
- **Always** wrap in transactions
- **Never** drop columns in same release as code change -- use deprecation cycle
- Test migration on staging clone before production

### Frontend

```bash
cd schemaplexai-ui
npm ci
npm run build
# Deploy dist/ to CDN or static host
```

---

## 12. v1.0 Release Checklist

- [x] All P0 blocking issues resolved
- [x] `mvn clean test` passes (0 failures, excluding Docker-dependent tests)
- [x] Core modules coverage >= 80% (common, model, dao, agent-engine, gateway, system, spec, workflow, context, quality, ops, task)
- [x] API documentation verified (Knife4j annotations on all controllers)
- [x] CHANGELOG.md created at project root
- [x] E2E integration tests for MCP, Git, Jenkins
- [x] Performance benchmarks for agent-engine
- [x] Security audit passed (no CRITICAL findings)
- [x] Deployment guide updated

---

## 13. References

- **Service map**: `CLAUDE.md` -> "Service Map"
- **Module dependencies**: `wiki/dependencies.md`
- **Architecture**: `wiki/architecture.md`
- **Active development**: `wiki/active-areas.md`
- **API docs**: <http://localhost:8082/doc.html> (Knife4j)
- **Coverage report**: `docs/COVERAGE.md`
- **Standards**: `docs/standards/`
- **Decisions**: `docs/decisions/` (ADRs)

---

*For questions or escalation, see the project owner in `wiki/active-areas.md`.*
