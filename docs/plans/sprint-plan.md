---
topic: sprint-plan
stage: plan
version: v1.0
status: 草稿
supersedes: ""
---

# Sprint 迭代计划

> **主题**: `sprint-plan`
> **阶段**: `plan`
> **版本**: v1.0
> **状态**: 草稿
> **日期**: 2026-04-30
> **范围**: SchemaPlexAI MVP 开发迭代规划

---

## 1. 迭代策略

- **迭代周期**: 2 周（10 个工作日）
- **团队规模**: 11 人（参照 project-plan v1.1）
- **每日站会**: 15min，同步阻塞与风险
- **迭代评审**: 每轮迭代最后 1 天 Demo
- **迭代回顾**: 每轮迭代最后半天

---

## 2. 迭代路线图

### Sprint 1-2: 基础设施强化（第 1-4 周）

**目标**: 修复 P0/P1 问题，建立测试框架，完成技术预研

| 任务 | 工时 | 负责人 | 优先级 |
|------|------|--------|--------|
| 验证并修复 P0-001 DB driver | 0.5d | 基础设施组 | P0 |
| 统一所有 application.yml 配置 | 1d | 基础设施组 | P0 |
| 建立测试框架（Testcontainers + JUnit） | 3d | 基础设施组 | P0 |
| LangChain4j 技术预研 | 5d | Agent 引擎组 | P0 |
| OpenSandbox SDK 集成预研 | 5d | Agent 引擎组 | P0 |
| Embedding 服务选型预研 | 3d | 数据/RAG 组 | P0 |
| 编写 common/model/dao 单元测试 | 2d | 基础设施组 | P1 |

**里程碑**: P0 问题清零 + 测试框架可用 + 技术预研报告产出

---

### Sprint 3-4: Agent 引擎核心（第 5-8 周）

**目标**: LLM Provider 真实集成，状态机闭环运行

| 任务 | 工时 | 负责人 | 优先级 |
|------|------|--------|--------|
| 集成 LangChain4j 到 agent-engine | 3d | Agent 引擎组 | P0 |
| 实现 OpenAiProvider / AnthropicProvider | 2d | Agent 引擎组 | P0 |
| 实现 ToolCallingStateHandler 真实逻辑 | 3d | Agent 引擎组 | P0 |
| 实现 AgentRuntimeOrchestrator 闭环 | 2d | Agent 引擎组 | P0 |
| 实现工具注册框架 + 本地工具 | 2d | Agent 引擎组 | P1 |
| 补充 Agent 引擎单元测试 | 3d | Agent 引擎组 | P1 |
| OpenSandbox 集成（如预研通过） | 5d | Agent 引擎组 | P1 |

**里程碑**: API 创建 Agent → 执行 → SSE 流 → 完成（端到端通）

---

### Sprint 5-6: RAG 与上下文（第 9-12 周）

**目标**: 知识文档上传 → 向量化 → 检索 → 一致

| 任务 | 工时 | 负责人 | 优先级 |
|------|------|--------|--------|
| 集成 Apache Tika 文本提取 | 1d | 数据/RAG 组 | P0 |
| 实现文本分块 + Embedding 服务 | 3d | 数据/RAG 组 | P0 |
| 实现 Milvus 向量写入/检索 | 3d | 数据/RAG 组 | P0 |
| 实现 PG→Milvus 同步消费者 | 2d | 数据/RAG 组 | P0 |
| 实现 RAG 检索 API（语义搜索） | 2d | 数据/RAG 组 | P0 |
| 实现 Milvus-PG 对账任务 | 1.5d | 数据/RAG 组 | P1 |
| 前端 ContextCenter 连接 API | 2d | 前端组 | P1 |

**里程碑**: 上传文档 → 自动向量化 → Agent 可检索

---

### Sprint 7-8: Spec 与工作流（第 13-16 周）

**目标**: Spec 版本管理 + 工作流编排

| 任务 | 工时 | 负责人 | 优先级 |
|------|------|--------|--------|
| 实现 Spec 发布工作流 | 2d | 数据/RAG 组 | P0 |
| 实现审批链（多级评审） | 3d | 数据/RAG 组 | P0 |
| 实现 Steering 文档绑定 Agent | 1d | 数据/RAG 组 | P1 |
| 实现 AgentNodeExecutor | 2d | Agent 引擎组 | P0 |
| 实现 HTTP/Script 节点真实执行 | 3d | 基础设施组 | P0 |
| 实现 Flowable 流程定义管理 | 2d | 基础设施组 | P1 |
| 前端 SpecCenter / WorkflowCenter 连接 API | 3d | 前端组 | P1 |

**里程碑**: 定义 Spec → 绑定 Agent → 工作流编排执行

