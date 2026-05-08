---
topic: ui-architecture-alignment
stage: design
version: v1.0
status: approved
date: 2026-05-08
supersedes: ""
---

# 设计方案：前端页面与架构对齐重设计

## Context

当前前端 UI 按"功能区域"分解（agents, workflows, specs...），后端按"领域服务"分解（agent-config, agent-engine, context, quality...）。两种分解策略导致结构性不匹配：AgentManager 合并了两个服务、system/task 服务无 UI 页面、多个页面只有扁平表格、存在死代码（WorkflowCenter、Dashboard）。已有 ui-alignment 修复了视觉层（Hive 组件、Layout 模式），但未解决结构对齐问题。

本方案采用**领域分组 + 子路由**策略，将 11 个后端服务映射到 6 个顶级前端域，全面重设计前端页面结构。

### 设计参考文档

| 文档 | 路径 | 用途 |
|---|---|---|
| Abyss Hive 设计系统 | `wiki/frontend/abyss-hive-design.md` | 颜色、字体、布局模式、组件规范 |
| 前端结构 | `wiki/frontend/structure.md` | 当前前端架构 |
| Task Board 设计 | `docs/designs/2026-04-30-v1.0-agent-runtime-task-board.md` | 7 列看板、SfTask 数据模型、ADR-012 |
| 系统架构 | `docs/designs/2026-04-29-v1.0-system-architecture.md` | 后端服务拓扑 |
| UI 对齐 Spec | `docs/specs/2026-05-07-v1.0-ui-alignment.md` | 已完成的视觉对齐工作 |

---

## 1. 导航与路由结构

### 顶级导航（6 项）

| 路由 | 图标 | 中文 | 英文 | Layout |
|---|---|---|---|---|
| `/cockpit` | 仪表盘 | 驾驶舱 | Cockpit | Immersive |
| `/agents` | 机器人 | 智能体 | Agents | Progressive |
| `/projects` | 文件夹 | 项目 | Projects | Progressive |
| `/quality` | 盾牌 | 质量 | Quality | Progressive |
| `/platform` | 设置 | 平台 | Platform | Progressive |
| `/tasks` | 闪电 | 任务 | Tasks | Progressive |

### 完整路由表

```
/cockpit                              → Cockpit (跨域概览, Immersive)

/agents                               → 重定向到 /agents/list
/agents/list                          → AgentList (agent-config 服务)
/agents/executor                      → AgentExecutor (agent-engine 服务)
/agents/:id                           → AgentDetail (agent-engine 服务)
/agents/canvas                        → AgentCanvas (DAG 编辑器, Immersive)

/projects                             → 重定向到 /projects/specs
/projects/specs                       → SpecCenter (spec 服务)
/projects/workflows                   → WorkflowCenter (workflow 服务)
/projects/contexts                    → ContextCenter (context 服务)

/quality                              → 重定向到 /quality/gates
/quality/gates                        → QualityGates (quality 服务)
/quality/issues                       → QualityIssues (quality 服务)
/quality/security                     → SecurityAudit (quality 服务)

/platform                             → 重定向到 /platform/system
/platform/system                      → SystemCenter (system 服务)
/platform/integrations                → IntegrationCenter (integration 服务)
/platform/ops                         → OpsCenter (ops 服务)

/tasks                                → TaskBoard (看板, task 服务)
/tasks/jobs                           → TaskJobs (后台监控, task 服务)
/tasks/:id                            → TaskDetail (task 服务)
```

### 服务-页面映射

| 后端服务 | 端口 | 前端页面 |
|---|---|---|
| gateway (8080) | 基础设施 | — |
| system (8081) | `/platform/system` | SystemCenter |
| web (8082) | 基础设施 | — |
| agent-config (8083) | `/agents/list` | AgentList |
| agent-engine (8084) | `/agents/executor`, `/agents/:id`, `/agents/canvas` | AgentExecutor, AgentDetail, AgentCanvas |
| context (8085) | `/projects/contexts` | ContextCenter |
| spec (8086) | `/projects/specs` | SpecCenter |
| workflow (8087) | `/projects/workflows` | WorkflowCenter |
| integration (8088) | `/platform/integrations` | IntegrationCenter |
| ops (8089) | `/platform/ops` | OpsCenter |
| quality (8090) | `/quality/gates`, `/quality/issues`, `/quality/security` | QualityGates, QualityIssues, SecurityAudit |
| task (8091) | `/tasks`, `/tasks/jobs` | TaskBoard, TaskJobs |

