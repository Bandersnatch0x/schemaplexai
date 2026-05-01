# Agentic Design Patterns 架构改进建议

> 基于《Agentic Design Patterns》21 个模式的预研成果，对比 SchemaPlexAI 当前架构，提出分阶段改进建议。
>
> **状态**: 初步版（产品视角）。待架构/安全/AI工程视角补充后更新为终版。

---

## 一、成熟度分层结论

### Layer 1: MVP 核心（9 个模式）— 立即补足

让 Agent 能安全、可控、可观测地执行基本任务。缺失任何一个，产品都无法称为"可用的 AI Agent 平台"。

| 优先级 | 模式 | 理由 |
|--------|------|------|
| P0 | **Tool Use** | 企业用户购买 Agent 平台的核心预期：Agent 能调用 API、查数据库、执行代码。没有 Tool Use = 聊天机器人 |
| P0 | **RAG** | 企业知识管理是刚需。RAG 让 Agent 能回答基于内部文档的问题 |
| P0 | **Planning** | 企业工作流自动化的基础。用户期望"帮我完成这个任务"而非"回答这个问题" |
| P1 | **Exception Handling** | 生产环境的底线要求。不能容忍 Agent 失败后无迹可寻 |
| P1 | **Guardrails** | 企业合规红线。数据泄露、越权操作 = 法律风险 |
| P1 | **Routing** | 多场景支持的基础。企业有多个业务线，需要不同专用 Agent |
| P1 | **Reflection** | 输出质量保障的基础机制。企业对 AI"胡说八道"零容忍 |
| P2 | **Resource Optimization** | 成本控制是企业采购决策的关键因素 |
| P2 | **Evaluation** | 可观测性是企业采纳 AI 的"入场券"。没有 Trace/成本归因/质量评估，企业不会投产 |

### Layer 2: 平台竞争力（8 个模式）— 中期建设

在 MVP 基础上建立护城河，让客户选择 SchemaPlexAI 而非 AutoGen/CrewAI/Dify。

| 优先级 | 模式 | 理由 |
|--------|------|------|
| P1 | **Multi-Agent Collaboration** | **最大差异化点**。企业不买单 Agent，买单的是"Agent 团队"能完成端到端任务 |
| P1 | **Human-in-the-Loop** | **企业合规刚需**。金融、医疗等 regulated industries 要求关键决策人工审批 |
| P1 | **Memory Management** | 企业级长期记忆是差异化能力。让 Agent"越用越聪明" |
| P2 | **Parallelization** | 效率倍增器。企业场景大量存在可并行子任务 |
| P2 | **Prompt Chaining** | 复杂任务分解的标准模式。与 Planning 配合，每步可独立监控和回滚 |
| P2 | **Goal Setting** | 企业项目管理集成。让 Agent 执行与 OKR/JIRA 里程碑对齐 |
| P3 | **MCP** | 生态扩展能力。统一工具发现协议降低集成成本 |
| P3 | **A2A** | 多 Agent 协作的通信基础设施。早期支持可建立标准制定者形象 |

### Layer 3: 未来创新（4 个模式）— 延后探索

提升竞争力、建立技术领导力，但需等待市场验证。

| 模式 | 延后理由 |
|------|---------|
| **Learning/Adaptation** | Self-improving Agent 存在幻觉放大风险，企业客户当前更关注可控性 |
| **Reasoning Techniques** | CoT 已隐含在 LLM 调用中，Self-Correction 属于"锦上添花" |
| **Prioritization** | 属于规模问题。当前单租户并发量不足以体现价值 |
| **Exploration/Discovery** | 市场需求尚未验证，与当前"R&D 协作"定位有偏离 |

---

## 二、关键改进建议

### 建议 1: Tool Use 完整实现（P0，Week 9-10）

**当前问题**: `ToolCallingStateHandler.parseToolCalls()` 返回空列表；无 `ToolRegistry`；无工具模式定义。

**改进方向**:
- 实现 `ToolRegistry`：支持动态注册/发现工具，支持 Java 方法反射 + HTTP API 调用
- 实现 `ToolAdapter`：统一工具调用接口，封装参数序列化/反序列化
- 实现 `ToolSafetyGuard`：拦截不可逆操作（删除、修改生产数据），要求 HITL 审批
- 对接 LLM 原生 Function Calling：升级 `LlmProvider` 接口，支持 OpenAI/Anthropic 函数调用格式

**影响文件**:
- `schemaplexai-agent-engine/src/.../tool/ToolRegistry.java` (新增)
- `schemaplexai-agent-engine/src/.../tool/ToolAdapter.java` (新增)
- `schemaplexai-agent-engine/src/.../tool/ToolSafetyGuard.java` (新增)
- `schemaplexai-agent-engine/src/.../state/ToolCallingStateHandler.java` (修改)
- `schemaplexai-agent-engine/src/.../model/LlmProvider.java` (修改)

