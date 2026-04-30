---
title: Ideas Collection
type: index
source: wiki/ideas/
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [ideas, backlog, research, agent]
confidence: high
---

# Ideas Collection

> One-sentence summary: Repository for raw ideas, inspirations, and opportunities encountered during development — awaiting agent research and architecture fit assessment.

## Purpose

The `ideas/` directory captures **unvetted sparks** — anything that could potentially improve the project but hasn't been analyzed yet:

- Interesting patterns from code reviews or other projects
- New libraries, tools, or frameworks worth evaluating
- Architectural improvements or refactor opportunities
- Feature concepts from team discussions or user feedback
- Performance optimizations spotted during profiling
- Security hardening ideas

## Process

```
Raw Idea (anyone) → Record in ideas/YYYYMMDD-*.md → Agent Research → Architecture Fit Assessment → Adopt / Defer / Reject
```

1. **Capture**: When an idea arises, create a new file using the [template](idea-template.md)
2. **Agent Research**: Assign an agent (or self-research) to investigate feasibility, alternatives, and impact
3. **Assessment**: Evaluate against current architecture, roadmap priorities, and technical debt
4. **Decision**: Move to `wiki/roadmap.md`, `wiki/technical-debt.md`, or archive as rejected

## File Naming

```
YYYYMMDD-<brief-keyword>.md
```

Examples:
- `20260430-pgvector-vs-milvus.md`
- `20260501-async-execution-thread-pool.md`
- `20260502-opentelemetry-tracing.md`

## Status Definitions

| Status | Meaning | Next Step |
|--------|---------|-----------|
| `raw` | Just captured, no analysis | Agent research |
| `researching` | Under investigation | Wait for analysis |
| `assessing` | Research complete, evaluating fit | Architecture review |
| `adopted` | Approved for implementation | Move to roadmap/technical-debt |
| `deferred` | Valid but not now | Revisit in future sprint |
| `rejected` | Doesn't fit architecture or priorities | Archive with rationale |

## Backlinks

- [[index]] — Wiki main index
- [[roadmap]] — Development vision and milestones
- [[technical-debt]] — Known debt and deferred tasks
- [[gaps]] — Knowledge gaps that may inspire ideas
