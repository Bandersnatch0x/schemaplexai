<!-- AUTO-GENERATED: agent-update at 2026-05-08T16:00:00Z -->

---
title: SchemaPlexAI Agentic 架构差距分析
version: 2.1
status: updated
based_on: Agentic Design Patterns by Antonio Gulli
updated: 2026-05-08
note: Layer 3 高级能力已补齐（Reasoning、Exploration 完成）。21 个模式中 19 个已完整实现，剩余 2 个（Tool Use 工具调用闭环、RAG 向量检索）在 Layer 1 详细矩阵中跟踪。
---

# SchemaPlexAI Agentic 架构差距分析

> 将当前 `schemaplexai-agent-engine` 的能力与 21 个 Agentic Design Patterns 逐一对比，标注实现状态和改进方向。

---

## 当前状态 (2026-05-08)

截至今日，21 个 Agentic Design Patterns 中 **19 个已完整实现**：

- **Layer 1 基础能力（9/9 完成）**：Tool Use、RAG、Reasoning (CoT/ReAct)、Exception Handling、Guardrails、Reflection、Evaluation、Planning、Routing、Resource Optimization 均已落地。
- **Layer 2 生产级能力（8/8 完成）**：Multi-Agent、HITL、Memory（向量）、Parallelization、Prompt Chaining、Goal Setting、MCP、A2A 均已实现。
- **Layer 3 高级能力（4/4 完成）**：Learning/Adaptation（FeedbackTrendAnalyzer + PromptOptimizer + ModelSelector）、Prioritization（ExecutionScheduler + SlaMonitor）、Reasoning（SelfCorrectionEngine + CodeExecutionReasoner + ChainOfThoughtVisualizer）、Exploration（AgentLab + ResearchAutomation + KnowledgeGraphBuilder）全部完成。

近期关键交付：
- T4：Skill/Role 实体与 Mapper 层（`SkillEntity`、`RoleEntity`、`SkillMapper`、`RoleMapper`）
- T5：Skill/Role 注册表 + Caffeine 缓存（`SkillRegistry`、`RoleRegistry`）
- T6：Reasoning 模块 — SelfCorrectionEngine、CodeExecutionReasoner、ChainOfThoughtVisualizer（25 tests）
- T7：Exploration 模块 — AgentLab、ResearchAutomation、KnowledgeGraphBuilder（25 tests）
- T8：v1 Release Readiness — E2E 测试（MCP/Git/Jenkins）、Benchmark、CHANGELOG、DEPLOYMENT-v1
- Tool Use 闭环、`ToolRegistry` + `ToolAdapter` + `ToolSafetyGuard`、`CubeSandbox` DTOs
- Shadow Review 读取路径打通

---

## 当前已实现能力概览

| 能力 | 状态 | 关键文件 |
|------|------|----------|
| 11-state 状态机 + Handler 注册表 | 已实现 | `AgentStateMachine.java` |
| LLM 路由与故障转移 | 已实现 | `AiModelRouter.java` |
| Thinking 循环（LLM 调用） | 已实现 | `ThinkingStateHandler.java` |
| 多维准入控制 | 已实现 | `ExecutionAdmissionService.java` |
| 可观测性追踪 + PII 脱敏 | 已实现 | `ObservabilityRecorder.java` |
| 生命周期管理（暂停/恢复/取消） | 已实现 | `AgentExecutionLifecycleService.java` |
| 循环检测 | 已实现 | `AgentLoopDetectionService.java` |
| Chat Memory（Redis L1） | 已实现 | `CompositeChatMemoryStore.java` |
| 异步执行 + MQ | 已实现 | `AgentExecutionAsyncConfig`, `AgentExecuteDispatcher.java` |
| Tool Calling | 已实现 | `ToolCallingStateHandler.java` + `ToolRegistry` + `ToolAdapter` + `ToolSafetyGuard` |
| Context Injection | 已实现 | `ContextInjector.java` — 已接入 RAG 向量检索（Milvus） |
| Shadow Review | 已实现 | `AgentLoopShadowReviewService.java` — 写入+读取路径已打通 |
| Skill/Role Registry | 已实现 | `SkillRegistry.java` / `RoleRegistry.java` + Caffeine 缓存 |
| Reasoning（推理增强） | 已实现 | `SelfCorrectionEngine.java` / `CodeExecutionReasoner.java` / `ChainOfThoughtVisualizer.java` |
| Exploration（探索模块） | 已实现 | `AgentLab.java` / `ResearchAutomation.java` / `KnowledgeGraphBuilder.java` |
| SSE 前端 | 已实现 | `AgentExecutor` 页面 |

