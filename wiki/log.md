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

## 2026-05-01 — Phase 1 Observability Foundation (archive)

**Trigger**: Security audit found 3 CRITICAL + 4 HIGH issues after multi-agent parallel implementation.
**Operation**: All issues fixed and merged to master. Change archived.
**Changes archived**: `docs/archive/phase1-observability/` (proposal, spec, design, tasks, context)
**Issues resolved**:
- C-1: SSE JWT validation (local JwtValidator added)
- C-2: PromptVersion @PreAuthorize
- C-3: SSE send endpoint authorization
- H-1: SfPromptVersion @Valid constraints
- H-2: MQ DTO deserialization
- H-3: Observability PII redaction
- H-4: SSE error wrapping
- CR-H1: Atomic version increment (SELECT FOR UPDATE)
**Branch merged**: `feature/phase1-observability` → `master` (fast-forward)

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

## 2026-05-01 — Workflow Validation: System Notification Module

**Trigger**: Validated the newly built `.claude/` six-phase workflow with a real feature.
**Operation**: Walked through Propose → Spec → Plan → Apply → Archive for "system notification module".
**Files created**:
- `schemaplexai-model/.../Notification.java` — Entity extending BaseEntity
- `schemaplexai-dao/.../NotificationMapper.java` — Mapper with custom @Update methods
- `schemaplexai-web/.../NotificationService.java` — Service interface + implementation
- `schemaplexai-web/.../NotificationController.java` — 3 REST endpoints
- `schemaplexai-web/.../NotificationVO.java` — Response VO
- `schemaplexai-ui/src/types/notification.ts` — Frontend types
- `schemaplexai-ui/src/api/notification.ts` — Frontend API client
- `docker/postgres/init/04-notification.sql` — DB schema
- `schemaplexai-web/.../NotificationServiceTest.java` — 3 unit tests (all pass)

**Discovery**: `schemaplexai-web` pom.xml was missing `schemaplexai-dao` dependency (fixed). Existing `schemaplexai-common` PageParamTest has compile errors (pre-existing, unrelated). Java 21 required via `JAVA_HOME` export.
**Workflow notes**: Graphify task graph worked well for identifying parallel groups. Gateguard hook requires fact verification on every write — adds friction but ensures discipline.

## 2026-05-01 — Abyss Hive UI Design System Spec & Plan

**Trigger**: User requested a comprehensive UI/UE design system for SchemaPlexAI frontend with "hive/ant colony" metaphor and Black Mirror futurism.
**Operation**: Brainstorming session with visual companion, design doc writing, implementation plan writing.
**Pages created**:
- `docs/superpowers/specs/2026-05-01-abyss-hive-ui-design.md` — Full UI/UE design spec (9 chapters: design system, layout, components, 4 signature pages, AntD theme, Midjourney prompts)
- `docs/superpowers/plans/2026-05-01-abyss-hive-ui.md` — 17-task implementation plan (TDD, bite-sized, exact code)
- `wiki/frontend/abyss-hive-design.md` — Wiki entry for design system

**Key design decisions**:
- Color: Blue-black `#0a0e1a` over pure black (extracted from AI-generated concept art)
- Fonts: Inter + Noto Sans SC + JetBrains Mono (all SIL OFL open source)
- Input style: x.com-inspired transparent background + floating labels + 8px radius
- Layout: Scene-adaptive — Immersive for cockpit/canvas, Progressive for monitor/detail
- Components: HexIcon, StatCard, PillNav, TerminalLog (all with tests)

**Change workspace**: `.claude/changes/abyss-hive-ui/`

## 2026-05-01 — Wiki Gaps Completion (Priority Services)

**Trigger**: Team lead assigned wiki gaps completion task — fill undocumented services and controllers from `wiki/gaps.md`.
**Operation**: Read gaps.md, explored 7 key source files, generated 8 wiki pages, updated index and gaps.
**Pages created**:
- `wiki/services/agent-runtime-orchestrator.md` — Core execution loop (trace, admission, state machine, 50-iteration guard)
- `wiki/services/agent-execution-lifecycle-service.md` — Pause/resume/cancel/snapshot with Redis state
- `wiki/services/agent-state-machine.md` — In-memory state machine with terminal state guard
- `wiki/services/execution-admission-service.md` — 4-dimension admission (rate, concurrency, token, cost)
- `wiki/services/jwt-auth-filter.md` — Gateway global filter with whitelist and claim injection
- `wiki/services/auth-service.md` — BCrypt login, JWT generation, Redis blacklist
- `wiki/services/tenant-line-interceptor.md` — MyBatis-Plus tenant SQL injection with global table bypass
- `wiki/controllers/auth-controller.md` — `/auth/login`, `/auth/refresh`, `/auth/logout` endpoints

**Pages updated**:
- `wiki/index.md` — Added 7 new service links + AuthController link
- `wiki/gaps.md` — Marked 4 Open Questions as resolved (JWT auth, execution flow, tenant isolation)

**Discovery**:
- `JwtAuthenticationFilter` was renamed to `JwtAuthFilter` in gateway module
- `AgentRuntimeOrchestrator` is synchronous (comment says "async in production" but not implemented)
- `ExecutionAdmissionService` uses 4 Redis key prefixes for rate/concurrency/token/cost
- `TenantLineInterceptor` ignores `sf_tenant` and `act_*` tables (global tables)
- `AuthService` stores access tokens in `sf:memory:chat:{userId}` key (chat memory namespace reuse)

**Remaining gaps** (not in scope for this pass):
- `QualityOrchestrator`, `FlowableDelegateAdapter` — undocumented services
- ClickHouse schema, Milvus collections — no init scripts found
- Flowable BPMN process definitions — no BPMN files found
- MQ exchanges/queues config — not explored
- Frontend page components — not explored

## 2026-05-01 — Core AI Engine Design Phase 2 (archive)

**Trigger**: Team lead requested T4 archive for Core AI Engine Design Phase 2 spec and design.
**Operation**: Code review agent completed review, fixed 2 HIGH issues, and archived to docs/.
**Files archived**:
- `docs/specs/core-ai-engine.md` — Phase 2 technical spec (11-state machine, data model, API, component gaps)
- `docs/designs/core-ai-engine.md` — Phase 2 architecture design (C4 L1-L3, sequence diagrams, Token Budget, Chat Memory)

**Review findings**:
- H-1: Spec state machine had 7 states, code has 11 — fixed by syncing to full state enum
- H-2: Spec data model fields mismatched code entities — fixed by aligning with SfAgentExecution/SfAgentExecutionSnapshot
- M-1: resume transition target was THINKING, code uses READY — fixed
- M-2: C4 Container diagram missed 7 existing handlers — fixed by adding all 9 handlers

**Change workspace**: `.claude/changes/core-ai-engine-design/`
**Review report**: `.claude/changes/core-ai-engine-design/review.md`

## 2026-04-30 — De-duplicated wiki/plans-and-initiatives.md

**Trigger**: User identified redundancy between `wiki/plans-and-initiatives.md` and `docs/plans/README.md`.
**Operation**: Deleted `wiki/plans-and-initiatives.md` — it was a redundant index of docs/plans/ content. Updated all backlinks in wiki/index.md, wiki/active-areas.md, wiki/roadmap.md, wiki/technical-debt.md to point to `docs/plans/README.md`. Added Wiki-to-Docs Fallback rule to CLAUDE.md.
