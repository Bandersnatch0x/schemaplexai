---
title: Architectural Decisions
type: decision
source: docs/decisions/
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [adr, decisions, architecture]
confidence: high
---

# Architectural Decisions

> One-sentence summary: All ADRs from docs/decisions/ consolidated in lightweight format (Title, Context, Decision, Status).

## ADR-001: Service Decomposition

- **Context**: Monolith vs microservices for AI R&D collaboration platform
- **Decision**: Adopted domain-driven microservices (16 modules) with Gateway aggregation
- **Status**: Accepted
- **Rationale**: Independent deployment of agent-engine, workflow, context domains; Gateway provides unified entry

## ADR-002: Cursor SDK to OpenSandbox

- **Context**: Need a sandboxed execution environment for agent code
- **Decision**: Migrated from Cursor SDK to OpenSandbox approach
- **Status**: Accepted
- **Rationale**: Better isolation and security for arbitrary code execution

## ADR-003: LangChain4j Selection

- **Context**: Java LLM orchestration library choice
- **Decision**: Selected LangChain4j 0.31.0 over Spring AI
- **Status**: Accepted
- **Rationale**: More mature Java-native LLM abstractions, better integration with external model providers

## ADR-004: Database Middleware Selection

- **Context**: Need ORM + tenant isolation for multi-tenant PostgreSQL
- **Decision**: MyBatis-Plus 3.5.5 with custom TenantLineInterceptor
- **Status**: Accepted
- **Rationale**: Flexible SQL control, easy tenant injection, compatible with complex queries

## Pending Decisions

- Vector DB: Milvus vs pgvector (Milvus chosen but not yet fully exercised)
- Frontend state: Zustand vs Redux (Zustand chosen, no migration planned)
- Test framework: JUnit 5 + Testcontainers (defined but not yet used — zero tests exist)

## Backlinks

- Full ADR sources in `docs/decisions/ADR-*.md`
- See [[architecture]] for service decomposition impact
