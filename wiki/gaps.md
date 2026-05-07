<!-- AUTO-GENERATED: sync-wiki.sh at 2026-05-07T07:47:42Z -->
---
title: Knowledge Gaps
type: index
source: wiki gap analysis
creation_date: 2026-04-30
update_date: 2026-05-07
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
| ~~`act_*` tables~~ | Flowable auto-DDL | BPMN runtime tables — documented in [[schema/flowable-runtime-tables]] |
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
9. ~~**What is the frontend page implementation status?**~~ — All 16 route-level pages implemented and registered in router: Dashboard, Cockpit, AgentManager, AgentExecutor, AgentDetail, AgentCanvas, SpecCenter, WorkflowCenter, WorkflowMonitor, ContextCenter, QualityCenter, IntegrationCenter, OpsCenter, SystemSettings, NotificationCenter, NotFound (+ Login). Router config at `schemaplexai-ui/src/router/index.tsx`.
10. **Agent-engine SSE event bus is single-node only** — `ExecutionEventBus` holds SSE emitters in a local `ConcurrentHashMap`. If an execution runs on Node A and a user connects to Node B via load balancer, Node B's event bus has no emitters for that execution and the subscriber receives nothing. Acceptable for MVP/single-node, but a blocker for horizontal scaling. Potential fixes: (a) load-balancer sticky sessions routing by `executionId`, or (b) Redis pub/sub bridge to fan out events across nodes.
11. ~~**cross-service-integration-tests**~~ — 已移除 `CrossServiceChainIntegrationTest.java` 与 `TestServiceConfig.java`（commit 104e170）。这些测试架构上违反 agent-engine ↔ siblings 边界（引用 `quality.gate.QualityReport`、`workflow.WorkflowInstanceService`、`ops.CostService` 等未声明依赖的模块类型），且已被 `testExcludes` 长期排除不编译。若需恢复跨服务集成测试，应在独立 `schemaplexai-integration-tests` 模块中声明全部依赖后重建。
12. ~~**flaky-retrying-state-handler-test**~~ — 已修复（commit 104e170）。在 `RetryingStateHandlerTest.clearRetryStateShouldRemoveCounters` 中 `clearRetryState(1L)` 后添加 `Mockito.reset(stateMachine)`，隔离第二个 execution 的 verify 不受第一个 execution 累积调用的影响。连续 5 次 `mvn test` 验证稳定通过。
13. **Milvus consistency_level 未配置 — 多 Agent 写后读空风险** — 整个代码库未设置 `consistency_level`，Collection 创建和所有 search 调用均使用 Milvus 默认的 `Bounded` 级别。在多 Agent 事件驱动场景下（Writer Agent 写入向量 → Reader Agent 立即检索），Bounded 的 5 秒可见性窗口会导致 Reader 返回空结果。受影响文件：`MilvusCollectionInitializer.java`（Collection 创建）、`RagSearchServiceImpl.java`（search 调用）、`MilvusIsolationService.java`（search 调用）、`MilvusClient` 接口（缺少一致性参数）。修复方案：在 `MilvusProperties` 增加 `consistencyLevel` 配置项，搜索调用时传入 `ConsistencyLevel.STRONG`。当前为潜在风险，多 Agent 编排落地前必须修复。
14. **Tool-call 无预算限制 — 循环内无限调用风险** — `ToolCallingStateHandler` 中没有 per-execution / per-tenant 的 tool call 次数限制。虽然 orchestrator 有 50 次迭代上限，但单次 iteration 内可触发多个 tool call。一个陷入循环的 Agent 可以在短时间内对同一工具发起数百次调用，造成资源耗尽和供应商账单爆炸。修复方案：在 `ToolCallingStateHandler` 中加入 `ToolCallBudget` 计数器（per execution, per tenant），超限后 transition 到 FAILED。与现有 `TokenBudget` 类似但针对调用次数。
15. **Agent 决策无审计日志** — `ToolExecutionRecorder` 记录了工具执行结果，但没有记录 Agent *为什么* 选择调用该工具（即决策 provenance）。缺少推理链的审计追踪，无法满足企业合规要求。修复方案：在 `ThinkingStateHandler` 中记录 LLM 的 reasoning/chain-of-thought 到 `AgentDecisionLog` 表，关联到 executionId 和 tool call。
16. **OpenTelemetry 缺失 — 自定义 trace 不可观测** — `ObservabilityRecorder` 将 trace/span 写入自定义 PostgreSQL 表，无法与标准观测栈（Jaeger、Grafana Tempo、Datadog）集成。12 个微服务之间无法做分布式追踪关联。修复方案：引入 `io.opentelemetry` SDK，替换自定义 trace 存储，保留 `PiiRedactor` 作为 `SpanProcessor`。

