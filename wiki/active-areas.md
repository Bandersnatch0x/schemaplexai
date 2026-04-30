---
title: Active Development Areas
type: project
source: git log, codebase analysis, docs/plans/
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [active, development, focus, hotspots]
confidence: high
---

# Active Development Areas

> One-sentence summary: Current hotspots of development activity based on recent commits, stubs, and planned work.

## Recent Git Activity

Last significant commits:
- `bf8b706` — refactor(docs): 建立主题化文档管理体系
- `c8aeb5d` — chore: unify docs directory structure + update CLAUDE.md with SDD+TDD
- `28cd7b9` — docs: add Agent Runtime Platform design spec + project plan direction update
- `f25315e` — chore: baseline before runtime redesign

**Pattern**: Heavy documentation and planning activity. Code changes are scaffolding-focused.

## Current Hotspots

### 1. Documentation System (High Activity)
- docs/ directory restructuring complete
- SDD + TDD process defined
- ADRs being written (4 complete)
- Specs being drafted (9 active)

### 2. Agent Engine (Core Focus)
- `schemaplexai-agent-engine` has execution stubs
- `AgentExecutionEngine` needs async implementation
- LangChain4j integration scaffolded

### 3. Workflow Engine
- `WorkflowNodeEngine` has strategy pattern for 7 node types
- Flowable BPMN integration in place
- Node executors need concrete implementations

### 4. Frontend Scaffolding
- React Router configured with 10+ pages
- Axios interceptors and auth flow ready
- Page components likely need business logic

## Inactive/Stale Areas

- `schemaplexai-admin` — completely empty
- `schemaplexai-task` — MQ consumers not yet explored
- Tests — zero tests across all modules

## Backlinks

- Plans: [[plans-and-initiatives]]
- Debt: [[technical-debt]]
- Roadmap: [[roadmap]]
