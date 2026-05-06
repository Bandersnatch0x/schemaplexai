---
title: Knowledge Gaps
type: index
source: wiki gap analysis
creation_date: 2026-04-30
update_date: 2026-05-07
10. ~~**Agent-engine SSE event bus is single-node only**~~ — documented in [[services/execution-event-bus]] and [[wiki/active-areas]] with fix options (sticky sessions or Redis pub/sub bridge)
tags: [gaps, questions, todo, undocumented]
confidence: high
---

# Knowledge Gaps

> One-sentence summary: Elements discovered in schema, routes, or code that lack dedicated wiki pages, plus open questions about the codebase.

## Undocumented Schema Elements

| Element | Location | Why It Matters |
|---------|----------|---------------|
| ~~`sf_agent_shadow_config`~~ | 02-init-schema-agent.sql | Self-improvement config — `AgentShadowConfigService` + controller + tests implemented |
| ~~`sf_tenant_environment_config`~~ | Entity defined 2026-05-04 | Tenant env security config — `TenantEnvironmentConfigService` + controller + tests + wiki implemented |
| ~~`sf_config`~~ | 01-init-schema.sql | System configuration table — `ConfigService` (system) + `SystemConfigService` (admin) implemented with feature flags and maintenance mode |
| `act_*` tables | Flowable auto-DDL | BPMN runtime tables — not in init scripts |
| ~~ClickHouse schema~~ | `docker/clickhouse/init/01-cost-analytics.sql` | 4 tables + 2 materialized views created 2026-05-06 |
| ~~Milvus collections~~ | `schemaplexai-context/src/main/resources/milvus/` | `knowledge_doc_embedding` collection schema with IVF_FLAT/COSINE index |

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

- ~~`AgentRuntimeOrchestrator`~~ — documented in [[services/agent-runtime-orchestrator]]
- ~~`AgentExecutionLifecycleService`~~ — documented in [[services/agent-execution-lifecycle-service]]
- ~~`ToolRegistry`~~ — documented in [[services/tool-registry]]
- ~~`SecurityPolicyLoader`~~ — documented in [[services/security-policy-loader]]
- ~~`ToolExecutionMetricsBinder`~~ — documented in [[services/tool-execution-metrics-binder]]
- ~~`RetryingStateHandler`~~ — documented in [[services/retrying-state-handler]]
- ~~`ResumingStateHandler`~~ — documented in [[services/resuming-state-handler]]
- ~~`QualityOrchestrator`~~ — implemented with rule registry, evaluation pipeline, and 10 unit tests
- ~~`FlowableDelegateAdapter`~~ — implemented as JavaDelegate bridge to WorkflowNodeEngine
- ~~All major Service interfaces~~ — 67 service wiki pages created covering all modules (agent-config, agent-engine, context, integration, ops, quality, spec, system, task, workflow, web)
- All `*ServiceImpl` classes (implementation details)

## Open Questions

1. ~~**How does JWT authentication work at the Gateway?**~~ — documented in [[services/jwt-auth-filter]] and [[services/auth-service]]
2. ~~**What is the complete agent execution flow?**~~ — documented in [[services/agent-runtime-orchestrator]] and [[services/agent-state-machine]]
3. ~~**How are Flowable BPMN processes defined and deployed?**~~ — 2 BPMN files exist (`ai-agent-execution.bpmn20.xml`, `spec-review-approval.bpmn20.xml`), 6 delegates implemented
4. ~~**What is the ClickHouse schema for cost analytics?**~~ — `docker/clickhouse/init/01-cost-analytics.sql` created with 4 tables + 2 materialized views
5. ~~**How does the RAG embedding pipeline work end-to-end?**~~ — Full pipeline implemented: `DocumentChunker` → `EmbeddingService` → `MilvusSyncServiceImpl` → `MilvusClientV2.insert`
6. ~~**What are the 7 node executor implementations?**~~ — 7 executors implemented: HTTP, SCRIPT, START, END, AI_MODEL, TOOL_CALL, CONDITION
7. ~~**How is tenant data physically isolated?**~~ — documented in [[services/tenant-line-interceptor]]
8. ~~**What MQ exchanges and queues are configured?**~~ — documented in [[services/rabbitmq-messaging]]
9. ~~**What is the frontend page implementation status?**~~ — 18 page directories exist covering all major modules (Agent, Context, Integration, Workflow, Quality, Ops, Spec, System, Notification, Dashboard, Login, Cockpit)
10. **Agent-engine SSE event bus is single-node only** — `ExecutionEventBus` holds SSE emitters in a local `ConcurrentHashMap`. If an execution runs on Node A and a user connects to Node B via load balancer, Node B's event bus has no emitters for that execution and the subscriber receives nothing. Acceptable for MVP/single-node, but a blocker for horizontal scaling. Potential fixes: (a) load-balancer sticky sessions routing by `executionId`, or (b) Redis pub/sub bridge to fan out events across nodes.

