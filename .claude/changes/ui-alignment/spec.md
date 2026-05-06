# UI/UE 对齐与架构一致性修复 — Spec

## 概述

当前前端实现与 Abyss Hive 设计系统严重脱节，页面内容与后端架构也不匹配。本变更将对齐 UI/UE 效果，并确保前端页面与后端服务能力一致。

## 现状 Gap 分析

### UE 设计不符

| 设计规范 | 当前实现 | 差距 |
|---|---|---|
| `/cockpit` 沉浸式驾驶舱 | 缺失，用 `/dashboard` 替代 | 核心页面未实现 |
| `/canvas` Agent DAG 编辑器 | 缺失 | 核心页面未实现 |
| `/agents` AgentDetail（身份卡片+标签页） | `/agents` 是 AgentManager 表格 | 页面类型错误 |
| `/workflows` WorkflowMonitor（Gantt） | `/workflows` 是普通表格 | 功能/视觉不符 |
| Immersive/Progressive Layout | 已编码但 router 未接入 | 布局系统未激活 |
| HexIcon/StatCard/PillNav/TerminalLog | 缺失 | 设计组件未实现 |

### 架构一致性不符

- 多个页面仍使用硬编码 mockData（SpecCenter, WorkflowCenter, ContextCenter, QualityCenter 等）
- 后端 API 已就绪（AgentConfigService, SpecService, WorkflowService 等），前端未完整对接

## 范围

**In Scope:**
- Router 改造：接入 ImmersiveLayout / ProgressiveLayout
- 新增页面：/cockpit, /canvas
- 重构页面：/agents → AgentDetail, /workflows → WorkflowMonitor
- 实现核心组件：HexIcon, StatCard, PillNav, TerminalLog
- API 对接补完：所有页面调用真实后端 API

**Out Scope:**
- 后端 API 开发（已就绪）
- 全新的设计系统定义（已就绪）
- 移动端适配

## API 规格

使用已有 API 层（`src/api/*.ts`），确保所有页面调用真实端点：
- Agent: `/agent-config/agents`, `/api/v1/agents/stats`, `/api/v1/agents/executions`
- Spec: `/spec/**`
- Workflow: `/workflow/templates`, `/workflow/instances`
- Context: `/context/**`
- Quality: `/quality/**`
- Integration: `/integration/**`
- Ops: `/ops/**`

## 性能目标

- 首屏加载 < 2s（Vite + lazy load）
- API 响应错误时显示友好提示（antd message）
- 截图测试覆盖所有路由（Playwright）

## 错误场景

- API 401 → 跳转登录
- API 500 → message.error 提示 + 重试按钮
- 网络断开 → 离线提示

---
> 关联设计: `wiki/frontend/abyss-hive-design.md`, `wiki/frontend/structure.md`
