---
title: Knowledge Gaps
type: index
source: wiki gap analysis
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [gaps, questions, todo, undocumented]
confidence: high
---

# Knowledge Gaps

> One-sentence summary: Elements discovered in schema, routes, or code that lack dedicated wiki pages, plus open questions about the codebase.

## Undocumented Schema Elements

| Element | Location | Why It Matters |
|---------|----------|---------------|
| `sf_agent_shadow_config` | 02-init-schema-agent.sql | Self-improvement config — no service/controller explored |
| `sf_config` | 01-init-schema.sql | System configuration table — no ConfigService impl explored |
| `act_*` tables | Flowable auto-DDL | BPMN runtime tables — not in init scripts |
| ClickHouse schema | Not found | Analytics tables for cost tracking |
| Milvus collections | Not found | Vector collection definitions |

## Undocumented Controllers

The following controllers exist but have no dedicated wiki pages (most are standard CRUD):

- `AgentConfigController` — agent config CRUD
- `AuthController` — login/JWT (critical but standard)
- `RagController` — RAG query endpoints
- `ContextController` — context management
- `KnowledgeDocController` — document upload/ingestion
- `WorkspaceController` — workspace CRUD
- `SpecController` — spec CRUD
- `SpecSteeringController` — steering CRUD
- `SpecTemplateController` — template CRUD
- `SpecVersionController` — version CRUD
- `SpecReviewController` — review CRUD
- `WorkflowTemplateController` — template CRUD
- `WorkflowInstanceController` — instance CRUD
- `QualityGateController` — gate CRUD
- `QualityIssueController` — issue CRUD
- `ReviewController` — review CRUD
- `SecurityPolicyController` — policy CRUD
- `AuditEventController` — audit CRUD
- `IntegrationController` — integration CRUD
- `ApiGatewayController` — API gateway config CRUD
- `McpServerController` — MCP server CRUD
- `SkillController` — skill CRUD
- `ArtifactController` — artifact CRUD
- `BudgetController` — budget CRUD
- `EvaluationController` — eval CRUD
- `NotificationController` — notification CRUD
- `CostController` — cost analytics
- `TenantController`, `UserController`, `RoleController`, `PermissionController` — RBAC CRUD
- `AiModelController`, `ModelProviderController` — AI model CRUD
- `ConfigController` — system config

## Undocumented Services

- `AgentRuntimeOrchestrator` — core orchestration (only referenced, not read)
- `AgentExecutionLifecycleService` — pause/resume/cancel logic
- `QualityOrchestrator` — quality gate orchestration
- `FlowableDelegateAdapter` — Flowable integration adapter
- All `*ServiceImpl` classes (implementation details)

## Open Questions

1. **How does JWT authentication work at the Gateway?** — `JwtAuthenticationFilter` file not found/read
2. **What is the complete agent execution flow?** — `AgentRuntimeOrchestrator` not explored
3. **How are Flowable BPMN processes defined and deployed?** — No BPMN files found
4. **What is the ClickHouse schema for cost analytics?** — No init scripts found
5. **How does the RAG embedding pipeline work end-to-end?** — Only service interfaces read
6. **What are the 7 node executor implementations?** — Only registry pattern read
7. **How is tenant data physically isolated?** — `TenantLineInterceptor` not explored
8. **What MQ exchanges and queues are configured?** — RabbitMQ config not explored
9. **What is the frontend page implementation status?** — Page components not read

## Backlinks

- See [[index]] for all documented pages
- See [[technical-debt]] for implementation gaps
- See [[log]] for wiki update history
