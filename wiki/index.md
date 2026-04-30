---
title: SchemaPlexAI Wiki Index
type: index
source: wiki/
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [wiki, index, schemaplexai]
confidence: high
---

# SchemaPlexAI Wiki Index

> One-sentence summary: Central index for the SchemaPlexAI LLM Wiki — a self-maintaining knowledge base for Claude Code development assistance.

## Data Layer

- [[data-model]] — Database schema overview, all entities, and Mermaid ER diagram
- [[schema-evolution]] — Key schema changes and migration rationale
- **Entities**
  - [[entities/base-entity]] — BaseEntity (all entities extend this)
  - [[entities/tenant]] — sf_tenant (global table, no tenant isolation)
  - [[entities/user]] — sf_user + sf_role + sf_permission (RBAC)
  - [[entities/ai-model]] — sf_ai_model + sf_model_provider
  - [[entities/agent]] — sf_agent + sf_agent_config + sf_agent_execution
  - [[entities/chat-message]] — sf_chat_message (hash-partitioned by conversation_id, 16 partitions)
  - [[entities/spec]] — sf_spec + sf_spec_document + sf_spec_version + sf_spec_template
  - [[entities/workflow]] — sf_workflow_template + sf_workflow_instance + sf_workflow_node_execution
  - [[entities/context]] — sf_workspace + sf_context + sf_context_item + sf_knowledge_doc
  - [[entities/quality]] — sf_quality_gate + sf_quality_issue + sf_security_policy + sf_audit_event
  - [[entities/integration]] — sf_integration + sf_skill + sf_mcp_server
  - [[entities/ops]] — sf_artifact + sf_delivery_record + sf_notification + sf_budget + sf_eval_dataset
  - [[entities/sync]] — sf_sync_cursor + sf_sync_batch_log + sf_idempotency_key + sf_message_fail_log

## API & Routing

- [[routes]] — Gateway routes, frontend routes, and auth requirements
- **Controllers**
  - [[controllers/agent-execution-controller]] — Agent execution lifecycle (start/pause/resume/cancel/snapshot)
  - [[controllers/sse-controller]] — SSE subscription and event push
  - [[controllers/system-controllers]] — Auth, tenant, user, role, permission, AI model

## Architecture & Patterns

- [[architecture]] — Service topology, module dependencies, communication patterns
- [[dependencies]] — Full tech stack and library versions
- [[decisions]] — Architectural Decision Records (ADRs)
- [[services/agent-execution-engine]] — Core agent execution engine
- [[services/workflow-node-engine]] — Flowable BPMN + AI node engine
- [[services/rag-service]] — RAG pipeline and Milvus integration

## Frontend

- [[frontend/structure]] — React + Vite project structure, routing, state management

## Project Management

- [[plans-and-initiatives]] — Active plans grouped by status and priority
- [[technical-debt]] — Known technical debt and deferred tasks
- [[roadmap]] — Overall development vision
- [[active-areas]] — Current development focus based on recent activity

### Reference Documentation (docs/)

| Document | Path | Description |
|----------|------|-------------|
| System Architecture | `docs/designs/system-architecture.md` | v1.1 架构设计文档 |
| Agent Runtime + Task Board | `docs/designs/agent-runtime-task-board.md` | v1.0 Agent Runtime Platform 设计 |
| Project Plan | `docs/plans/project-plan.md` | v1.1 30 周修订计划 |
| Unified Dev Plan | `docs/plans/unified-dev-plan.md` | v1.0 47 任务实施计划 |
| SDD Process | `docs/standards/sdd-process.md` | 规格驱动开发流程规范 |
| TDD Guide | `docs/standards/tdd-guide.md` | 测试驱动开发指南 |
| Document Template | `docs/DOCUMENT-TEMPLATE.md` | 活跃文档标准模板 |
| AGENTS.md | `AGENTS.md` | Agent 开发详细指南 |
| README.md | `README.md` | 快速开始、服务列表、访问地址 |

## Ideas & Research

- [[ideas/README]] — Ideas collection process and status definitions
- [[ideas/idea-template]] — Template for recording new ideas
- **Raw Ideas** — captured idea files following `YYYYMMDD-*.md` naming

## System

- [[gaps]] — Undocumented elements, open questions, and knowledge gaps
- [[log]] — Wiki operation log (updates, discoveries, triggers)

---

> Always check wiki/ before answering questions about this project's architecture, patterns, or decisions.
