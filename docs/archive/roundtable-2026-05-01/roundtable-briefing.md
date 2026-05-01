# 圆桌辩论议题简报：Agentic Design Patterns 成熟度分层

## 日期
2026-05-01

## 背景
SchemaPlexAI 是一个企业级 AI R&D 协作平台。agent-engine 模块已构建了一个生产级的单 Agent ReAct 状态机骨架，但 14+ 个设计模式仍处于缺失或存根状态。

## 当前已实现能力（agent-engine）

| 能力 | 状态 | 关键文件 |
|------|------|----------|
| 11-state 状态机 + Handler 注册表 | 已实现 | AgentStateMachine.java |
| LLM 路由与故障转移 | 已实现 | AiModelRouter.java |
| Thinking 循环（LLM 调用） | 已实现 | ThinkingStateHandler.java |
| 多维准入控制 | 已实现 | ExecutionAdmissionService.java |
| 可观测性追踪 + PII 脱敏 | 已实现 | ObservabilityRecorder.java |
| 生命周期管理（暂停/恢复/取消） | 已实现 | AgentExecutionLifecycleService.java |
| 循环检测 | 已实现 | AgentLoopDetectionService.java |
| Chat Memory（Redis L1） | 已实现 | CompositeChatMemoryStore.java |
| 异步执行 + MQ | 已实现 | AgentExecutionAsyncConfig, AgentExecuteDispatcher.java |
| Tool Calling | 存根 | ToolCallingStateHandler.java — parseToolCalls() 返回空列表 |
| Context Injection | 存根 | ContextInjector.java — 仅日志，无实际 RAG |
| Shadow Review | 存根 | AgentLoopShadowReviewService.java — 写入但无读取路径 |
| SSE 前端 | 已实现 | AgentExecutor 页面 |

## 21 个设计模式（Antonio Gulli 书籍）

1. **Prompt Chaining** — 将复杂任务分解为串联的提示链
2. **Routing** — 根据输入路由到不同的专用 Agent/Handler
3. **Parallelization** — 并行执行多个子任务并聚合结果
4. **Reflection** — Agent 自我评估并迭代改进输出
5. **Tool Use** — Agent 调用外部工具（搜索、代码执行、API）
6. **Planning** — Agent 制定计划并分解目标为子任务
7. **Multi-Agent Collaboration** — 多 Agent 协作（Coordinator、Swarm、Crew）
8. **Memory Management** — 短期/长期记忆、显式状态更新、向量存储
9. **Learning and Adaptation** — 从反馈中学习并自适应调整行为
10. **MCP (Model Context Protocol)** — 标准化工具发现和调用协议
11. **Goal Setting and Monitoring** — 目标设定、迭代追踪、完成检测
12. **Exception Handling and Recovery** — 异常处理、降级策略、重试机制
13. **Human-in-the-Loop** — 人在回路：审批、纠正、指导
14. **Knowledge Retrieval (RAG)** — 检索增强生成：向量搜索、文档注入
15. **Inter-Agent Communication (A2A)** — Agent 间通信协议（Agent Card、Streaming）
16. **Resource-Aware Optimization** — 资源感知优化：Token、成本、延迟
17. **Reasoning Techniques** — 推理技术：CoT、Self-Correction、代码执行推理
18. **Guardrails / Safety** — 安全护栏：输入验证、输出过滤、LLM-as-Guardrail
19. **Evaluation and Monitoring** — 评估与监控：LLM-as-Judge、响应质量评估
20. **Prioritization** — 任务优先级管理：排序、调度、资源分配
21. **Exploration and Discovery** — 探索与发现：Agent Laboratory、研究自动化

## 当前 Gap 映射（来自架构分析）

| 模式 | 当前状态 |
|------|----------|
| Prompt Chaining | 未实现 — ThinkingState 单次调用 LLM，无链式分解 |
| Routing | 未实现 — 无 Coordinator 路由逻辑 |
| Parallelization | 未实现 — 单线程执行 |
| Reflection | 未实现 — 无自我评估/迭代改进机制 |
| Tool Use | 部分实现 — ToolCallingStateHandler 为存根，无 Tool Registry |
| Planning | 未实现 — THINKING 状态无任务分解 |
| Multi-Agent | 未实现 — 单 Agent 执行，无多 Agent 编排 |
| Memory | 部分实现 — Chat Memory 有，但长期记忆/向量存储无读取路径 |
| Adaptation | 未实现 — Shadow Review 为存根 |
| MCP | 未实现 — 无 MCP 服务器/客户端集成 |
| Goal Setting | 未实现 — 无目标追踪机制 |
| Exception Handling | 部分实现 — 有 Failed/Retrying 状态，但无结构化降级策略 |
| HITL | 部分实现 — PAUSED 状态存在，但无 UI 审批流 |
| RAG | 未实现 — ContextInjector 为存根，无向量搜索 |
| Inter-Agent (A2A) | 未实现 — 无 Agent 间通信 |
| Resource Optimization | 部分实现 — TokenBudget、Admission Control 有，但无动态优化 |
| Reasoning | 部分实现 — 基础 CoT 通过 LLM，但无 Self-Correction |
| Guardrails | 未实现 — 无安全护栏 |
| Evaluation | 部分实现 — ToolErrorCategory 刚添加，评估框架计划中 |
| Prioritization | 未实现 — 无任务队列优先级 |
| Exploration | 未实现 — 无研究自动化 |

## 任务

请从您的专业视角，将这 21 个模式分配到以下三层成熟度模型中：

- **Layer 1: 基础能力（Foundation）** — 当前架构正常运行所必需，缺失会导致系统不可用或不可靠
- **Layer 2: 生产级能力（Production-Ready）** — 从 Demo 到生产环境必须补足的差距
- **Layer 3: 高级/差异化能力（Advanced/Differentiation）** — 提升竞争力、可延后实现

对每一层，请给出：
1. 包含哪些模式（编号+名称）
2. 推荐理由（为什么属于这一层）
3. 对 SchemaPlexAI 当前架构的具体建议
