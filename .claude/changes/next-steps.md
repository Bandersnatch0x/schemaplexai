---
topic: agent-execution-control-plane
stage: approved
version: 1.1.0
status: active
---

# SchemaPlexAI v1 下一步行动清单

> 基于 PLAN.md 生成，所有任务按优先级和依赖关系排列。

---

## P0-1: Gateway 路由与鉴权 (`schemaplexai-gateway`)

### 依赖
无（可立即启动）

### 复杂度
⭐⭐⭐ 中等 | 预估 5-7 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P0-1.1 | 路由配置 DSL 定义 | ⭐⭐ | 1d | YAML 路由表格式：path → target service mapping |
| P0-1.2 | 请求转发核心逻辑 | ⭐⭐⭐ | 2d | Spring Cloud Gateway 路由过滤器、超时/重试配置 |
| P0-1.3 | JWT 校验过滤器 | ⭐⭐ | 1d | Token 解析 + 租户 ID 提取 + 请求头注入 |
| P0-1.4 | 租户解析中间件 | ⭐⭐ | 0.5d | 从 JWT/Tenant-Key header 提取租户上下文 |
| P0-1.5 | 限流中间件 | ⭐⭐ | 1d | Redis 令牌桶 / 滑动窗口限流 |
| P0-1.6 | 路由集成测试 | ⭐⭐ | 1d | Testcontainers 模拟下游服务 |

### 验收标准
- [ ] Gateway 能转发请求到 agent-engine / web / system 模块
- [ ] JWT 校验失败返回 401
- [ ] 限流超阈值返回 429
- [ ] 租户上下文自动注入请求头

---

## P0-2: Agent 执行生命周期 API (`schemaplexai-agent-engine`)

### 依赖
无（内部已有 StateMachine + LifecycleService）

### 复杂度
⭐⭐⭐ 中高 | 预估 7-10 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P0-2.1 | 暂停信号注入 | ⭐⭐⭐ | 2d | 在 AgentLoop 检查点插入 PAUSE 信号 |
| P0-2.2 | 快照序列化/反序列化 | ⭐⭐⭐⭐ | 2.5d | ExecutionSnapshot 完整性保证（LLM 历史+工具状态+内存） |
| P0-2.3 | 恢复流程（状态重建） | ⭐⭐⭐⭐ | 2d | 从快照重建 AgentContext + StateMachine |
| P0-2.4 | 终止流程（资源清理） | ⭐⭐ | 1d | 强制停止 + sandbox 资源释放 + MQ 通知 |
| P0-2.5 | 状态持久化（Redis + PG） | ⭐⭐⭐ | 1.5d | 执行状态双写 + 快照完整性校验 |
| P0-2.6 | REST API 层 | ⭐⭐ | 1d | POST /executions/{id}/pause /resume /cancel |
| P0-2.7 | 并发安全测试 | ⭐⭐⭐ | 1d | 多线程暂停/恢复竞态测试 |

### 验收标准
- [ ] 可通过 API 暂停/恢复/终止执行
- [ ] 恢复后执行状态与暂停前一致
- [ ] 终止后 sandbox 资源完全释放
- [ ] 并发操作无竞态条件

---

## P0-3: Web 控制器层 (`schemaplexai-web`)

### 依赖
无（Controller 接口可先定义）

### 复杂度
⭐⭐⭐ 中等 | 预估 5-7 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P0-3.1 | ExecutionWebController | ⭐⭐ | 1d | 执行 CRUD + 状态查询 API |
| P0-3.2 | ApprovalWebController | ⭐⭐ | 1d | 审批请求列表 + 审批/拒绝操作 |
| P0-3.3 | SseController | ⭐⭐⭐ | 1.5d | SSE 端点：执行事件流推送 |
| P0-3.4 | DTO / VO 映射 | ⭐⭐ | 1d | 请求/响应对象定义 + MapStruct 映射 |
| P0-3.5 | Knife4j API 文档注解 | ⭐ | 0.5d | OpenAPI 3 注解 + 接口分组 |
| P0-3.6 | 全局异常处理 | ⭐⭐ | 0.5d | @ControllerAdvice + 统一错误响应 |
| P0-3.7 | Controller 集成测试 | ⭐⭐ | 1d | MockMvc + 完整请求链路测试 |

### 验收标准
- [ ] 所有 API 返回标准 JSON 响应
- [ ] SSE 端点能推送实时执行事件
- [ ] API 文档可通过 /doc.html 访问
- [ ] 异常响应格式统一

---

