---
topic: cursor-sdk-to-opensandbox
stage: decision
version: v1.0
status: 已批准
supersedes: ""
---

# ADR-002: 放弃 Cursor SDK，转向自建 Agent Runtime + OpenSandbox

> **日期**: 2026-04-30
> **决策人**: 架构评审委员会
> **状态**: 已批准

---

## 背景

项目初期规划使用 Cursor SDK (`@cursor/sdk`) 作为外部 Runtime Daemon，期望快速获得以下能力：

- 代码智能补全与生成
- 终端命令执行
- 文件系统操作
- 浏览器自动化

经深入技术调研发现，Cursor SDK 存在根本性的自主可控问题。

## 决策

**放弃 Cursor SDK 接入方案**，转向 **OpenSandbox 作为沙箱底座 + 自有 `schemaplexai-agent-engine` 作为智能核心** 的完全自建方案。

### 方案对比

| 维度 | Cursor SDK（原方案） | OpenSandbox + 自建引擎（新方案） |
|------|---------------------|--------------------------------|
| **控制权** | 强制依赖 Cursor API Key 和控制平面，受第三方商业策略约束 | 完全自主可控，开源底座可私有化部署 |
| **数据安全** | 代码/提示词必须上传 Cursor 云端 | 数据完全留存本地/自有基础设施 |
| **成本可控** | 按 Cursor Pro 订阅收费，无法预估规模成本 | 仅计算自有算力成本，可优化 |
| **定制化** | SDK 能力边界固定，无法扩展 | 可深度定制工具集、安全策略、执行环境 |
| **稳定性** | 依赖 Cursor 服务可用性 | 依赖自有基础设施，SLA 自主保障 |
| **接入复杂度** | 低（SDK 封装好） | 中（需集成 OpenSandbox Java SDK + 自建编排） |

### 新架构组成

```
┌─────────────────────────────────────────────┐
│         schemaplexai-agent-engine           │
│  ┌──────────────┐  ┌──────────────────────┐ │
│  │ AgentState   │  │  Tool Registry       │ │
│  │  Machine     │  │  (Git/Shell/Browser) │ │
│  └──────────────┘  └──────────────────────┘ │
└──────────┬──────────────────────────────────┘
           │ gRPC / REST
           ▼
┌─────────────────────────────────────────────┐
│           OpenSandbox Daemon                │
│  ┌──────────┐ ┌──────────┐ ┌─────────────┐ │
│  │  Filesys │ │ Terminal │ │   Browser   │ │
│  │   Box    │ │  Sandbox │ │   Sandbox   │ │
│  └──────────┘ └──────────┘ └─────────────┘ │
└─────────────────────────────────────────────┘
```

### 关键变更

| 原方案 | 新方案 | 影响 |
|--------|--------|------|
| 无 Task Board | **新增 Task Board（看板）核心编排层** | 支持人工/AI 自动任务分配，实现 Iterative Kanban |
| `ToolCallingStateHandler` 空实现（JVM 内模拟） | 工具调用在 **OpenSandbox 隔离沙箱** 中真实执行 | 支持文件系统、浏览器、终端、代码执行等 Computer Use 能力 |
| 依赖 Cursor Runtime | 依赖 **OpenSandbox + 自研状态机** | 工期 +1~2 周（Phase 2） |

## 替代方案

| 方案 | 评估 | 结论 |
|------|------|------|
| 继续使用 Cursor SDK | 快速上线，但失去自主可控底线 | 拒绝 |
| 使用 Devin / OpenAI Operator 等替代 SDK | 同样存在第三方锁定问题 | 拒绝 |
| **OpenSandbox + 自建引擎** | 开源底座 + 自有智能核心，完全可控 | **采纳** |
| 完全自研沙箱（不依赖 OpenSandbox） | 安全性和灵活性最高，但工作量过大（+8周） | 拒绝 |

## 影响

- **工期影响**：Phase 2（Agent 引擎）增加 OpenSandbox Java SDK 集成任务，预计 **+1~2 周**
- **兼容现有资产**：agent-engine 状态机、`TokenBudget`、`AdmissionControl` 全部保留，仅执行后端从"本地 JVM 空跑"升级为"沙箱真实执行"
- **新增组件**：Task Board 编排层、OpenSandbox 适配器、沙箱工具注册器
- **风险**：OpenSandbox Java SDK 成熟度待验证，需 Phase 0 并行预研

## 相关文档

- `docs/designs/agent-runtime-task-board.md` v1.0
- `docs/plans/project-plan.md` v1.1（附录 B）
