<!-- AUTO-GENERATED: sync-wiki.sh at 2026-08-29T17:39:17Z -->

---
title: Wiki Operation Log
type: log
source: auto-generated
creation_date: 2026-08-30
update_date: 2026-08-30
tags: [wiki, log, maintenance]
confidence: high
---

# Wiki Operation Log

> Auto-generated from git log + docs/ status. Manual edits will be overwritten.

## 2026-08-30 — fix(integration): bound external calls with 30s timeouts and degrade on expiry (issue 918)
d4b85ac
## 2026-08-30 — fix(agent-engine): write failedToolName on failure path so retry replays only the failed call (issue 908)
7b9ef46
## 2026-08-30 — fix(ops): unify budget threshold to decimal, hourly alert job with notification chain (issue 921)
37d1de3
## 2026-08-30 — fix(integration): tenant-scope Git repository/webhook in-memory stores (issue 917)
e051bce
## 2026-08-30 — fix(agent-engine): wire real provider into TOOL_CALLING structured parse (issue 905)
6854c31
## 2026-08-30 — fix(agent-engine): allow null fromState in state-transition event payload (issue 929)
7bf3a6e
## 2026-08-30 — fix(web): wire pagination/tenant interceptors and bound page params (issue 926)
086e302
## 2026-08-30 — fix(integration): implement OAuth authorization_code exchange with encrypted token storage (issue 916)
8552f44
## 2026-08-30 — fix(ops): configurable model pricing, 6-digit cost precision, claude fallback (issue 920)
9466cef
## 2026-08-30 — fix(gateway): tenant existence validation with Caffeine+Redis channel (issue 913)
703d896
## 2026-08-30 — fix(integration): encrypt Git credentials at rest, stop embedding tokens in clone URLs (issue 915)
fad2188
## 2026-08-30 — fix(gateway): converge JWT whitelist to spec set, fix change-password/logout (issue 912)
e28965b
## 2026-08-30 — fix(gateway): run rate limiting before the JWT auth short-circuit (issue 911)
6795f6a
## 2026-08-30 — fix: unify spec lifecycle status vocabulary with DDL VARCHAR semantics
d8d1095
## 2026-08-30 — fix: align quality/MCP entity status types with DDL VARCHAR semantics
04dcc3a
## 2026-08-30 — fix(agent-engine): mutable GATE_BLOCKED event payload
635aaad
## 2026-08-30 — fix(gateway): make lb:// routes resolvable via static instance source (issue 910)
5bc0e53
## 2026-08-30 — fix(db): unify sf_notification DDL into 03 and drop conflicting 04
cdcc318
## 2026-08-29 — fix(agent-engine): align pause tests with tenant-scoped Redis key
99cb3e7
## 2026-08-29 — chore: sync master (incl. dcab02b paused-key fix) into spec-compliance-fix branch
76108b4

---

## Recent Docs Status Changes

- **agent-execution-engine** (spec v1.0): 已批准 — 2026-04-30-v1.0-agent-execution-engine.md
- **api-gateway** (spec v1.0): 已批准 — 2026-04-30-v1.0-api-gateway.md
- **cost-analytics** (spec v1.0): 草稿 — 2026-04-30-v1.0-cost-analytics.md
- **integration-layer** (spec v1.0): 草稿 — 2026-04-30-v1.0-integration-layer.md
- **open-source-agent-architecture-research** (spec v1.0): draft — 2026-04-30-v1.0-open-source-agent-architecture-research.md
- **quality-gate** (spec v1.0): 草稿 — 2026-04-30-v1.0-quality-gate.md
- **rag-pipeline** (spec v1.0): 草稿 — 2026-04-30-v1.0-rag-pipeline.md
- **spec-management** (spec v1.0): 草稿 — 2026-04-30-v1.0-spec-management.md
- **workflow-engine** (spec v1.0): 草稿 — 2026-04-30-v1.0-workflow-engine.md
- **notification** (spec 1.0): 已批准 — 2026-05-01-v1.0-notification.md
- **release-readiness** (spec 1.0): approved — 2026-05-05-v1.0-release-readiness.md
- **test-fixes-and-coverage** (spec 1.0): approved — 2026-05-05-v1.0-test-fixes-and-coverage.md
- **mcp-tool-discovery** (implementation 1.0): completed — 2026-05-07-v1.0-mcp-tool-discovery.md
- **ui-alignment** (standard 1.0): approved — 2026-05-07-v1.0-ui-alignment.md
- **specs-index** ( ):  — README.md
- **spec-review** (spec v1.0): 已批准 — SPEC-REVIEW-v1.0.md
- **agent-engine-agentic-gaps** (spec v1.0): implemented — agent-engine-agentic-gaps-2026-05-07.md
- **agent-engine-core-completion** (spec 1.0): approved — agent-engine-core-completion.md
- **agent-engine-p1-p2-p4-batch** (spec v1.0): implemented — agent-engine-p1-p2-p4-batch-2026-05-08.md
- **core-ai-engine** (spec v1.0): draft — core-ai-engine.md
- **project-plan** (plan v1.1): 已批准 — 2026-04-29-v1.0-project-plan.md
- **phase1-observability-foundation** (plan v1.0): approved — 2026-04-30-phase1-observability-foundation.md
- **claude-code-harness-research** (plan v1.0): 草稿 — 2026-04-30-v1.0-claude-code-harness-research.md
- **sprint-plan** (plan v1.0): 草稿 — 2026-04-30-v1.0-sprint-plan.md
- **tech-research** (plan v1.0): 已批准 — 2026-04-30-v1.0-tech-research-plan.md
- **unified-dev-plan** (plan v1.0): 草稿 — 2026-04-30-v1.0-unified-dev-plan.md
- **agent-execution-control-plane** (approved 1.0.0): active — 2026-05-10-agent-execution-control-plane.md
- **plan-review** (plan v1.0): 已批准 — PLAN-REVIEW-v1.0.md
- **plans-index** ( ):  — README.md
- **system-architecture** (design v1.1): 已批准 — 2026-04-29-v1.0-system-architecture.md
- **agent-runtime-task-board** (design v1.0): 草稿 — 2026-04-30-v1.0-agent-runtime-task-board.md
- **workflow-task-orchestration** (design v1.0): 草稿 — 2026-04-30-v1.0-workflow-task-orchestration.md
- **designs-index** ( ):  — README.md
- **core-ai-engine** (design v1.0): draft — core-ai-engine.md
