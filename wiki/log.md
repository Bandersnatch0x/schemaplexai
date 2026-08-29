<!-- AUTO-GENERATED: sync-wiki.sh at 2026-08-29T20:04:28Z -->

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

## 2026-08-30 — fix(integration): fail fast when master secret is unset (review ST-04)
66c4c87
## 2026-08-30 — docs(quality): update javadoc after legacy consumer retirement (NEW-03)
ae4db78
## 2026-08-30 — docs(db): clarify sf_spec.version is an optimistic-lock counter (NEW-08)
132308a
## 2026-08-30 — fix(spec): require approved status before publish (NEW-06)
2127a70
## 2026-08-30 — fix(agent-engine): dispatch snapshot persistence through async proxy (NEW-02)
6027601
## 2026-08-30 — fix(workflow): wire post-node quality gate trigger (C4 remainder)
8f62bd5
## 2026-08-30 — fix(agent-engine): wire post-tool quality gate trigger (C4 remainder)
229ef34
## 2026-08-30 — fix(common): centralize sf.quality.verdict routing key (review ST-03)
fc5d1af
## 2026-08-30 — fix(web): align pagination params to API spec current/size defaults (review ST-02)
5c4112a
## 2026-08-30 — fix(ops): tenant-isolate budget alert endpoint and register tenant interceptor (review ST-01)
eb2da55
## 2026-08-30 — fix(task): retire legacy sf.quality consumer superseded by 924 chain (NEW-03)
19470cb
## 2026-08-30 — fix(integration): inject credentials for pull/fetch/push, not just clone (NEW-01)
75f45ff
## 2026-08-30 — fix(integration): implement real MCP client and wire tool discovery into execution chain (issue 930)
811d673
## 2026-08-30 — fix(agent-config): rename cockpit read-only mapper to avoid bean name clash
d40eb89
## 2026-08-30 — fix(task): consume sf.cost cost events and persist to sf_cost_record (issue 919)
1eb82ac
## 2026-08-30 — fix(agent-engine): capture LLM tokenUsage and publish CostRecordedEvent on sf.cost (issue 919)
56d8648
## 2026-08-30 — fix(agent-engine): wire post-execution quality gate trigger and verdict handling (issue 924)
5135dcd
## 2026-08-30 — fix(workflow): instance cancel/approve/reject control plane and Flowable bridge audit (issue 923)
a6a08ec
## 2026-08-30 — fix(quality): wire gate evaluation with default policy and disposition verdicts (issue 924)
91cdfd9
## 2026-08-30 — fix(spec): restore rejected review semantics, add change audit trail and steering prompt fragment (issue 925)
c462839

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
