---
title: DeerFlow 架构调研
status: researching
project: deer-flow
source: https://github.com/bytedance/deer-flow
creation_date: 2026-04-30
---

# DeerFlow 架构调研

> 字节跳动开源的"超级 Agent Harness"，用于研究、编码、创意等长时程任务。

## 一句话描述

基于 LangGraph 的模块化多 Agent 架构，通过 Lead Agent + 动态子 Agent + 11层中间件链处理分钟到小时级别的复杂任务。

## 核心架构

```
User Request
    ↓
Lead Agent (LangGraph StateGraph)
    ↓
Middleware Chain (11 layers)
    ↓
Sub-Agent Delegation (并行 scoped agents)
    ↓
Sandboxed Execution (Docker / K8s)
    ↓
Checkpointing (SQLite/Postgres/Redis)
    ↓
Result Synthesis
```

## 关键设计决策

### 1. 分层中间件链

- 每层中间件处理一个横切关注点（记忆、循环检测、工具错误处理、澄清、上传等）
- 开发者可通过**堆叠/移除中间件**扩展能力，无需修改核心 Agent 循环
- MemoryMiddleware 固定在位置 #8，每次 Agent Turn 自动执行

### 2. 子 Agent 编排（Fan-out / Fan-in）

- Lead Agent 接收请求、拆解子任务、动态生成带作用域的子 Agent
- 子 Agent 并行执行（数据爬取、图像生成、代码编写可同时运行）
- 隔离上下文：子 Agent 彼此不可见，防止上下文污染
- 结果回传：结构化结果汇总到 Lead Agent，合成统一交付物

### 3. 渐进式 Skill 加载

- Skill 定义为 **Markdown + YAML frontmatter**（`SKILL.md`）
- **按需加载**：仅在当前任务需要时加载，保持上下文窗口精简
- 可组合：多个 Skill 可组合为复合工作流

### 4. 状态持久化

- LangGraph Checkpointing 支持 SQLite/PostgreSQL/Redis
- 崩溃后从最近 Checkpoint 恢复，而非重新开始
- 使用唯一 `thread_id` 追踪每次运行

### 5. 记忆系统

- 异步中间件管道：过滤对话 → 30秒防抖 → LLM 提取器生成记忆差异 → 去重 → 持久化
- 跨会话保留：用户偏好、品牌声音、项目结构随时间积累

## 可借鉴设计

1. **中间件链模式**：我们的 Agent 执行引擎可引入可插拔的中间件链来处理记忆、循环检测、人类介入等横切关注点
2. **Lead Agent + 子 Agent 分层**：当前 Agent 是单体的，可演进为 Lead Agent 编排多个 Specialist Agent
3. **Skill 即 Markdown**：用 Markdown + YAML 定义 Agent 能力，降低创建门槛
4. **渐进式加载**：Agent 配置/Skill 按需加载，减少每次调用的上下文负担
5. **记忆中间件**：将记忆提取作为标准中间件，自动维护跨会话上下文

## 参考链接

- [GitHub - bytedance/deer-flow](https://github.com/bytedance/deer-flow)
- [DeerFlow ARCHITECTURE.md](https://github.com/bytedance/deer-flow/blob/main/backend/docs/ARCHITECTURE.md)
- [Mem0 - How Memory Works in DeerFlow](https://mem0.ai/blog/how-memory-works-in-deerflow)
