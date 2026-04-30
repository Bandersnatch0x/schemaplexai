---
topic: codebase-audit
stage: standard
version: v1.0
status: 已批准
supersedes: ""
---

# SchemaPlexAI 代码库功能打标审计报告

> **主题**: `codebase-audit`
> **阶段**: `standard`
> **版本**: v1.0
> **日期**: 2026-04-30
> **审计范围**: 全部 16 个后端模块 + 前端 UI

---

## 1. 审计方法

1. **静态代码扫描**: 遍历所有 `.java` / `.tsx` 源文件
2. **功能对标**: 对照 `docs/plans/unified-dev-plan.md` 的 47 个任务
3. **状态定义**:
   - **已实现**: 有完整业务逻辑，可直接运行
   - **部分实现**: 框架完成，但核心逻辑是 stub/placeholder
   - **未实现**: 仅有接口定义或完全缺失
   - **不适用**: 当前阶段不涉及

---

## 2. 后端模块审计

### 2.1 基础设施层（框架完成）

| 模块 | 文件数 | 代码行 | 状态 | 关键实现 | 缺失 |
|------|--------|--------|------|----------|------|
| **common** | 6 | ~219 | 已实现 | Result, ResultCode, BaseException, PageParam, TenantContextHolder | 无 |
| **model** | 2 | ~72 | 已实现 | BaseEntity, PageResult | 无 |
| **dao** | 2 | ~38 | 已实现 | BaseMapperX, TenantLineInterceptor | 无 |
| **gateway** | 6 | ~359 | 已实现 | JWT/限流/租户/日志/CORS | 无 |
| **web** | 11 | ~442 | 部分实现 | SSE/WebSocket 框架完整，但 Token 验证不完整 | SSE/WebSocket 的 JWT 签名验证 |

### 2.2 系统治理层

| 模块 | 文件数 | 代码行 | 状态 | 关键实现 | 缺失 |
|------|--------|--------|------|----------|------|
| **system** | 40 | ~1,584 | 部分实现 | 完整的 CRUD + Auth + JWT + RBAC 实体 | 模型路由逻辑、RBAC 分配逻辑 |
| **agent-config** | 12 | ~386 | 已实现 | Agent/Config/ShadowConfig/ToolBinding 完整 CRUD | 无 |

### 2.3 核心引擎层

| 模块 | 文件数 | 代码行 | 状态 | 关键实现 | 缺失 |
|------|--------|--------|------|----------|------|
| **agent-engine** | 47 | ~1,329 | 部分实现 | 状态机完整、TokenBudget CAS、准入控制、记忆管理、生命周期 | **LLM Provider 真实集成、工具真实执行、循环检测 sophistication** |

**详细打标**:

| 组件 | 状态 | 说明 |
|------|------|------|
| AgentStateMachine | 已实现 | 11 状态、终端保护、内存清理 |
| TokenBudget | 已实现 | CAS 循环、输入/输出预算 |
| ExecutionAdmissionService | 已实现 | 四维限流 |
| AiModelRouter | 已实现 | 供应商冷却 + 降级 |
| CompositeChatMemoryStore | 已实现 | L1 Redis + 7d TTL + 50 消息限制 |
| ThinkingStateHandler | 部分实现 | LLM 调用框架完整，但 Provider 返回空字符串 |
| ToolCallingStateHandler | 部分实现 | 工具解析返回 `List.of()`，执行是 stub |
| AgentRuntimeOrchestrator | 部分实现 | 循环框架完整，依赖 stubbed Provider |
| OpenAiProvider | 未实现 | `generate()` 返回空字符串 |
| AnthropicProvider | 未实现 | 同上 |

### 2.4 业务支撑层

| 模块 | 文件数 | 代码行 | 状态 | 关键实现 | 缺失 |
|------|--------|--------|------|----------|------|
| **context** | 31 | ~514 | 部分实现 | 实体/Mapper/Controller 完整，关键词检索可用 | **向量检索、Milvus 同步、Tika 文本提取** |
| **spec** | 32 | ~525 | 部分实现 | LCS diff、版本管理、评审工作流 | **发布工作流、审批链** |
| **workflow** | 20 | ~297 | 部分实现 | NodeEngine 框架、Flowable 桥接 | **真实 HTTP/Script 执行、Agent 节点** |
| **quality** | 37 | ~527 | 部分实现 | QualityOrchestrator 规则管道、SecurityScanRule | **真实 Spec 合规检查、LLM 安全扫描** |
| **integration** | 27 | ~429 | 部分实现 | ToolExecution、LocalToolExecutor、McpToolExecutor、Jenkins | **Git OAuth、Git Webhook、MCP 生命周期管理** |
| **ops** | 34 | ~562 | 部分实现 | CostService 框架、ClickHouse 同步游标 | **真实 ClickHouse JDBC 同步、成本分析查询** |
| **task** | 25 | ~800 | 部分实现 | MQ/DLX/幂等/ShedLock 基础设施完整 | **所有 MQ Consumer 业务逻辑、定时任务实现** |
| **admin** | 0 | 0 | 未实现 | — | **全部** |