---

## 2. 页面设计

### 2.1 Cockpit（驾驶舱）
- **Layout**: ImmersiveLayout
- **职责**: 跨域运营概览
- **内容**:
  - 4 个 StatCard：活跃 Agent 数、今日执行数、Token 消耗、待审批工单
  - Agent 状态轨道图（HexIcon 可视化）
  - 最近执行时间线（跨 agent/workflow/quality 的最近事件）
- **API**: 聚合调用 agent-engine + quality + ops

### 2.2 Agents（智能体域）
- **AgentList** `/agents/list`：表格视图，来自 agent-config 服务。CRUD、搜索、分页。替代 AgentManager。
- **AgentExecutor** `/agents/executor`：保持现有 SSE 执行界面
- **AgentDetail** `/agents/:id`：身份卡片 + 标签页（metrics/logs/charts/config）
- **AgentCanvas** `/agents/canvas`：DAG 编辑器，ImmersiveLayout

### 2.3 Projects（项目域）
- **SpecCenter** `/projects/specs`：规格文档管理，来自 spec 服务
- **WorkflowCenter** `/projects/workflows`：Tab 布局，Tab 1 为模板管理（CRUD 表格，来自 workflow 服务），Tab 2 为实例监控（Gantt 时间线，复用现有 WorkflowMonitor 的 buildTimelineMeta 逻辑）
- **ContextCenter** `/projects/contexts`：知识库管理，来自 context 服务

### 2.4 Quality（质量域）
拆分为 3 个子页面：
- **QualityGates** `/quality/gates`：质量门禁定义与评估结果
- **QualityIssues** `/quality/issues`：质量问题追踪
- **SecurityAudit** `/quality/security`：安全策略与审计事件

### 2.5 Platform（平台域）
- **SystemCenter** `/platform/system`：用户/租户/角色/权限管理 + AI 模型配置
- **IntegrationCenter** `/platform/integrations`：外部集成管理
- **OpsCenter** `/platform/ops`：制品、成本分析、预算、交付

### 2.6 Tasks（任务域）— 基于 `docs/designs/2026-04-30-v1.0-agent-runtime-task-board.md`

- **TaskBoard** `/tasks`：**Iterative Kanban 7 列看板**（设计文档 ADR-012）
  - 列：BACKLOG → QUEUED → IN_PROGRESS → AWAITING_REVIEW → REVISING → BLOCKED → DONE
  - **AWAITING_REVIEW 为强制人工关卡**：Agent 执行完成后必须进入此列，不可自动跳到 DONE
  - Reviewer 操作：Approve → DONE / Request Changes → REVISING / Escalate → 转人类工程师
  - 卡片信息：标题、描述、技能标签 (skillTags)、优先级 (P0-P3)、分配类型 (MANUAL/AUTO/MIXED)、关联 Spec
  - @dnd-kit 拖拽支持列间移动
  - **数据模型**：`SfTask`（id, tenantId, title, description, skillTags, priority, status, assignedRuntimeId, assignedAgentId, assignmentType, specId, blockerReason）
  - **依赖后端**：task 服务需新增 `sf_task`、`sf_task_comment`、`sf_task_assignment_log` 表及 CRUD API
  - **降级方案**：若后端 API 未就绪，前端使用 mock 数据 + Zustand 本地状态先行开发

- **TaskJobs** `/tasks/jobs`：RabbitMQ 消费者状态、后台作业列表、重试/取消
- **TaskDetail** `/tasks/:id`：单任务详情 + 评论/反馈流（sf_task_comment）

---

## 3. Layout 系统

### ImmersiveLayout（沉浸式）
- **用途**: 驾驶舱、画布等全屏沉浸页面
- **Sidebar**: 52px 图标栏，显示全部 6 个域图标 + canvas
  - cockpit、canvas → 当前页面高亮，保持 ImmersiveLayout
  - 其他 5 个图标 → 点击跳转 ProgressiveLayout 对应路由