## Backlinks

- See [[index]] for all documented pages
- See [[technical-debt]] for implementation gaps
- See [[log]] for wiki update history

## Current Status

**All core documentation gaps are closed as of 2026-05-07.**

| Category | Count | Status |
|----------|-------|--------|
| Schema elements | 6/6 | All documented |
| Controllers | 34/34 | All have wiki pages |
| Service interfaces | 68/68 | All have wiki pages |
| Open questions | 11/16 resolved | #10 SSE single-node, #13-16 MAF roundtable findings |

### What Remains

- **Open Question #10** — Agent-engine SSE event bus is single-node only. Documented architectural limitation (see [[services/execution-event-bus]] and [[wiki/active-areas]]). Requires Redis pub/sub or sticky sessions before multi-node production.
- **Open Question #11** — Cross-service integration tests excluded from build via `testExcludes`. Architectural debt; restoration tracked above.
- **Open Question #12** — `RetryingStateHandlerTest.clearRetryStateShouldRemoveCounters` flaky in full reactor; passes in isolation. Pre-existing from commit 869d416.
- **Open Question #13** — Milvus `consistency_level` 未配置。多 Agent 写后读场景下 Bounded 默认级别会导致查询空结果，需在多 Agent 编排落地前修复。
- **Open Question #14** — Tool-call 无预算限制。单次 iteration 内可触发无限 tool call，需增加 per-execution budget。
- **Open Question #15** — Agent 决策无审计日志。无 reasoning provenance，无法满足企业合规。
- **Open Question #16** — OpenTelemetry 缺失。自定义 trace 不可与标准观测栈集成。

### Auto-Generated Gap Scan (Stale — Resync Required)

> The auto-generated section below is from 2026-05-01 and references deleted worktree paths. Many items listed have since been resolved. Run `sync-wiki.sh` to regenerate.
>
> <!-- STALE: sync-wiki.sh at 2026-05-01T18:07:31Z — needs resync -->

## Auto-Generated Gap Scan

## Auto-Generated Gap Scan

## Auto-Generated Gap Scan

## Auto-Generated Gap Scan

## Auto-Generated Gap Scan

## Auto-Generated Gap Scan

### Undocumented Entities

- Missing wiki page for entity: `SfAgentShadowConfig` (source: D:/code_space/frige/schemaplexai-model/src/main/java/com/schemaplexai/model/entity/agent/SfAgentShadowConfig.java)
- Missing wiki page for entity: `TenantEnvironmentConfig` (source: D:/code_space/frige/schemaplexai-model/src/main/java/com/schemaplexai/model/entity/config/TenantEnvironmentConfig.java)
- Missing wiki page for entity: `BaseEntityTest` (source: D:/code_space/frige/schemaplexai-model/src/test/java/com/schemaplexai/model/entity/BaseEntityTest.java)

### Undocumented Controllers

- Missing wiki page for controller: `BaseController` (source: D:/code_space/frige/schemaplexai-web/src/main/java/com/schemaplexai/web/controller/BaseController.java)
- Missing wiki page for controller: `TenantEnvironmentConfigController` (source: D:/code_space/frige/schemaplexai-web/src/main/java/com/schemaplexai/web/controller/config/TenantEnvironmentConfigController.java)

### Undocumented Services