## Backlinks

- See [[index]] for all documented pages
- See [[technical-debt]] for implementation gaps
- See [[log]] for wiki update history

## Auto-Generated Gap Scan

<!-- AUTO-GENERATED: sync-wiki.sh at 2026-05-01T18:07:31Z -->

### Undocumented Entities

- Missing wiki page for entity: `Notification` (source: D:/code_space/frige/schemaplexai-model/src/main/java/com/schemaplexai/model/entity/notification/Notification.java)
- Missing wiki page for entity: `ObservabilitySpan` (source: D:/code_space/frige/schemaplexai-model/src/main/java/com/schemaplexai/model/entity/observability/ObservabilitySpan.java)
- Missing wiki page for entity: `ObservabilityTrace` (source: D:/code_space/frige/schemaplexai-model/src/main/java/com/schemaplexai/model/entity/observability/ObservabilityTrace.java)

### Undocumented Controllers

- Missing wiki page for controller: `BaseController` (source: D:/code_space/frige/schemaplexai-web/src/main/java/com/schemaplexai/web/controller/BaseController.java)
- Missing wiki page for controller: `NotificationController` (source: D:/code_space/frige/schemaplexai-web/src/main/java/com/schemaplexai/web/controller/notification/NotificationController.java)

### Undocumented Services