- **Header**: 浮动胶囊，SchemaPlexAI + 实时统计
- **路由**: `/cockpit`、`/agents/canvas`

### ProgressiveLayout（渐进式）
- **用途**: 所有管理类页面
- **Sidebar**: 200px，6 个顶级导航项 + 子菜单展开
  - 点击顶级项展开子路由列表
  - 当前选中项高亮，父级自动展开
  - 底部：语言切换 + 租户选择
- **Header**: 48px，品牌名 + 语言 + 租户 + 通知 + 头像
- **Content**: `<Outlet/>` 渲染子路由

### 导航交互规则
- 进入 `/agents` 自动展开 Agents 子菜单，重定向到 `/agents/list`
- 进入 `/projects` 自动展开 Projects 子菜单，重定向到 `/projects/specs`
- 子菜单展开/收起状态持久化到 localStorage

---

## 4. 数据流与 API 对齐

### API 路由映射

| 前端页面 | 后端服务 | API 前缀 |
|---|---|---|
| Cockpit | 聚合 | 多服务 |
| AgentList | agent-config | `/agent-config/**` |
| AgentExecutor | agent-engine | `/agent/**` |
| AgentDetail | agent-engine | `/agent/**` |
| AgentCanvas | agent-engine | `/agent/**` |
| SpecCenter | spec | `/spec/**` |
| WorkflowCenter | workflow | `/workflow/**` |
| ContextCenter | context | `/context/**` |
| QualityGates/Issues/Security | quality | `/quality/**` |
| SystemCenter | system | `/system/**` + `/auth/**` |
| IntegrationCenter | integration | `/integration/**` |
| OpsCenter | ops | `/ops/**` |
| TaskBoard/TaskJobs | task | `/task/**` |

### 数据流模式（保持不变）

```
Page Component → API Hook (src/api/*.ts) → Axios → Gateway (8080) → Backend Service
Page Component → Zustand Store → localStorage (auth/tenant)
```

### 改进点
- 分离 `api/agent.ts` 为 `api/agent-config.ts` + `api/agent-engine.ts`
- 统一错误处理：401→登录，500→message.error+重试，断网→离线提示
- 移除所有硬编码 mockData

---

## 5. 组件架构

### Hive 组件库 — 遵循 `wiki/frontend/abyss-hive-design.md`

现有保持：HexIcon, StatCard, PillNav, TerminalLog

新增：
- `KanbanBoard` — 7 列看板容器（基于 @dnd-kit/core + @dnd-kit/sortable，列定义来自 Task Board 设计文档）
- `TaskCard` — 任务卡片（标题、技能标签、优先级 P0-P3、分配类型、关联 Spec）
- `DomainNav` — 域导航组件（ProgressiveLayout sidebar 子菜单展开逻辑提取为独立组件）

**Abyss Hive 设计规范（所有页面必须遵循）**：
- 背景层级：Abyss `#0a0e1a` → Hive Wall `#0d1117` → Honeycomb Card `#111827`
- 荧光色：Cyan `#00d4aa`（正常）、Amber `#ff9f43`（执行中）、Red `#ff4757`（异常）
- 字体：Inter + Noto Sans SC + JetBrains Mono
- 圆角：8px 标准，12px 最大
- 布局模式：Immersive（无边界、隐藏导航）vs Progressive（结构化、持久导航）

### 清理项

| 清理项 | 说明 |
|---|---|
| 删除 WorkflowCenter 死代码 | 功能合并到 /projects/workflows |
| 删除 Dashboard 死代码 | 已被 Cockpit 替代 |
| 删除 AgentManager | 替换为 AgentList |
| 分离 agent API | 拆为 agent-config.ts + agent-engine.ts |
| 删除 SystemSettings 旧代码 | 扩展为 SystemCenter |
| 统一 i18n key | nav.xxx → nav.domain.xxx 格式 |

### 目标文件结构