## P0-4: 系统认证与 RBAC (`schemaplexai-system`)

### 依赖
无（可与 P0-1 Gateway 并行）

### 复杂度
⭐⭐⭐ 中等 | 预估 5-7 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P0-4.1 | AuthService（JWT 签发/校验） | ⭐⭐⭐ | 2d | 登录 → JWT 签发 + 刷新 + 校验 |
| P0-4.2 | TenantService | ⭐⭐ | 1d | 租户 CRUD + 租户上下文隔离 |
| P0-4.3 | UserService | ⭐⭐ | 1d | 用户 CRUD + 密码加密 |
| P0-4.4 | RoleService + 权限模型 | ⭐⭐⭐ | 1.5d | RBAC 角色-权限-资源模型 |
| P0-4.5 | TenantPolicyService | ⭐⭐ | 1d | 租户级策略配置（限流/预算/审批规则） |
| P0-4.6 | 安全集成测试 | ⭐⭐ | 0.5d | JWT 验证 + 权限拦截测试 |

### 验收标准
- [ ] 登录返回有效 JWT
- [ ] JWT 过期自动拒绝
- [ ] 租户数据隔离（无法跨租户访问）
- [ ] 角色权限正确拦截

---

## P1-1: 成本追踪与预算 (`schemaplexai-ops`)

### 依赖
P0-3 Web Controllers（需要 CostWebController）

### 复杂度
⭐⭐⭐ 中等 | 预估 5-6 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P1-1.1 | CostService | ⭐⭐⭐ | 2d | 成本记录、聚合查询、按租户/Agent/时间维度 |
| P1-1.2 | BudgetService | ⭐⭐ | 1d | 预算设置、剩余查询、预警 |
| P1-1.3 | BudgetGuard | ⭐⭐⭐ | 1.5d | 执行前预算检查 + 超限拒绝 |
| P1-1.4 | CostWebController | ⭐⭐ | 1d | 成本查询 API + 预算管理 API |
| P1-1.5 | PG Schema 设计 | ⭐⭐ | 0.5d | sf_cost_record / sf_budget 表 |

### 验收标准
- [ ] 每次工具调用记录成本
- [ ] 预算超限自动拒绝新执行
- [ ] 可按租户/Agent/时间维度查询成本

---

## P1-2: 审计事件服务 (`schemaplexai-quality`)

### 依赖
P0-3 Web Controllers

### 复杂度
⭐⭐ 简单 | 预估 3-4 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P1-2.1 | AuditEventService | ⭐⭐ | 1d | 审计事件 CRUD + 查询 |
| P1-2.2 | ToolApprovalServiceImpl | ⭐⭐ | 1d | 工具注册审批流程 |
| P1-2.3 | 审计 API | ⭐⭐ | 1d | 查询/导出审计日志 |
| P1-2.4 | PG Schema 设计 | ⭐ | 0.5d | sf_audit_event 表 |

---

## P1-3: MQ 消费者 (`schemaplexai-task`)

### 依赖
P0-2 Lifecycle API（需要执行事件定义）

### 复杂度
⭐⭐ 简单 | 预估 3-4 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P1-3.1 | AgentExecuteDispatcher | ⭐⭐ | 1d | 消费执行请求 → 调度到 Engine |
| P1-3.2 | CostSyncConsumer | ⭐⭐ | 1d | 消费成本事件 → 同步到 PG |
| P1-3.3 | NotificationConsumer | ⭐⭐ | 1d | 消费通知事件 → 推送到 SSE/邮件 |
| P1-3.4 | MQ 配置 + 死信队列 | ⭐⭐ | 0.5d | RabbitMQ exchange/queue 配置 |

---

## P1-4: 工具集成（GitHub/GitLab）(`schemaplexai-integration`)

### 依赖
P0-2 Lifecycle API

### 复杂度
⭐⭐ 简单 | 预估 3-4 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P1-4.1 | GitHubToolExecutor | ⭐⭐ | 1.5d | GitHub API 封装（repo/issue/PR 操作） |
| P1-4.2 | GitLabToolExecutor | ⭐⭐ | 1.5d | GitLab API 封装 |
| P1-4.3 | ToolAdapter 接口统一 | ⭐ | 0.5d | 统一工具执行接口 + 错误处理 |

---

## P1-5: 知识上下文管理 (`schemaplexai-context`)

### 依赖
P0-3 Web Controllers