### 2.5 P0/P1 问题修复状态

| 问题 | 状态 | 说明 |
|------|------|------|
| P0-001 DB driver | 待验证 | 未检查 application.yml |
| P0-003 StateMachine 构造器 | 已修复 | 手动构造器 |
| P0-004 JwtAuthFilter | 已修复 | 单次 builder |
| P0-005 重复实体 | 已修复 | 仅 Sf 前缀实体 |
| P0-006 重复主类 | 已修复 | 单一主类 |
| P1-002 TokenBudget 竞态 | 已修复 | CAS 循环 |
| P1-003 终端状态保护 | 已修复 | isTerminal() 检查 |
| P1-004 内存清理 | 已修复 | removeExecution() |
| P1-005 AiModelRouter 冷却 | 已修复 | activateCooldown() |
| P1-006 Redis TTL | 已修复 | 7d TTL + 50 消息 |
| P1-011 限流 fail-closed | 已修复 | onErrorResume 拒绝 |
| P1-013 Gateway JSON | 已修复 | ObjectMapper |
| P1-018 MQ 幂等竞态 | 已修复 | DB 唯一约束 |
| P1-019 分布式锁 | 已修复 | ShedLock |

---

## 3. 前端审计

| 页面/组件 | 状态 | 说明 |
|-----------|------|------|
| Dashboard | 已实现 | 统计卡片、图表、最近执行记录 |
| AgentManager | 已实现 | 完整 CRUD、分页、搜索、详情抽屉 |
| AgentExecutor | 已实现 | Agent 选择、SSE 流式执行、聊天记忆 |
| Login | 部分实现 | UI 完整，mock 认证待替换 |
| NotFound | 已实现 | 404 页面 |
| ContextCenter | 未实现 | 仅 mock 数据占位 |
| IntegrationCenter | 未实现 | 仅 mock 数据占位 |
| OpsCenter | 未实现 | 仅 mock 数据占位 |
| QualityCenter | 未实现 | 仅 mock 数据占位 |
| SpecCenter | 未实现 | 仅 mock 数据占位 |
| SystemSettings | 未实现 | 仅 mock 数据占位 |
| WorkflowCenter | 未实现 | 仅 mock 数据占位 |

**API 层**: 7/7 模块完整定义，但页面未连接
**Store 层**: 3/3 完整
**组件层**: 4/4 功能正常

---

## 4. 测试覆盖率

| 模块 | 单元测试 | 集成测试 | E2E 测试 |
|------|----------|----------|----------|
| 全部 | 0 | 0 | 0 |

**结论**: 零测试。必须立即建立测试基础设施。

---

## 5. 风险矩阵

| 风险 | 等级 | 影响 | 缓解 |
|------|------|------|------|
| LLM Provider 未集成 | **高** | Agent 引擎无法产生真实输出 | Phase 0 并行预研 |
| 零测试覆盖 | **高** | 无法验证重构安全性 | 优先建立测试框架 |
| 前端页面未连接 API | **中** | 用户无法使用大部分功能 | 按优先级逐个连接 |
| MQ Consumer 全部 TODO | **中** | 异步功能不可用 | 按业务优先级实现 |
| ClickHouse 未真实同步 | **中** | 成本分析不可用 | 配置 JDBC + 实现同步 |
| admin 模块为空 | **低** | 缺少聚合管理界面 | 后续迭代补充 |

---

## 6. 下一步建议

### 短期（1-2 周）
1. 建立测试框架（Testcontainers + JUnit + Vitest）
2. LangChain4j 技术预研（阻塞 Agent 引擎）
3. 修复 P0-001 DB driver 验证

### 中期（3-6 周）
1. LLM Provider 真实集成
2. 前端 6 个占位页面连接 API
3. MQ Consumer 业务逻辑实现

### 长期（7-12 周）
1. 完整 RAG Pipeline（Milvus + Tika + Embedding）
2. Workflow 真实节点执行器
3. ClickHouse 成本分析

---

## 7. 相关文档

- `docs/plans/unified-dev-plan.md` v1.0
- `docs/plans/project-plan.md` v1.1
- `docs/standards/feature-workflow.md`
- `docs/standards/fix-workflow.md`
