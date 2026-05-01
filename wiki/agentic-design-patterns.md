---
title: Agentic Design Patterns 预研总结
version: 1.0
status: preliminary
source: Antonio Gulli, "Agentic Design Patterns: A Hands-On Guide to Building Intelligent Systems"
source_path: D:\Downloads\Agentic-Design-Patterns-main
updated: 2026-05-01
note: 本文件为初步版，待圆桌辩论（4位专家视角）完成后更新为终版
---

# Agentic Design Patterns 预研总结

> 基于 Antonio Gulli《Agentic Design Patterns》书籍的 21 章 + 9 附录、58 个 notebook 的预研成果。

---

## 一、模式全景图

### 1. 编排模式（Orchestration Patterns）

| # | 模式 | 核心概念 | 关键实现 |
|---|------|---------|---------|
| 1 | **Prompt Chaining** | 将复杂任务分解为串联的提示链，每步输出作为下一步输入 | LCEL 管道、RunnableSequence、模板嵌套 |
| 2 | **Routing** | 根据输入特征路由到不同的专用 Agent/Handler | RunnableBranch、Coordinator Agent、条件判断 |
| 3 | **Parallelization** | 并行执行多个独立子任务，聚合结果 | 异步并发、Map-Reduce、结果合并 |
| 4 | **Reflection** | Agent 自我评估输出质量，迭代改进 | 生成→批判→精修三步循环、自我纠错 |

### 2. Agent 核心模式（Core Agent Patterns）

| # | 模式 | 核心概念 | 关键实现 |
|---|------|---------|---------|
| 5 | **Tool Use** | Agent 调用外部工具扩展能力边界 | Function Calling、Tool Registry、MCP、代码执行 |
| 6 | **Planning** | 将高层目标分解为可执行的子任务计划 | Planner Agent、Task DAG、目标拆解 |
| 7 | **Multi-Agent Collaboration** | 多个 Agent 协作完成复杂任务 | Coordinator、Swarm、Crew、Sequential/Parallel/Loop |
| 8 | **Memory Management** | 管理短期/长期记忆，支持跨会话知识 | BufferMemory、Vector Store、显式状态更新、Session Service |

### 3. 进阶能力模式（Advanced Capability Patterns）

| # | 模式 | 核心概念 | 关键实现 |
|---|------|---------|---------|
| 9 | **Learning and Adaptation** | 从反馈中学习，自适应调整 Agent 行为 | Shadow Review、反馈闭环、指令自优化 |
| 10 | **MCP (Model Context Protocol)** | 标准化工具发现和调用协议 | FastMCP Server/Client、Tool Schema、@tool 装饰器 |
| 11 | **Goal Setting and Monitoring** | 目标设定、迭代追踪、完成检测 | OKR 对齐、进度追踪、里程碑检测 |
| 12 | **Exception Handling and Recovery** | 异常处理、降级策略、重试机制 | Fallback Chain、Circuit Breaker、结构化降级 |
| 13 | **Human-in-the-Loop** | 人在回路：审批、纠正、指导 | 审批流 UI、权限控制、回调继续、审计留痕 |

### 4. 知识与通信模式（Knowledge & Communication Patterns）

| # | 模式 | 核心概念 | 关键实现 |
|---|------|---------|---------|
| 14 | **Knowledge Retrieval (RAG)** | 检索增强生成，基于私有知识回答问题 | 文档分块、Embedding、Vector Search（Milvus/Weaviate）、上下文注入 |
| 15 | **Inter-Agent Communication (A2A)** | Agent 间标准化通信协议 | Agent Card、Streaming、Task Request/Response、技能发现 |

### 5. 优化与评估模式（Optimization & Evaluation Patterns）

| # | 模式 | 核心概念 | 关键实现 |
|---|------|---------|---------|
| 16 | **Resource-Aware Optimization** | 资源感知优化：Token、成本、延迟 | 动态预算、成本追踪、模型降级、缓存策略 |
| 17 | **Reasoning Techniques** | 推理技术增强决策质量 | CoT (Chain-of-Thought)、Self-Correction、代码执行推理 |
| 18 | **Guardrails / Safety** | 安全护栏防止有害输出 | 输入验证、输出过滤、LLM-as-Guardrail、分类器 |
| 19 | **Evaluation and Monitoring** | 评估与监控确保输出质量 | LLM-as-Judge、Rubric 评分、Trace/Span、质量门禁 |
| 20 | **Prioritization** | 任务优先级管理和资源分配 | 队列排序、动态调度、SLA 保障 |
| 21 | **Exploration and Discovery** | 探索与发现：研究自动化 | Agent Laboratory、自动知识图谱、假设验证 |

---

## 二、框架覆盖映射

书籍中的 58 个 notebook 覆盖了以下框架：

| 框架 | 覆盖章节 | 特点 |
|------|---------|------|
| **Google ADK** | Ch 2, 3, 4, 7, 8, 10, 13, 18 | Google 官方 Agent 框架，LlmAgent + Tool + Session/Memory Service |
| **LangChain / LCEL** | Ch 1, 2, 3, 4, 5, 8, 14 | 管道编排、Runnable 抽象、Memory 组件 |
| **LangGraph** | Ch 2, 8, 14 | 图结构状态机、节点-边模型、持久化状态 |
| **CrewAI** | Ch 5, 7 | Multi-Agent Crew、Role-Based、Process 编排 |
| **OpenRouter** | Ch 2 | 统一 LLM API 路由 |
| **Vertex AI** | Ch 5, 14 | Google 云端 RAG 和搜索 |
| **FastMCP** | Ch 10 | 轻量级 MCP 服务器/客户端 |

---

## 三、关键洞察

1. **Pattern 之间存在强依赖关系**：Tool Use 是 Reflection、Planning、Multi-Agent 的基础；Memory 是 Learning 和长期 RAG 的前提；Guardrails 是所有生产场景的安全基线。

2. **MCP 正在成为行业标准**：Anthropic 主导的 Model Context Protocol 统一了工具发现协议，与 Function Calling 互补。

3. **A2A 协议崭露头角**：Google 发布的 Agent-to-Agent 协议（Agent Card + Streaming）为跨平台 Agent 协作提供了标准。

4. **Evaluation 是生产化的前提**：没有 LLM-as-Judge 和质量门禁，企业不敢将 Agent 用于关键业务。

5. **HITL 是企业合规刚需**：金融、医疗、制造业等 regulated industries 要求关键决策必须人工审批。