### 复杂度
⭐⭐⭐ 中等 | 预估 5-6 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P1-5.1 | 文档上传接口 | ⭐⭐ | 1d | Multipart + 格式校验 + 存储（先本地/mock） |
| P1-5.2 | 文档解析（Tika） | ⭐⭐⭐ | 1.5d | PDF/MD/代码解析 + 分块 |
| P1-5.3 | ContextInjector | ⭐⭐ | 1d | 将解析结果注入 Agent 上下文 |
| P1-5.4 | 知识库 CRUD | ⭐⭐ | 1d | 创建/查询/删除知识库 |
| P1-5.5 | 向量检索接口（mock） | ⭐⭐ | 1d | 接口定义 + 内存 mock，v1.1 接 Milvus |

---

## P2-1: 前端执行面板 (`schemaplexai-ui`)

### 依赖
P0-3 Web Controllers + P1-1 Cost API

### 复杂度
⭐⭐⭐⭐ 较高 | 预估 8-10 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P2-1.1 | 页面框架 + 路由 | ⭐⭐ | 1d | React Router 布局、导航栏 |
| P2-1.2 | 执行列表页 | ⭐⭐⭐ | 2d | 状态/时间/Agent 筛选 + 分页 |
| P2-1.3 | 执行详情页 | ⭐⭐⭐ | 2d | 状态时间线、工具调用记录、对话历史 |
| P2-1.4 | 实时事件流 | ⭐⭐⭐ | 2d | SSE 对接 + 消息渲染 |
| P2-1.5 | 审批操作面板 | ⭐⭐ | 1d | 审批列表 + approve/reject 操作 |
| P2-1.6 | 成本仪表盘 | ⭐⭐ | 1d | 图表组件 + 数据对接 |
| P2-1.7 | 前端 E2E 测试 | ⭐⭐ | 1d | Playwright 端到端测试 |

---

## P2-2: Workflow BPMN 集成 (`schemaplexai-workflow`)

### 依赖
P0-3 Web Controllers + P1-3 MQ Consumers

### 复杂度
⭐⭐⭐⭐ 较高 | 预估 7-8 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P2-2.1 | Flowable 引擎配置 | ⭐⭐ | 1d | Spring Boot 集成 + 数据源配置 |
| P2-2.2 | BPMN 流程定义 | ⭐⭐⭐ | 2d | agent-execution-approval.bpmn20.xml |
| P2-2.3 | BpmnApprovalDeployer | ⭐⭐ | 1d | 启动时自动部署 BPMN |
| P2-2.4 | HumanTaskAssignmentDelegate | ⭐⭐⭐ | 1.5d | 人任务分配逻辑 |
| P2-2.5 | Workflow ↔ Engine 集成 | ⭐⭐⭐ | 1.5d | 审批事件 ↔ BPMN 流程同步 |
| P2-2.6 | 集成测试 | ⭐⭐ | 1d | Testcontainers Flowable 测试 |

---

## P2-3: Agent 配置管理 (`schemaplexai-agent-config`)

### 依赖
P0-4 System Auth

### 复杂度
⭐⭐ 简单 | 预估 3-4 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P2-3.1 | Agent CRUD API | ⭐⭐ | 1d | 创建/查询/更新/删除 Agent |
| P2-3.2 | 模型配置 | ⭐⭐ | 1d | LLM provider 配置 + 模型选择 |
| P2-3.3 | 技能绑定 | ⭐ | 0.5d | Agent ↔ Skill 关联管理 |
| P2-3.4 | PG Schema | ⭐ | 0.5d | sf_agent / sf_agent_skill 表 |

---

## P2-4: Spec 规范中心 (`schemaplexai-spec`)

### 依赖
P0-3 Web Controllers

### 复杂度
⭐⭐ 简单 | 预估 3-4 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P2-4.1 | OpenAPI Spec CRUD | ⭐⭐ | 1d | 上传/查询/版本管理 |
| P2-4.2 | Spec 验证 | ⭐⭐ | 1d | OpenAPI 3 规范校验 |
| P2-4.3 | Spec 导出 | ⭐ | 0.5d | JSON/YAML 导出 |
| P2-4.4 | PG Schema | ⭐ | 0.5d | sf_openapi_spec 表 |

---

## P3-1: 集成测试套件

### 依赖
所有 P0 + P1 完成

