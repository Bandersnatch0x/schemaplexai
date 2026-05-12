---
topic: agent-execution-control-plane
stage: approved
version: 1.1.0
status: active
---

# SchemaPlexAI v1 实施计划

## 核心产品线（4 步）

```
启动执行 → 观察执行 → 控制执行 → 审计执行
```

不增强这 4 步的功能不进入 v1。

---

## 已完成（MAF Phase 1-3）

| 里程碑 | 状态 | 内容 |
|--------|------|------|
| M8: OpenTelemetry | ✅ | 执行事件 span 导出、TraceQL 查询、PII 脱敏 |
| M9: 工具调用预算 | ✅ | 五维度准入、快照哈希链、Token 估算 |
| M10: 渐进式技能披露 | ✅ | 技能注册、动态加载、版本管理 |
| M11: 中间件管道 | ✅ | 6 层中间件（准入/审批/事件/策略/快照/工具执行） |
| M12: ApprovalMode | ✅ | FAST + BPMN 双模式、SLA 监控、审批流 |
| M13: SSE 前端 | ✅ | 执行事件实时推送、可观测性面板 |
| Bug Fixes | ✅ | Flyway 迁移、NPE、null check、SLA 隔离 |

---

## 基础设施 v1（PG + Redis + RabbitMQ + Flowable）

ClickHouse / Elasticsearch / Milvus / MinIO → v1.1 延期。

---

## 当前 v1 剩余工作

### P0 — 核心闭环（Week 1-2）

| # | 任务 | 模块 | 复杂度 | 说明 |
|---|------|------|--------|------|
| P0-1 | Gateway 路由与鉴权 | gateway | ⭐⭐⭐ | 路由配置、JWT 校验、租户解析、限流中间件 |
| P0-2 | Agent 执行生命周期 API | agent-engine | ⭐⭐⭐ | pause/resume/cancel REST API + 状态持久化 |
| P0-3 | Web 控制器层 | web | ⭐⭐⭐ | ExecutionWebController、ApprovalWebController、SSE 端点 |
| P0-4 | 系统认证与 RBAC | system | ⭐⭐⭐ | AuthService、TenantService、RoleService、权限策略 |

### P1 — 产品能力（Week 2-3）

| # | 任务 | 模块 | 复杂度 | 说明 |
|---|------|------|--------|------|
| P1-1 | 成本追踪与预算 | ops | ⭐⭐⭐ | CostService、BudgetService、BudgetGuard（PG 存储） |
| P1-2 | 审计事件服务 | quality | ⭐⭐ | AuditEventService CRUD、工具注册审批 |
| P1-3 | MQ 消费者 | task | ⭐⭐ | AgentExecuteDispatcher、CostSyncConsumer、NotificationConsumer |
| P1-4 | 工具集成（GitHub/GitLab） | integration | ⭐⭐ | GitHubToolExecutor、GitLabToolExecutor |
| P1-5 | 知识上下文管理 | context | ⭐⭐⭐ | 文档解析、上下文注入（mock 向量检索） |

### P2 — 产品化（Week 3-4）

| # | 任务 | 模块 | 复杂度 | 说明 |
|---|------|------|--------|------|
| P2-1 | 前端执行面板 | ui | ⭐⭐⭐ | 执行列表、详情页、实时事件流、审批操作 |
| P2-2 | Workflow BPMN 集成 | workflow | ⭐⭐⭐ | Flowable 引擎、BPMN 部署、人任务委派 |
| P2-3 | Agent 配置管理 | agent-config | ⭐⭐ | Agent CRUD、模型配置、技能绑定 |
| P2-4 | Spec 规范中心 | spec | ⭐⭐ | OpenAPI spec 管理、版本控制 |

### P3 — 质量与交付（Week 4+）

| # | 任务 | 模块 | 复杂度 | 说明 |
|---|------|------|--------|------|
| P3-1 | 集成测试套件 | all | ⭐⭐⭐ | Testcontainers（PG/Redis/RabbitMQ）端到端测试 |
| P3-2 | Docker Compose 编排 | infra | ⭐⭐ | 一键启动全部服务 + 基础设施 |
| P3-3 | Flyway 迁移完善 | dao | ⭐⭐ | 补全缺失表结构、清理 v1.1 废弃列 |
| P3-4 | API 文档（Knife4j） | web | ⭐ | OpenAPI 注解、接口文档生成 |

---

## 执行策略

1. **接口先行**：先定义 Controller 接口和 DTO，再实现内部逻辑
2. **Mock 替代**：ClickHouse/ES/Milvus 依赖用 mock 或 PG 替代，v1.1 切换
3. **并行推进**：P0 四项可并行（模块独立），P1 依赖 P0 的 Controller 接口
4. **测试驱动**：每个子任务至少 1 个集成测试

---

## 里程碑时间线

```
Week 1: P0-1 Gateway + P0-4 System Auth + P0-3 Web Controllers
Week 2: P0-2 Lifecycle API + P1-1 Cost + P1-3 MQ Consumers
Week 3: P1-2 Audit + P1-4 Integration + P2-1 Frontend
Week 4: P2-2 Workflow + P3-1 Integration Tests + P3-2 Docker
```
