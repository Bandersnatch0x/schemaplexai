---
title: open-agents 架构调研
status: researching
project: open-agents
source: https://github.com/vercel-labs/open-agents
creation_date: 2026-04-30
---

# open-agents 架构调研

> Vercel Labs 开源的云 Agent 模板，Next.js + Workflow SDK + Sandbox 三层架构。

## 一句话描述

开源的云原生 Agent 执行模板，核心设计是 Agent 逻辑与沙箱执行环境的严格分离。

## 核心架构

```
Web → Agent Workflow → Sandbox VM
```

| 层级 | 技术 | 职责 |
|------|------|------|
| Web | Next.js | Auth、会话、Chat UI、流式接口 |
| Agent | Vercel Workflow SDK | 持久化多步执行、工具编排、子 Agent |
| Sandbox | Vercel Sandboxes | 文件系统、Shell、Git、Dev Server、预览端口 |

## 关键设计决策

### 1. Agent ≠ Sandbox（严格分离）

- Agent 运行在 VM **外部**，通过工具与 VM 交互（文件读写、编辑、搜索、Shell 命令）
- 独立生命周期：沙箱可独立休眠/恢复
- 无请求生命周期耦合：Agent 执行超越单次 HTTP 请求
- LLM 选择可独立演进

### 2. Workflow 支撑的执行模型

- 聊天请求启动**持久化 Workflow Run**，而非内联执行
- 每次 Agent Turn 可跨多个 Workflow Step，状态持久化
- 流式恢复：通过重新连接现有 Workflow Stream 恢复活跃运行
- 快照化沙箱：使用基础快照 + 不活动后休眠

### 3. 仓库结构

```
apps/web         → Next.js 应用、Workflow、Auth、Chat UI
packages/agent   → Agent 实现、工具、子 Agent、Skills
packages/sandbox → 沙箱抽象 + Vercel Sandbox 集成
packages/shared  → 共享工具
```

## 可借鉴设计

1. **Agent 执行层与运行环境分离**：我们的 `agent-engine` 服务不应直接执行代码/命令，而是通过工具接口与隔离环境交互
2. **Workflow 持久化**：当前 Agent 执行是同步的，可引入持久化 Workflow/状态机来支持长时任务
3. **Snapshot + 休眠**：沙箱环境支持快照和休眠，降低资源占用
4. **Skills 生态**：Vercel 的 `skills.sh` 是 Agent 能力的 npm，我们可建立类似的 Skill 注册中心

## 参考链接

- [GitHub - vercel-labs/open-agents](https://github.com/vercel-labs/open-agents)
- [Vercel AI SDK](https://sdk.vercel.ai/docs)
