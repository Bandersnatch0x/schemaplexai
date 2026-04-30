---
title: Routes & API Gateway
type: architecture
source: schemaplexai-gateway/src/main/resources/application.yml, schemaplexai-gateway/src/main/java/com/schemaplexai/gateway/config/GatewayConfig.java, schemaplexai-ui/src/router/index.tsx
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [routing, gateway, api, spring-cloud]
confidence: high
---

# Routes & API Gateway

> One-sentence summary: Spring Cloud Gateway (port 8080) routes 11 service domains; Java Config and YAML define routes; frontend React Router handles 10+ SPA pages.

## Gateway Routes (Java Config — Primary)

| Route ID | Path Predicates | Target Service | Port |
|----------|----------------|----------------|------|
| system-service | `/system/**`, `/auth/**` | schemaplexai-system | 8081 |
| web-service | `/web/**`, `/sse/**`, `/ws/**` | schemaplexai-web | 8082 |
| agent-config-service | `/agent-config/**` | schemaplexai-agent-config | 8083 |
| agent-engine-service | `/agent/**` | schemaplexai-agent-engine | 8084 |
| workflow-service | `/workflow/**` | schemaplexai-workflow | 8087 |
| context-service | `/context/**` | schemaplexai-context | 8085 |
| quality-service | `/quality/**` | schemaplexai-quality | 8090 |
| integration-service | `/integration/**` | schemaplexai-integration | 8088 |
| task-service | `/task/**` | schemaplexai-task | 8091 |
| ops-service | `/ops/**` | schemaplexai-ops | 8089 |
| admin-service | `/admin/**` | schemaplexai-admin | — |

Note: YAML config (`application.yml`) has slightly different path prefixes (`/agents/**`, `/agent-engine/**`) vs Java Config (`/agent/**`). The Java Config uses `lb://` (load balancer) URIs while YAML uses hardcoded `http://localhost` URIs.

## Authentication

- JWT validation at Gateway via [[JwtAuthenticationFilter]] (not yet fully explored)
- `X-Tenant-Id` header extracted and propagated via `TenantContextHolder` (ThreadLocal)
- JWT secret from `JWT_SECRET` env var, expiration 86400000ms (24h)

## Frontend Routes (React Router)

| Path | Page | Auth Required |
|------|------|---------------|
| `/login` | Login | No |
| `/dashboard` | Dashboard | Yes |
| `/agents` | AgentManager | Yes |
| `/agents/executor` | AgentExecutor | Yes |
| `/specs` | SpecCenter | Yes |
| `/workflows` | WorkflowCenter | Yes |
| `/contexts` | ContextCenter | Yes |
| `/quality` | QualityCenter | Yes |
| `/integrations` | IntegrationCenter | Yes |
| `/ops` | OpsCenter | Yes |
| `/settings` | SystemSettings | Yes |

## SSE & WebSocket

- SSE endpoint: `/sse/subscribe/{clientId}` (schemaplexai-web)
- WebSocket: `/ws/**` (agent WebSocket handler)
- See [[controllers/sse-controller]]

## Backlinks

- See [[architecture]] for service topology
- Controller details in [[controllers/*]]
- Frontend structure in [[frontend/structure]]