---

### Sprint 9-10: 质量与集成（第 17-20 周）

**目标**: 质量门禁生效 + 外部集成可用

| 任务 | 工时 | 负责人 | 优先级 |
|------|------|--------|--------|
| 实现 SpecComplianceRule L2/L3 | 3d | Agent 引擎组 | P0 |
| 实现 SecurityScanRule 完整检测 | 2d | Agent 引擎组 | P0 |
| 实现 Git OAuth + 仓库操作 | 3d | 集成/运营组 | P0 |
| 实现 Git Webhook 处理 | 2d | 集成/运营组 | P1 |
| 实现 MCP Server 生命周期管理 | 3d | 集成/运营组 | P1 |
| 前端 QualityCenter / IntegrationCenter 连接 API | 3d | 前端组 | P1 |

**里程碑**: 执行自动过门禁 + Agent 可调用 Git/MCP

---

### Sprint 11-12: 运营与前端完善（第 21-24 周）

**目标**: 成本分析 + 前端页面完善

| 任务 | 工时 | 负责人 | 优先级 |
|------|------|--------|--------|
| ClickHouse JDBC 真实同步 | 3d | 集成/运营组 | P0 |
| 成本报表 API + 日聚合 | 3d | 集成/运营组 | P0 |
| 预算告警 + 通知 | 2d | 集成/运营组 | P1 |
| MQ Consumer 业务逻辑实现 | 5d | 基础设施组 | P1 |
| 定时任务实现（6 个 Job） | 4d | 基础设施组 | P1 |
| 前端 OpsCenter / SystemSettings 连接 API | 3d | 前端组 | P1 |
| Login 替换真实认证 | 1d | 前端组 | P0 |

**里程碑**: 成本报表 → 通知 → 前端全页面可用

---

### Sprint 13-14: 高级特性（第 25-28 周）

**目标**: Team Agent + 性能优化

| 任务 | 工时 | 负责人 | 优先级 |
|------|------|--------|--------|
| LangGraph4j 评估（如通过） | 3d | Agent 引擎组 | P1 |
| Team Agent 模型设计 | 2d | Agent 引擎组 | P1 |
| 循环检测 sophistication | 2d | Agent 引擎组 | P1 |
| 记忆压缩策略优化 | 2d | Agent 引擎组 | P1 |
| ChatMessage 归档任务 | 1d | 基础设施组 | P1 |
| OpenTelemetry 追踪集成 | 2d | 基础设施组 | P1 |

**里程碑**: Team Agent 协作 + 循环检测 + 全链路追踪

---

### Sprint 15: 系统测试（第 29-30 周）

**目标**: 生产就绪

| 任务 | 工时 | 负责人 | 优先级 |
|------|------|--------|--------|
| 核心链路集成测试 | 3d | 测试组 | P0 |
| Agent 引擎压力测试 | 3d | 测试组 | P0 |
| 多租户隔离安全审计 | 2d | 测试组 | P0 |
| 部署文档 + 运维手册 | 2d | DevOps 组 | P0 |
| Bug 修复 | 持续 | 全员 | P0 |

**里程碑**: 系统具备生产上线条件

---

## 3. 迭代看板（Sprint Board）

```
┌──────────┬──────────┬──────────┬──────────┬──────────┐
│  Backlog │  To Do   │ In Prog  │  Review  │  Done    │
├──────────┼──────────┼──────────┼──────────┼──────────┤
│          │          │          │          │          │
│ 待排期   │ 本轮迭代  │ 开发中   │ CR中    │ 已完成   │
│ 任务     │ 任务     │          │          │          │
└──────────┴──────────┴──────────┴──────────┴──────────┘
```

**看板规则**:
- 每列 WIP 限制: In Progress ≤ 3 / Review ≤ 5
- 任务卡片必须关联 Spec 和 Plan
- 每日站会更新看板状态

---

## 4. 风险与缓冲

| 风险 | 影响迭代 | 缓冲策略 |
|------|----------|----------|
| LangChain4j 预研不通过 | Sprint 3-4 | +1 周 fallback 到直接 SDK |
| OpenSandbox 不成熟 | Sprint 3-4 | +2 周降级为本地 JVM 执行 |
| 前端人力不足 | Sprint 5-12 | 优先 Dashboard/Agent 页面，管理页面延后 |
| ClickHouse 同步性能差 | Sprint 11-12 | 增加批次大小，异步化 |

---

## 5. 相关文档

- `docs/plans/project-plan.md` v1.1（30 周路线图）
- `docs/plans/unified-dev-plan.md` v1.0（47 任务）
- `docs/plans/tech-research-plan.md`（技术预研）
- `docs/standards/feature-workflow.md`
