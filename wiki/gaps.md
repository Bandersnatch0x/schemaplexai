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
11. **cross-service-integration-tests** — `schemaplexai-agent-engine/src/test/java/.../integration/CrossServiceChainIntegrationTest.java` 与 `TestServiceConfig.java`（commit 554e7cd 提交）引用 `quality.gate.QualityReport`、`workflow.WorkflowInstanceService`、`ops.CostService` 等兄弟模块类型，但 `agent-engine/pom.xml` 未声明这些依赖，且架构上违反 agent-engine ↔ siblings 边界。当前通过 `maven-compiler-plugin testExcludes` 临时排除。**修复方向**：(a) 移到独立 `schemaplexai-integration-tests` 模块并声明所有依赖；或 (b) 拆分成 mock-based 单元测试，断言 agent-engine 自身的 hook 协议而非 sibling 实现。
12. **flaky-retrying-state-handler-test** — `RetryingStateHandlerTest.clearRetryStateShouldRemoveCounters` 单独运行通过，整套 `mvn test` 偶发 Mockito `TooFewActualInvocations` 错误（commit 869d416 引入）。怀疑测试间共享了 `@Mock AgentStateMachine` 的累计调用计数或 `RetryingStateHandler` 内部静态状态未在 `@BeforeEach` 重置。**修复方向**：检查 setup 中是否对 stateMachine 使用 `reset()`，或将 retry counter 改为依赖注入，便于测试隔离。

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
| Open questions | 9/12 resolved | #10 architectural limit; #11–#12 test infrastructure debt |

### What Remains

- **Open Question #10** — Agent-engine SSE event bus is single-node only. Documented architectural limitation (see [[services/execution-event-bus]] and [[wiki/active-areas]]). Requires Redis pub/sub or sticky sessions before multi-node production.
- **Open Question #11** — Cross-service integration tests excluded from build via `testExcludes`. Architectural debt; restoration tracked above.
- **Open Question #12** — `RetryingStateHandlerTest.clearRetryStateShouldRemoveCounters` flaky in full reactor; passes in isolation. Pre-existing from commit 869d416.

### Auto-Generated Gap Scan (Stale — Resync Required)

> The auto-generated section below is from 2026-05-01 and references deleted worktree paths. Many items listed have since been resolved. Run `sync-wiki.sh` to regenerate.
>
> <!-- STALE: sync-wiki.sh at 2026-05-01T18:07:31Z — needs resync -->