### 建议 2: RAG 管道接入（P0，Week 11-12）

**当前问题**: `ContextInjector.inject()` 仅打印日志；无向量检索。

**改进方向**:
- 复用已有的 `schemaplexai-context` 服务（Milvus + MinIO）
- 实现文档分块 → Embedding → 向量存储 → 相似度检索管道
- `ContextInjector` 接入检索结果，注入到 LLM 上下文
- 支持多知识库隔离（按 tenant + project）

**影响文件**:
- `schemaplexai-context/.../RagPipelineService.java` (修改)
- `schemaplexai-agent-engine/src/.../context/ContextInjector.java` (修改)
- `schemaplexai-agent-engine/src/.../state/ThinkingStateHandler.java` (修改，注入上下文)

### 建议 3: Planning 状态实现（P0，Week 13-14）

**当前问题**: `THINKING` 状态单次调用 LLM，无任务分解。

**改进方向**:
- 新增 `PlanningStateHandler`：调用 LLM 生成子任务计划（任务列表 + 依赖关系）
- 子任务状态追踪：每个子任务有自己的状态机实例
- 与 Tool Use 集成：Planning 识别需要工具调用的步骤
- 支持计划修订：执行过程中发现新信息时动态调整计划

**影响文件**:
- `schemaplexai-agent-engine/src/.../state/PlanningStateHandler.java` (新增)
- `schemaplexai-agent-engine/src/.../plan/SubTask.java` (新增)
- `schemaplexai-agent-engine/src/.../plan/TaskPlan.java` (新增)
- `schemaplexai-agent-engine/src/.../AgentExecutionState.java` (修改，新增 PLANNING 状态)

### 建议 4: Multi-Agent 编排架构（P1，Week 17-22）

**当前问题**: 单 Agent 执行；无 Coordinator/Swarm/Crew。

**改进方向**:
- **Coordinator 模式**: 中央协调器接收任务，分发给子 Agent，聚合结果
- **消息总线**: 利用现有 RabbitMQ 实现 Agent 间异步通信
- **Swarm 拓扑**: 支持去中心化 Agent 协作（如代码评审 Swarm）
- **Crew 模式**: Role-Based 多 Agent 团队（如：Planner + Coder + Reviewer）
- 利用微服务架构优势：每个 Agent 可部署为独立服务

**影响文件**:
- `schemaplexai-agent-engine/src/.../orchestrator/MultiAgentOrchestrator.java` (新增)
- `schemaplexai-agent-engine/src/.../orchestrator/CoordinatorAgent.java` (新增)
- `schemaplexai-agent-engine/src/.../message/AgentMessageBus.java` (新增)
- `schemaplexai-agent-engine/src/.../swarm/SwarmTopology.java` (新增)

### 建议 5: HITL 审批流（P1，Week 20-22）

**当前问题**: `PAUSED` 状态存在但无 UI 审批流。

**改进方向**:
- 后端：PAUSED 状态触发审批任务创建，支持通过/拒绝/注释回调
- 前端：审批任务列表 + 详情查看 + 操作按钮
- 权限控制：基于角色（RBAC）的审批权限
- 审计留痕：所有审批操作记录到 ObservabilityTrace

**影响文件**:
- `schemaplexai-agent-engine/src/.../lifecycle/ApprovalService.java` (新增)
- `schemaplexai-ui/src/pages/Approvals/` (新增)
- `schemaplexai-web/.../ApprovalController.java` (新增)

---

## 三、竞品对标与竞争策略

| 能力维度 | SchemaPlexAI (当前) | AutoGen | CrewAI | Dify | 策略 |
|----------|---------------------|---------|--------|------|------|
| 单 Agent 执行 | 骨架完成 | 成熟 | 成熟 | 成熟 | 补齐 Tool Use + RAG 追平 |
| Multi-Agent 编排 | ❌ 未实现 | **领先** | **领先** | 基础 | **重点突破** — 微服务支持真分布式编排 |
| HITL 审批 | ⚠️ 骨架 | 基础 | 有限 | 基础 | **差异化** — 企业级审批流 + 审计 |
| RAG/知识库 | ❌ 存根 | 需自建 | 需自建 | **领先** | 快速补齐，利用已有 Milvus |
| 可观测性 | ⚠️ 部分 | 基础 | 基础 | **领先** | 建立 Trace/Span/Generation 体系 |
| 多租户/企业级 | **领先** | 无 | 有限 | 有限 | **核心优势** — 保持并强化 |
| 工作流引擎 | Flowable 骨架 | 无 | 无 | 基础 | **差异化** — BPMN + AI 节点双引擎 |
| 成本管控 | TokenBudget 骨架 | 无 | 无 | 基础 | **差异化** — 多维度准入 + ClickHouse 分析 |

