---
title: Wiki Operation Log
type: log
source: wiki/log.md
creation_date: 2026-04-30
update_date: 2026-05-01
tags: [wiki, log, maintenance]
confidence: high
---

# Wiki Operation Log

> One-sentence summary: Chronological record of all wiki operations — initialization, updates, discoveries, and triggers.

## 2026-04-30 — Wiki Initialization (bootstrap)

**Trigger**: User requested LLM Wiki construction per Karpathy/Claude Code methodology.
**Operation**: Phase 1-5 bootstrap executed in a single session.
**Pages created**:
- `wiki/index.md` — Central index with categorized page catalog
- `wiki/data-model.md` — Schema overview with Mermaid ER diagram
- `wiki/schema-evolution.md` — Schema evolution notes (initial load, 3 SQL files)
- `wiki/entities/*.md` — 14 entity pages covering all 35+ tables
- `wiki/routes.md` — Gateway routes + frontend routes + auth map
- `wiki/controllers/*.md` — 3 controller group pages
- `wiki/architecture.md` — Service topology and module dependency chain
- `wiki/dependencies.md` — Full dependency matrix (Spring Boot 3.2.5, Java 21)
- `wiki/decisions.md` — 4 ADRs from docs/decisions/
- `wiki/services/*.md` — 3 core service pages
- `wiki/frontend/structure.md` — React 18 + Vite frontend architecture
- `wiki/plans-and-initiatives.md` — Plans from docs/plans/ and docs/specs/
- `wiki/technical-debt.md` — Extracted from docs and code state
- `wiki/roadmap.md` — 30-week project plan overview
- `wiki/active-areas.md` — Current development hotspots
- `wiki/gaps.md` — Initial gap analysis (9 gaps identified)

**Discovery**: Project has zero tests. `schemaplexai-admin` is empty placeholder. Many business modules are stubs.
**Budget**: Manual session, no API cost.

## 2026-04-30 — CLAUDE.md Refactoring

**Trigger**: User requested CLAUDE.md audit for contradictions/redundancies with wiki.
**Operation**: Reviewed and refactored CLAUDE.md to eliminate dual reference systems.
**Changes**:
- Fixed Gateway routing errors (`/agent/**` for agent-engine, added `/sse/**` `/ws/**` for web)
- Added "docs/ vs wiki/ boundary" section clarifying parallel documentation systems
- Added "Wiki metadata spec" section with independent front-matter format
- Removed direct `docs/` file references from CLAUDE.md; consolidated into `wiki/index.md` > Reference Documentation
- Removed detailed SDD/TDD process descriptions; retained core rules with wiki pointers
- Removed document naming/header/change-process details; retained structure diagram with wiki pointer
- Added `wiki/.qmd/` to `.gitignore`
- Plugin output constraints explicitly exempt `wiki/`

**Discovery**: CLAUDE.md previously maintained a parallel index of docs/ files (Reference Documentation section), creating maintenance burden and ambiguity.

## 2026-04-30 — Ideas Directory Created

**Trigger**: User requested a dedicated space for capturing development ideas for later agent research.
**Operation**: Created `wiki/ideas/` directory with README and template.
**Pages created**:
- `wiki/ideas/README.md` — Process definition, status lifecycle, naming conventions
- `wiki/ideas/idea-template.md` — Standardized template for idea capture

**Process defined**:
```
Raw Idea → Record in ideas/YYYYMMDD-*.md → Agent Research → Architecture Fit → Adopt/Defer/Reject
```

**Status flow**: raw → researching → assessing → adopted/deferred/rejected

## 2026-04-30 — Open Source Agent Architecture Research

**Trigger**: User requested research on 6 open-source AI Agent projects for architecture design reference.
**Operation**: Researched via WebSearch + GitHub API, created ideas files, ran multi-perspective roundtable debate, produced spec and implementation plan.
**Pages created**:
- `wiki/ideas/2026-04-30-open-agents-architecture.md` — Vercel Labs cloud agent template
- `wiki/ideas/2026-04-30-deer-flow-architecture.md` — ByteDance SuperAgent harness
- `wiki/ideas/2026-04-30-langfuse-architecture.md` — LLM observability platform
- `wiki/ideas/2026-04-30-holyclaude-architecture.md` — Containerized AI workstation
- `wiki/ideas/2026-04-30-aionui-architecture.md` — Multi-agent desktop orchestrator
- `wiki/ideas/2026-04-30-zeroboot-architecture.md` — Sub-millisecond CoW VM sandbox
- `docs/specs/open-source-agent-architecture-research.md` — Formal spec with 3-phase design roadmap
- `docs/plans/2026-04-30-phase1-observability-foundation.md` — Phase 1 implementation plan (10 tasks, TDD)
**Key decisions**: Phase 1 — observability + prompt versioning + skill spec + unified messaging. Phase 3 sandbox updated from Docker to zeroboot CoW VM (~0.8ms fork, hardware isolation).

## 2026-04-30 — Phase 3 Sandbox Design Updated

**Trigger**: User identified zeroboot as a superior sandbox solution and requested spec update.
**Operation**: Updated `docs/specs/open-source-agent-architecture-research.md` — Phase 3 sandbox design changed from Docker to zeroboot CoW VM. Rewrote Debate 3 (sandbox technology selection), added Debate 6 (zeroboot vs Docker), updated implementation path.
**Impact**: Phase 3 sandbox implementation cost reduced from 3-4 person-weeks to 1-2 person-weeks, sandbox creation latency from ~500ms to ~0.8ms (600x improvement), memory per sandbox from ~20MB to ~265KB.

## 2026-04-30 — De-duplicated wiki/plans-and-initiatives.md

**Trigger**: User identified redundancy between `wiki/plans-and-initiatives.md` and `docs/plans/README.md`.
**Operation**: Deleted `wiki/plans-and-initiatives.md` — it was a redundant index of docs/plans/ content. Updated all backlinks in wiki/index.md, wiki/active-areas.md, wiki/roadmap.md, wiki/technical-debt.md to point to `docs/plans/README.md`. Added Wiki-to-Docs Fallback rule to CLAUDE.md.