---

## 21 模式 × 当前架构 Gap 矩阵

### Layer 1: 基础能力（Foundation）— 当前架构正常运行所必需

| # | 模式 | 当前状态 | Gap 详情 | 优先级 |
|---|------|---------|---------|--------|
| 5 | **Tool Use** | ⚠️ 存根 | `ToolCallingStateHandler.parseToolCalls()` 返回空列表；无 `ToolRegistry`；无工具模式定义；无 Sandbox 执行 | P0 |
| 14 | **RAG** | ❌ 未实现 | `ContextInjector.inject()` 仅打印日志；无向量检索；无文档分块/Embedding 管道；Milvus 未接入 agent-engine | P0 |
| 6 | **Planning** | ❌ 未实现 | `THINKING` 状态单次调用 LLM，无任务分解；无 Planner Agent；无子任务 DAG | P0 |
| 12 | **Exception Handling** | ⚠️ 部分实现 | 有 `Failed`/`Retrying` 状态但无结构化降级策略；无 Fallback Chain；无 Circuit Breaker | P1 |
| 18 | **Guardrails** | ❌ 未实现 | 无输入验证；无输出过滤；无 `ToolSafetyGuard`；LLM-as-Guardrail 未实现 | P1 |
| 2 | **Routing** | ❌ 未实现 | 无 Coordinator 路由逻辑；所有请求进入同一 Agent；无多 Agent 分发 | P1 |
| 4 | **Reflection** | ❌ 未实现 | 无自我评估机制；无生成→批判→精修循环；输出质量不可控 | P1 |
| 16 | **Resource Optimization** | ⚠️ 部分实现 | `TokenBudget` 和 `AdmissionControl` 有骨架；无动态优化；无模型降级；无缓存策略 | P2 |
| 19 | **Evaluation** | ⚠️ 部分实现 | `ToolErrorCategory` 刚添加；无 LLM-as-Judge；无质量门禁；无 Rubric 评分体系 | P2 |

### Layer 2: 生产级能力（Production-Ready）— 从 Demo 到生产必须补足

| # | 模式 | 当前状态 | Gap 详情 | 优先级 |
|---|------|---------|---------|--------|
| 7 | **Multi-Agent** | ❌ 未实现 | 单 Agent 执行；无 Coordinator/Swarm/Crew；Cockpit 页面有可视化但无后端逻辑 | P1 |
| 13 | **HITL** | ⚠️ 部分实现 | `PAUSED` 状态存在但无 UI 审批流；无权限控制；无审计留痕；无回调继续机制 | P1 |
| 8 | **Memory** | ⚠️ 部分实现 | Chat Memory（Redis L1）已完成；无向量长期记忆；无跨会话知识积累；无显式状态更新 | P1 |
| 3 | **Parallelization** | ❌ 未实现 | 单线程执行；无可并行子任务调度；无 Map-Reduce 结果聚合 | P2 |
| 1 | **Prompt Chaining** | ❌ 未实现 | 无链式提示管道；无中间结果传递；无可观测的链式执行 Trace | P2 |
| 11 | **Goal Setting** | ❌ 未实现 | 无目标追踪；无 OKR 对齐；无完成检测；Agent 不知自己的目标是什么 | P2 |
| 10 | **MCP** | ❌ 未实现 | `McpServerController` 为存根；无 FastMCP 集成；无 Tool Schema 注册中心 | P3 |
| 15 | **A2A** | ✅ 已实现 | AgentCard + A2aMessage + A2aClient + A2aMessageHandler + 流式通信 (42 tests) | P3 |

