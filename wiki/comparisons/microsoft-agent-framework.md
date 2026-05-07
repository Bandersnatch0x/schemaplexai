<!-- AUTO-GENERATED: manual-maintained wiki at 2026-05-08T01:50:00Z -->
---
title: SchemaPlexAI vs Microsoft Agent Framework
type: decision
source: Codebase analysis + github.com/microsoft/agent-framework
creation_date: 2026-05-08
update_date: 2026-05-08
tags: [comparison, architecture, agent-framework, microsoft, multi-agent]
confidence: high
---

# SchemaPlexAI vs Microsoft Agent Framework (MAF)

> Comprehensive architecture comparison. Full analysis: `.claude/plans/breezy-wishing-kitten.md`

## TL;DR

SchemaPlexAI is an **enterprise platform** (multi-tenant, UI, cost analytics, quality gates). MAF is a **framework SDK** (multi-agent orchestration, middleware, provider flexibility). We have what they don't (platform); they have what we don't (orchestration patterns).

## Key Gaps (MAF has, we don't)

| Feature | Priority | Effort |
|---------|----------|--------|
| Multi-agent orchestration (sequential, concurrent, handoff, group chat, Magentic) | HIGH | Large |
| OpenTelemetry integration | HIGH | Small |
| Progressive skill disclosure | MEDIUM | Small |
| Graph-based workflow engine (arbitrary DAG) | MEDIUM | Medium |
| Pluggable middleware pipeline | MEDIUM | Medium |
| A2A protocol | MEDIUM | Medium |

## Our Advantages (we have, MAF doesn't)

- Multi-tenant platform (DB isolation, encryption, rate limiting, cost budgets)
- Spec/Review/Quality gates + Cost analytics (ClickHouse)
- 16-page React frontend
- Tool safety depth (homoglyph detection, SSRF protection, env mismatch)
- Shadow config A/B testing
- 4-dimension admission control (rate, concurrency, token, cost)
- Document ingestion pipeline (MinIO + Tika + Milvus RAG)

## Near-term Action Items

1. **OpenTelemetry** — replace custom PG traces with OTel SDK (~2 days)
2. **Milvus consistency_level** — tracked in [[gaps]] #13 (~0.5 day)
3. **Progressive skill disclosure** — on-demand skill loading (~1 day)

## Backlinks

- See [[architecture]] for system design
- See [[technical-debt]] for implementation gaps
- See [[gaps]] #13 for Milvus consistency issue