- Missing wiki page for service: `AgentConfigService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/AgentConfigService.java)
- Missing wiki page for service: `PromptVersionService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/PromptVersionService.java)
- Missing wiki page for service: `ShadowConfigService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/ShadowConfigService.java)
- Missing wiki page for service: `AgentLoopDetectionService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/loop/AgentLoopDetectionService.java)
- Missing wiki page for service: `AgentLoopShadowReviewService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/shadow/AgentLoopShadowReviewService.java)
- Missing wiki page for service: `ContextService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-context/src/main/java/com/schemaplexai/context/service/ContextService.java)
- Missing wiki page for service: `ContextSnapshotService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-context/src/main/java/com/schemaplexai/context/service/ContextSnapshotService.java)
- Missing wiki page for service: `KnowledgeDocService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-context/src/main/java/com/schemaplexai/context/service/KnowledgeDocService.java)
- Missing wiki page for service: `MilvusSyncService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-context/src/main/java/com/schemaplexai/context/service/MilvusSyncService.java)
- Missing wiki page for service: `WorkspaceService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-context/src/main/java/com/schemaplexai/context/service/WorkspaceService.java)
- Missing wiki page for service: `ApiGatewayService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/ApiGatewayService.java)
- Missing wiki page for service: `GitIntegrationService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/GitIntegrationService.java)
- Missing wiki page for service: `IntegrationService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/IntegrationService.java)
- Missing wiki page for service: `JenkinsIntegrationService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/JenkinsIntegrationService.java)
- Missing wiki page for service: `McpServerService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/McpServerService.java)
- Missing wiki page for service: `SkillService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/SkillService.java)
- Missing wiki page for service: `ToolExecutionService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/ToolExecutionService.java)
- Missing wiki page for service: `ArtifactService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/ArtifactService.java)
- Missing wiki page for service: `BudgetService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/BudgetService.java)
- Missing wiki page for service: `ClickHouseCostSyncService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/ClickHouseCostSyncService.java)
- Missing wiki page for service: `CostService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/CostService.java)
- Missing wiki page for service: `DeliveryService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/DeliveryService.java)
- Missing wiki page for service: `EvaluationService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/EvaluationService.java)
- Missing wiki page for service: `NotificationService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/NotificationService.java)
- Missing wiki page for service: `AuditEventService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/AuditEventService.java)
- Missing wiki page for service: `QualityGateService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/QualityGateService.java)
- Missing wiki page for service: `QualityIssueService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/QualityIssueService.java)
- Missing wiki page for service: `ReviewService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/ReviewService.java)
- Missing wiki page for service: `SecurityPolicyService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/SecurityPolicyService.java)
- Missing wiki page for service: `ToolApprovalService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/ToolApprovalService.java)
- Missing wiki page for service: `SpecReviewService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecReviewService.java)
- Missing wiki page for service: `SpecService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecService.java)
- Missing wiki page for service: `SpecSteeringService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecSteeringService.java)
- Missing wiki page for service: `SpecTemplateService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecTemplateService.java)
- Missing wiki page for service: `SpecVersionService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecVersionService.java)
- Missing wiki page for service: `AiModelService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-system/src/main/java/com/schemaplexai/system/service/AiModelService.java)
- Missing wiki page for service: `ConfigService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-system/src/main/java/com/schemaplexai/system/service/ConfigService.java)
- Missing wiki page for service: `ModelProviderService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-system/src/main/java/com/schemaplexai/system/service/ModelProviderService.java)
- Missing wiki page for service: `PermissionService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-system/src/main/java/com/schemaplexai/system/service/PermissionService.java)
- Missing wiki page for service: `RoleService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-system/src/main/java/com/schemaplexai/system/service/RoleService.java)
- Missing wiki page for service: `TenantService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-system/src/main/java/com/schemaplexai/system/service/TenantService.java)
- Missing wiki page for service: `UserService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-system/src/main/java/com/schemaplexai/system/service/UserService.java)
- Missing wiki page for service: `MessageFailLogService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-task/src/main/java/com/schemaplexai/task/mq/MessageFailLogService.java)
- Missing wiki page for service: `WorkflowInstanceService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/service/WorkflowInstanceService.java)
- Missing wiki page for service: `WorkflowTemplateService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/service/WorkflowTemplateService.java)
- Missing wiki page for service: `AgentConfigService` (source: D:/code_space/frige/schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/AgentConfigService.java)
- Missing wiki page for service: `PromptVersionService` (source: D:/code_space/frige/schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/PromptVersionService.java)
- Missing wiki page for service: `ShadowConfigService` (source: D:/code_space/frige/schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/ShadowConfigService.java)
- Missing wiki page for service: `AgentLoopDetectionService` (source: D:/code_space/frige/schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/loop/AgentLoopDetectionService.java)
- Missing wiki page for service: `AgentLoopShadowReviewService` (source: D:/code_space/frige/schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/shadow/AgentLoopShadowReviewService.java)
- Missing wiki page for service: `ContextService` (source: D:/code_space/frige/schemaplexai-context/src/main/java/com/schemaplexai/context/service/ContextService.java)
- Missing wiki page for service: `ContextSnapshotService` (source: D:/code_space/frige/schemaplexai-context/src/main/java/com/schemaplexai/context/service/ContextSnapshotService.java)
- Missing wiki page for service: `KnowledgeDocService` (source: D:/code_space/frige/schemaplexai-context/src/main/java/com/schemaplexai/context/service/KnowledgeDocService.java)
- Missing wiki page for service: `MilvusSyncService` (source: D:/code_space/frige/schemaplexai-context/src/main/java/com/schemaplexai/context/service/MilvusSyncService.java)
- Missing wiki page for service: `WorkspaceService` (source: D:/code_space/frige/schemaplexai-context/src/main/java/com/schemaplexai/context/service/WorkspaceService.java)
- Missing wiki page for service: `ApiGatewayService` (source: D:/code_space/frige/schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/ApiGatewayService.java)
- Missing wiki page for service: `GitIntegrationService` (source: D:/code_space/frige/schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/GitIntegrationService.java)
- Missing wiki page for service: `IntegrationService` (source: D:/code_space/frige/schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/IntegrationService.java)
- Missing wiki page for service: `JenkinsIntegrationService` (source: D:/code_space/frige/schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/JenkinsIntegrationService.java)
- Missing wiki page for service: `McpServerService` (source: D:/code_space/frige/schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/McpServerService.java)
- Missing wiki page for service: `SkillService` (source: D:/code_space/frige/schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/SkillService.java)
- Missing wiki page for service: `ToolExecutionService` (source: D:/code_space/frige/schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/ToolExecutionService.java)
- Missing wiki page for service: `ArtifactService` (source: D:/code_space/frige/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/ArtifactService.java)
- Missing wiki page for service: `BudgetService` (source: D:/code_space/frige/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/BudgetService.java)
- Missing wiki page for service: `ClickHouseCostSyncService` (source: D:/code_space/frige/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/ClickHouseCostSyncService.java)
- Missing wiki page for service: `CostService` (source: D:/code_space/frige/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/CostService.java)
- Missing wiki page for service: `DeliveryService` (source: D:/code_space/frige/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/DeliveryService.java)
- Missing wiki page for service: `EvaluationService` (source: D:/code_space/frige/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/EvaluationService.java)
- Missing wiki page for service: `NotificationService` (source: D:/code_space/frige/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/NotificationService.java)
- Missing wiki page for service: `AuditEventService` (source: D:/code_space/frige/schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/AuditEventService.java)
- Missing wiki page for service: `QualityGateService` (source: D:/code_space/frige/schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/QualityGateService.java)
- Missing wiki page for service: `QualityIssueService` (source: D:/code_space/frige/schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/QualityIssueService.java)
- Missing wiki page for service: `ReviewService` (source: D:/code_space/frige/schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/ReviewService.java)
- Missing wiki page for service: `SecurityPolicyService` (source: D:/code_space/frige/schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/SecurityPolicyService.java)
- Missing wiki page for service: `ToolApprovalService` (source: D:/code_space/frige/schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/ToolApprovalService.java)
- Missing wiki page for service: `SpecReviewService` (source: D:/code_space/frige/schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecReviewService.java)
- Missing wiki page for service: `SpecService` (source: D:/code_space/frige/schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecService.java)
- Missing wiki page for service: `SpecSteeringService` (source: D:/code_space/frige/schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecSteeringService.java)
- Missing wiki page for service: `SpecTemplateService` (source: D:/code_space/frige/schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecTemplateService.java)
- Missing wiki page for service: `SpecVersionService` (source: D:/code_space/frige/schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/SpecVersionService.java)
- Missing wiki page for service: `AiModelService` (source: D:/code_space/frige/schemaplexai-system/src/main/java/com/schemaplexai/system/service/AiModelService.java)
- Missing wiki page for service: `ConfigService` (source: D:/code_space/frige/schemaplexai-system/src/main/java/com/schemaplexai/system/service/ConfigService.java)
- Missing wiki page for service: `ModelProviderService` (source: D:/code_space/frige/schemaplexai-system/src/main/java/com/schemaplexai/system/service/ModelProviderService.java)
- Missing wiki page for service: `PermissionService` (source: D:/code_space/frige/schemaplexai-system/src/main/java/com/schemaplexai/system/service/PermissionService.java)
- Missing wiki page for service: `RoleService` (source: D:/code_space/frige/schemaplexai-system/src/main/java/com/schemaplexai/system/service/RoleService.java)
- Missing wiki page for service: `TenantService` (source: D:/code_space/frige/schemaplexai-system/src/main/java/com/schemaplexai/system/service/TenantService.java)
- Missing wiki page for service: `UserService` (source: D:/code_space/frige/schemaplexai-system/src/main/java/com/schemaplexai/system/service/UserService.java)
- Missing wiki page for service: `MessageFailLogService` (source: D:/code_space/frige/schemaplexai-task/src/main/java/com/schemaplexai/task/mq/MessageFailLogService.java)
- Missing wiki page for service: `NotificationService` (source: D:/code_space/frige/schemaplexai-web/src/main/java/com/schemaplexai/web/service/notification/NotificationService.java)
- Missing wiki page for service: `WorkflowInstanceService` (source: D:/code_space/frige/schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/service/WorkflowInstanceService.java)
- Missing wiki page for service: `WorkflowTemplateService` (source: D:/code_space/frige/schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/service/WorkflowTemplateService.java)