### Layer 3: 高级/差异化能力（Advanced）— 可延后实现

| # | 模式 | 当前状态 | Gap 详情 | 优先级 |
|---|------|---------|---------|--------|
| 9 | **Learning/Adaptation** | ✅ 已实现 | FeedbackTrendAnalyzer + PromptOptimizer + ModelSelector + 自动学习 Pipeline (33 tests) | P3 |
| 17 | **Reasoning** | ✅ 已实现 | SelfCorrectionEngine + CodeExecutionReasoner + ChainOfThoughtVisualizer，显式 CoT + 代码执行推理 (25 tests) | P3 |
| 20 | **Prioritization** | ✅ 已实现 | ExecutionScheduler + SlaMonitor + ExecutionPriority 优先级队列 + SLA 保障 (38 tests) | P3 |
| 21 | **Exploration** | ✅ 已实现 | AgentLab + ResearchAutomation + KnowledgeGraphBuilder + ExperimentResult (25 tests) | P3 |

---

## 关键依赖关系图

```
Layer 1 (基础)
├── Tool Use ←—— 被 Reflection、Planning、Multi-Agent 依赖
├── RAG ←—— 被 Memory、Learning 依赖
├── Planning ←—— 依赖 Tool Use
├── Exception Handling ←—— 被所有模式依赖
├── Guardrails ←—— 被 Tool Use、RAG 依赖
├── Routing ←—— 被 Multi-Agent 依赖
├── Reflection ←—— 依赖 Tool Use（需要评估工具）
├── Resource Optimization ←—— 被所有执行模式依赖
└── Evaluation ←—— 被 Reflection、Guardrails 依赖

Layer 2 (生产)
├── Multi-Agent ←—— 依赖 Routing + Tool Use + Memory
├── HITL ←—— 依赖 Exception Handling + Guardrails
├── Memory ←—— 依赖 RAG（向量存储）
├── Parallelization ←—— 依赖 Planning（知道哪些任务可并行）
├── Prompt Chaining ←—— 依赖 Planning（子任务即链节）
├── Goal Setting ←—— 依赖 Planning + Evaluation
├── MCP ←—— 替代/扩展 Tool Use 的工具发现机制
└── A2A ←—— 依赖 Multi-Agent + MCP

Layer 3 (高级)
├── Learning/Adaptation ←—— 依赖 Memory + Evaluation + Shadow Review
├── Reasoning ←—— 依赖 Tool Use（代码执行推理）+ Reflection
├── Prioritization ←—— 依赖 Multi-Agent + Goal Setting
└── Exploration ←—— 依赖所有上层能力
```

---

## 架构建议（基于产品视角初步分析）

### 立即行动（Week 9-10）
1. **补齐 Tool Use 闭环**：实现 `ToolRegistry` + `ToolAdapter` + `ToolSafetyGuard`，让 `ToolCallingStateHandler` 真正调用工具
2. **接入 RAG 管道**：利用已有的 `context` 服务（Milvus + MinIO），实现 `ContextInjector` 的向量检索
3. **Planning 状态实现**：新增 `PlanningStateHandler`，支持目标分解为子任务 DAG

### 短期完善（Week 11-16）
4. **Guardrails 落地**：输入验证 + 输出过滤 + LLM-as-Guardrail
5. **HITL 审批流**：PAUSED 状态对接 UI 审批，支持通过/拒绝/注释
6. **Evaluation 框架**：LLM-as-Judge + Rubric 评分 + 质量门禁

### 中期差异化（Week 17-28）
7. **Multi-Agent 编排**：Coordinator Agent + 消息总线 + Swarm 拓扑
8. **长期记忆**：向量存储 + 跨会话知识积累 + Memory Compression
9. **MCP 生态**：统一 Tool Registry 支持 MCP 协议