```
src/
  api/
    agent-config.ts    ← 新
    agent-engine.ts    ← 新
    context.ts
    integration.ts
    ops.ts
    quality.ts
    spec.ts
    system.ts          ← 新
    task.ts            ← 新
    workflow.ts
  components/
    Hive/
      KanbanBoard.tsx  ← 新
      TaskCard.tsx     ← 新
      DomainNav.tsx    ← 新
      HexIcon.tsx
      StatCard.tsx
      PillNav.tsx
      TerminalLog.tsx
    Layout/
      ImmersiveLayout.tsx   ← 修改
      ProgressiveLayout.tsx ← 修改
  pages/
    Cockpit/
    AgentCanvas/
    AgentList/              ← 新（替代 AgentManager）
    AgentExecutor/
    AgentDetail/
    Projects/
      SpecCenter/
      WorkflowCenter/       ← 复活 + 重构
      ContextCenter/
    Quality/
      QualityGates/         ← 新
      QualityIssues/        ← 新
      SecurityAudit/        ← 新
    Platform/
      SystemCenter/         ← 扩展自 SystemSettings
      IntegrationCenter/
      OpsCenter/
    Tasks/
      TaskBoard/            ← 新（看板）
      TaskJobs/             ← 新
    Login/
    NotFound/
```

---

## 6. 实施阶段

### Phase 1: 基础设施（路由 + Layout）
- 修改 router/index.tsx：新路由结构
- 修改 ImmersiveLayout：扩展 6 域图标导航
- 修改 ProgressiveLayout：域导航 + 子菜单展开
- 创建 DomainNav 组件
- 更新 i18n key

### Phase 2: 页面迁移与新建
- AgentList 替代 AgentManager
- WorkflowCenter 复活 + 重构
- Quality 域拆分为 3 个子页面
- SystemCenter 扩展自 SystemSettings
- TaskBoard（看板）+ TaskJobs 新建

### Phase 3: API 对齐
- 分离 agent API
- 所有页面对接真实 API
- 移除 mockData
- 统一错误处理

### Phase 4: 清理与测试
- 删除死代码（Dashboard、旧 AgentManager、旧 SystemSettings）
- 修复所有测试
- Playwright 截图基线更新

---

## 7. 验证方案

1. `npm run lint` — 零错误
2. `npm run test:run` — 全部通过
3. `npm run build` — 构建成功
4. Playwright 截图覆盖所有路由
5. 手动验证：每个顶级域导航 → 子路由切换 → 数据加载
6. 手动验证：ImmersiveLayout ↔ ProgressiveLayout 切换流畅
7. 手动验证：看板拖拽功能

---

## 8. 关键文件清单

| 文件 | 操作 |
|---|---|
| `src/router/index.tsx` | 重写 |
| `src/components/Layout/ImmersiveLayout.tsx` | 修改 |
| `src/components/Layout/ProgressiveLayout.tsx` | 修改 |
| `src/components/Hive/KanbanBoard.tsx` | 新建 |
| `src/components/Hive/TaskCard.tsx` | 新建 |
| `src/components/Hive/DomainNav.tsx` | 新建 |
| `src/pages/AgentList/index.tsx` | 新建 |
| `src/pages/Projects/SpecCenter/index.tsx` | 移动 |
| `src/pages/Projects/WorkflowCenter/index.tsx` | 重构 |
| `src/pages/Projects/ContextCenter/index.tsx` | 移动 |
| `src/pages/Quality/QualityGates/index.tsx` | 新建 |
| `src/pages/Quality/QualityIssues/index.tsx` | 新建 |
| `src/pages/Quality/SecurityAudit/index.tsx` | 新建 |
| `src/pages/Platform/SystemCenter/index.tsx` | 重构 |
| `src/pages/Platform/IntegrationCenter/index.tsx` | 移动 |
| `src/pages/Platform/OpsCenter/index.tsx` | 移动 |
| `src/pages/Tasks/TaskBoard/index.tsx` | 新建 |
| `src/pages/Tasks/TaskJobs/index.tsx` | 新建 |
| `src/api/agent-config.ts` | 新建 |
| `src/api/agent-engine.ts` | 新建 |
| `src/api/system.ts` | 新建 |
| `src/api/task.ts` | 新建 |