### 复杂度
⭐⭐⭐ 中等 | 预估 5-6 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P3-1.1 | Testcontainers 基础设施 | ⭐⭐ | 1d | PG + Redis + RabbitMQ 容器配置 |
| P3-1.2 | Gateway 集成测试 | ⭐⭐ | 1d | 路由 + 鉴权 + 限流端到端 |
| P3-1.3 | Engine 集成测试 | ⭐⭐⭐ | 2d | 执行生命周期 + 状态机 + 快照 |
| P3-1.4 | Web API 集成测试 | ⭐⭐ | 1d | Controller → Service → DB 完整链路 |
| P3-1.5 | MQ 集成测试 | ⭐⭐ | 1d | 生产 → 消费 → 状态同步 |

---

## P3-2: Docker Compose 编排

### 依赖
所有服务可独立启动

### 复杂度
⭐⭐ 简单 | 预估 2-3 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P3-2.1 | docker-compose.yml | ⭐⭐ | 1d | PG + Redis + RabbitMQ + 4 个服务 |
| P3-2.2 | 服务 Dockerfile | ⭐ | 0.5d | 每个 deploy unit 的 Dockerfile |
| P3-2.3 | 环境变量配置 | ⭐ | 0.5d | .env 模板 + 启动脚本 |
| P3-2.4 | 健康检查 | ⭐ | 0.5d | /actuator/health 端点配置 |

---

## P3-3: Flyway 迁移完善

### 依赖
无

### 复杂度
⭐⭐ 简单 | 预估 2-3 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P3-3.1 | 审计所有 Flyway 迁移 | ⭐ | 0.5d | 检查命名冲突、缺失表、废弃列 |
| P3-3.2 | 补全缺失表 | ⭐⭐ | 1d | sf_approval_ticket 等缺失表 |
| P3-3.3 | 清理 v1.1 废弃列 | ⭐ | 0.5d | 标记 ClickHouse/ES/Milvus 相关列为 v1.1 |
| P3-3.4 | 迁移验证测试 | ⭐⭐ | 0.5d | Flyway validate + migrate 测试 |

---

## P3-4: API 文档（Knife4j）

### 依赖
P0-3 Web Controllers

### 复杂度
⭐ 简单 | 预估 1-2 天

### 子任务

| # | 子任务 | 复杂度 | 工时 | 说明 |
|---|--------|--------|------|------|
| P3-4.1 | OpenAPI 注解补充 | ⭐ | 1d | @Operation / @Schema / @Parameter |
| P3-4.2 | 接口分组配置 | ⭐ | 0.5d | 按模块分组（Execution / Approval / Cost） |

---

## 依赖关系总览

```
P0-1 Gateway ──────────────────────────┐
                                       ├──→ P1-3 MQ Consumers ──→ P2-2 Workflow
P0-2 Lifecycle API ────────────────────┤
                                       ├──→ P1-4 Integration
P0-3 Web Controllers ──────────────────┤
                                       ├──→ P1-1 Cost ──→ P2-1 Frontend
                                       ├──→ P1-2 Audit
                                       ├──→ P1-5 Context
                                       └──→ P2-4 Spec
P0-4 System Auth ──────────────────────┤
                                       ├──→ P2-3 Agent Config
                                       └──→ Gateway JWT (P0-1)

所有 P0+P1 ──→ P3-1 集成测试 ──→ P3-2 Docker ──→ P3-4 API 文档
```

---

## 建议执行顺序

### Week 1（并行 3 条线）
- **线 1**: P0-1 Gateway + P0-4 System Auth
- **线 2**: P0-2 Lifecycle API
- **线 3**: P0-3 Web Controllers + P3-3 Flyway

### Week 2（并行 3 条线）
- **线 1**: P1-1 Cost + P1-2 Audit + P1-3 MQ
- **线 2**: P1-4 Integration + P1-5 Context
- **线 3**: P2-3 Agent Config + P2-4 Spec

### Week 3（前端 + 工作流）
- **线 1**: P2-1 Frontend（最大工时，需持续 8-10 天）
- **线 2**: P2-2 Workflow BPMN

### Week 4（质量 + 交付）
- **线 1**: P3-1 Integration Tests
- **线 2**: P3-2 Docker Compose + P3-4 API 文档

---

## ⚠️ 关键风险

| 风险 | 影响 | 缓解 |
|------|------|------|
| Gateway 路由粒度不清 | P0-1 可能返工 | 先出路由表设计文档 |
| 快照一致性难保证 | P0-2 恢复流程验收风险 | 重点写并发测试 |
| SSE 连接管理 | P0-3/P2-1 实时性问题 | 先用轮询 fallback |
| ClickHouse 迁移延迟 | P1-1 成本追踪受限 | 先用 PG 短路径 |
| 前端工时超预期 | P2-1 延期 | 先做最小可用版本 |