- Missing wiki page for service: `AuditLogService` (source: D:/code_space/frige/.claude/worktrees/agent-a89981c3/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/AuditLogService.java)
- Missing wiki page for service: `PlatformHealthService` (source: D:/code_space/frige/.claude/worktrees/agent-a89981c3/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/PlatformHealthService.java)
- Missing wiki page for service: `RoleAdminService` (source: D:/code_space/frige/.claude/worktrees/agent-a89981c3/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/RoleAdminService.java)
- Missing wiki page for service: `SystemConfigService` (source: D:/code_space/frige/.claude/worktrees/agent-a89981c3/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/SystemConfigService.java)
- Missing wiki page for service: `TenantAdminService` (source: D:/code_space/frige/.claude/worktrees/agent-a89981c3/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/TenantAdminService.java)
- Missing wiki page for service: `UserAdminService` (source: D:/code_space/frige/.claude/worktrees/agent-a89981c3/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/UserAdminService.java)
- Missing wiki page for service: `MilvusIsolationService` (source: D:/code_space/frige/.claude/worktrees/agent-a89981c3/schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/rag/MilvusIsolationService.java)
- Missing wiki page for service: `ClickHouseCostSyncService` (source: D:/code_space/frige/.claude/worktrees/agent-a89981c3/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/ClickHouseCostSyncService.java)
- Missing wiki page for service: `WorkflowDeployService` (source: D:/code_space/frige/.claude/worktrees/agent-a89981c3/schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/service/WorkflowDeployService.java)
- Missing wiki page for service: `AuditLogService` (source: D:/code_space/frige/.claude/worktrees/agent-ae0545fd/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/AuditLogService.java)
- Missing wiki page for service: `PlatformHealthService` (source: D:/code_space/frige/.claude/worktrees/agent-ae0545fd/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/PlatformHealthService.java)
- Missing wiki page for service: `RoleAdminService` (source: D:/code_space/frige/.claude/worktrees/agent-ae0545fd/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/RoleAdminService.java)
- Missing wiki page for service: `SystemConfigService` (source: D:/code_space/frige/.claude/worktrees/agent-ae0545fd/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/SystemConfigService.java)
- Missing wiki page for service: `TenantAdminService` (source: D:/code_space/frige/.claude/worktrees/agent-ae0545fd/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/TenantAdminService.java)
- Missing wiki page for service: `UserAdminService` (source: D:/code_space/frige/.claude/worktrees/agent-ae0545fd/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/UserAdminService.java)
- Missing wiki page for service: `MilvusIsolationService` (source: D:/code_space/frige/.claude/worktrees/agent-ae0545fd/schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/rag/MilvusIsolationService.java)
- Missing wiki page for service: `ClickHouseCostSyncService` (source: D:/code_space/frige/.claude/worktrees/agent-ae0545fd/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/ClickHouseCostSyncService.java)
- Missing wiki page for service: `WorkflowDeployService` (source: D:/code_space/frige/.claude/worktrees/agent-ae0545fd/schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/service/WorkflowDeployService.java)
- Missing wiki page for service: `ClickHouseCostSyncService` (source: D:/code_space/frige/.worktrees/phase1-observability-foundation/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/ClickHouseCostSyncService.java)
- Missing wiki page for service: `AuditLogService` (source: D:/code_space/frige/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/AuditLogService.java)
- Missing wiki page for service: `PlatformHealthService` (source: D:/code_space/frige/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/PlatformHealthService.java)
- Missing wiki page for service: `RoleAdminService` (source: D:/code_space/frige/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/RoleAdminService.java)
- Missing wiki page for service: `SystemConfigService` (source: D:/code_space/frige/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/SystemConfigService.java)
- Missing wiki page for service: `TenantAdminService` (source: D:/code_space/frige/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/TenantAdminService.java)
- Missing wiki page for service: `UserAdminService` (source: D:/code_space/frige/schemaplexai-admin/src/main/java/com/schemaplexai/admin/service/UserAdminService.java)
- Missing wiki page for service: `AgentShadowConfigService` (source: D:/code_space/frige/schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/AgentShadowConfigService.java)
- Missing wiki page for service: `MilvusIsolationService` (source: D:/code_space/frige/schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/rag/MilvusIsolationService.java)
- Missing wiki page for service: `ClickHouseCostSyncService` (source: D:/code_space/frige/schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/ClickHouseCostSyncService.java)
- Missing wiki page for service: `WorkflowDeployService` (source: D:/code_space/frige/schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/service/WorkflowDeployService.java)