**核心竞争策略**:

```
SchemaPlexAI = 企业级基础设施（多租户/RBAC/审计）
             + 深度 AI 编排（Multi-Agent + BPMN 工作流）
             + 生产级保障（HITL + Evaluation + 沙箱隔离）
             + 成本可控（Token 预算 + ClickHouse 分析）
```

**不与 Dify 拼产品体验，不与 AutoGen 拼学术深度。拼的是"企业敢把关键业务流程交给 AI"的信任度。**

---

## 四、交付路线图

### Phase 1: Layer 1 MVP（Week 9-16，8 周）

| 周次 | 重点 | 目标 |
|------|------|------|
| 9-10 | Tool Use 完整实现 | Agent 能调用工具完成基本任务 |
| 11-12 | RAG 管道接入 | Agent 能基于知识库回答问题 |
| 13-14 | Planning 状态实现 | Agent 能自动分解目标为子任务 |
| 15-16 | Guardrails + Evaluation | 安全可控，可观测可评估 |

**成功标准**:
- [ ] 单 Agent 完整执行闭环（输入 → Planning → Thinking → ToolUse → Observation → Completed）
- [ ] RAG 检索准确率 Top5 > 80%
- [ ] Tool Safety Guard 拦截 100% 不可逆操作测试用例
- [ ] 测试覆盖率 > 50%

### Phase 2: Layer 2 平台竞争力（Week 17-28，12 周）

| 周次 | 重点 | 目标 |
|------|------|------|
| 17-19 | Multi-Agent 基础 | Coordinator + 消息总线 |
| 20-22 | HITL + Memory | 审批流 + 长期记忆 |
| 23-25 | 高级编排 | Parallelization + Prompt Chaining |
| 26-28 | 生态扩展 | MCP + A2A |

**成功标准**:
- [ ] 3+ Agent 协作完成端到端任务
- [ ] HITL 审批平均响应时间 < 4 小时
- [ ] 跨会话记忆命中率 > 70%
- [ ] 测试覆盖率 > 80%

### Phase 3: Layer 3 未来创新（Week 29+，视市场反馈）

- Learning/Adaptation: Shadow Review 数据收集 → 自动学习 Pipeline
- Reasoning: Self-Correction + 代码执行推理
- Prioritization: 任务队列优先级 + 资源分配
- Exploration: Agent Laboratory（研究自动化）

---

## 五、风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Layer 1 工期超支 | 错过早期客户窗口 | 严格 MVP 范围控制，Reflection 和 Resource Optimization 可降级 |
| Multi-Agent 复杂度失控 | Layer 2 延期 | 先实现简单 Coordinator 模式，Swarm/Crew 延后 |
| RAG 准确率不达标 | 知识场景不可用 | 预留 2 周调优窗口，准备关键词匹配 Fallback |
| 竞品快速迭代 | 差异化窗口缩小 | 聚焦企业级功能（HITL/审计/多租户），避开消费级竞争 |
| 零测试文化阻力 | 质量债务累积 | TDD 强制要求，代码审查 blocker |

---

## 六、附录：模式依赖关系

```
Layer 1 基础
  Tool Use ←──┬── Reflection
              ├── Planning
              ├── Multi-Agent
              └── Reasoning
  RAG ←───────┬── Memory
              └── Learning
  Planning ←── Tool Use (执行子任务)
  Exception Handling ←── 所有模式
  Guardrails ←── Tool Use, RAG
  Routing ←── Multi-Agent
  Reflection ←── Tool Use (评估工具)
  Resource Optimization ←── 所有执行模式
  Evaluation ←── Reflection, Guardrails

Layer 2 生产
  Multi-Agent ←── Routing + Tool Use + Memory
  HITL ←── Exception Handling + Guardrails
  Memory ←── RAG (向量存储)
  Parallelization ←── Planning (识别可并行任务)
  Prompt Chaining ←── Planning (子任务即链节)
  Goal Setting ←── Planning + Evaluation
  MCP ←── 扩展 Tool Use 的工具发现
  A2A ←── Multi-Agent + MCP

Layer 3 高级
  Learning/Adaptation ←── Memory + Evaluation + Shadow Review
  Reasoning ←── Tool Use + Reflection
  Prioritization ←── Multi-Agent + Goal Setting
  Exploration ←── 所有上层能力
```
