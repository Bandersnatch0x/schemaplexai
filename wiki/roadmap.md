---
title: Development Roadmap
type: project
source: docs/plans/project-plan.md, docs/plans/unified-dev-plan.md
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [roadmap, vision, milestones]
confidence: medium
---

# Development Roadmap

> One-sentence summary: 30-week phased delivery from scaffolding to production-ready AI R&D collaboration platform.

## Phase 1: Foundation (Weeks 1-8)
- Multi-tenant auth and RBAC
- Gateway routing and rate limiting
- Base entity, mapper, controller patterns
- Docker infrastructure
- **Status**: Complete ✅

## Phase 2: Core AI Engine (Weeks 9-16)
- Agent execution engine (async, lifecycle)
- LLM orchestration via LangChain4j
- Token budgeting and cost tracking
- Chat memory (short/long term)
- **Status**: Scaffolding complete, logic stubs ⚠️

## Phase 3: Workflow & Context (Weeks 17-24)
- Flowable BPMN integration
- AI node engine (7 node types)
- RAG pipeline (Tika → Milvus)
- Knowledge document ingestion
- **Status**: Scaffolding complete, logic stubs ⚠️

## Phase 4: Quality & Integration (Weeks 25-30)
- Spec drift detection
- Security audit and quality gating
- GitHub/GitLab/Jenkins integrations
- MCP server management
- **Status**: Partial scaffolding ⚠️

## Phase 5: Polish & Production (Post-30)
- Test coverage 80%+
- Performance optimization
- Documentation completion
- Admin module
- **Status**: Not started ❌

## Key Milestones

| Milestone | Target | Status |
|-----------|--------|--------|
| First agent execution | Week 12 | Not yet |
| End-to-end workflow | Week 20 | Not yet |
| Quality gate active | Week 26 | Not yet |
| Production ready | Week 30 | Not yet |

## Backlinks

- Detailed plans: [[plans-and-initiatives]]
- Technical debt: [[technical-debt]]
- Active work: [[active-areas]]
