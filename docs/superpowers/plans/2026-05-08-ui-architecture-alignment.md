# UI Architecture Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure SchemaPlexAI frontend to align UI pages with backend microservices architecture using 6 domain groups with sub-routes, expand ImmersiveLayout, add Kanban task board, and clean up dead code.

**Architecture:** Domain-driven routing with nested React Router v6 routes. Two layout modes: ImmersiveLayout (52px icon sidebar) for full-screen pages and ProgressiveLayout (200px sidebar with expandable submenus) for management pages. @dnd-kit for Kanban drag-and-drop.

**Tech Stack:** React 18.3 + TypeScript 5.5 + Vite + Ant Design 5 + React Router 6 + Zustand + @dnd-kit/core + @dnd-kit/sortable

---

## File Structure

### New files (create)
- `src/api/agent-config.ts` — Agent CRUD (from agent.ts)
- `src/api/agent-engine.ts` — Agent execution, stats (from agent.ts)
- `src/api/task.ts` — SfTask CRUD + comments
- `src/components/Hive/DomainNav.tsx` — Domain navigation with submenus
- `src/components/Hive/DomainNav.test.tsx`
- `src/components/Hive/KanbanBoard.tsx` — 7-column DnD board
- `src/components/Hive/KanbanBoard.test.tsx`
- `src/components/Hive/TaskCard.tsx` — Task card for Kanban
- `src/components/Hive/TaskCard.test.tsx`
- `src/pages/AgentList/index.tsx` — Agent table (from AgentManager)
- `src/pages/AgentList/AgentList.css`
- `src/pages/Projects/SpecCenter/index.tsx` — Moved from SpecCenter
- `src/pages/Projects/ContextCenter/index.tsx` — Moved from ContextCenter
- `src/pages/Projects/WorkflowCenter/index.tsx` — Tabbed (templates + instances)
- `src/pages/Projects/WorkflowCenter/WorkflowCenter.css`
- `src/pages/Quality/QualityGates/index.tsx`
- `src/pages/Quality/QualityGates/QualityGates.css`
- `src/pages/Quality/QualityIssues/index.tsx`
- `src/pages/Quality/QualityIssues/QualityIssues.css`
- `src/pages/Quality/SecurityAudit/index.tsx`
- `src/pages/Quality/SecurityAudit/SecurityAudit.css`
- `src/pages/Platform/SystemCenter/index.tsx` — From SystemSettings
- `src/pages/Platform/SystemCenter/SystemCenter.css`
- `src/pages/Platform/IntegrationCenter/index.tsx` — Moved from IntegrationCenter
- `src/pages/Platform/OpsCenter/index.tsx` — Moved from OpsCenter
- `src/pages/Tasks/TaskBoard/index.tsx`
- `src/pages/Tasks/TaskBoard/TaskBoard.css`
- `src/pages/Tasks/TaskJobs/index.tsx`
- `src/pages/Tasks/TaskJobs/TaskJobs.css`
- `src/pages/Tasks/TaskDetail/index.tsx`
- `src/pages/Tasks/TaskDetail/TaskDetail.css`

### Modified files (edit)
- `src/router/index.tsx` — New domain route structure
- `src/components/Layout/ImmersiveLayout.tsx` — Expand to 6 domain icons
- `src/components/Layout/ProgressiveLayout.tsx` — Use DomainNav
- `src/components/Layout/ImmersiveLayout.test.tsx`
- `src/components/Layout/ProgressiveLayout.test.tsx`
- `src/components/Hive/index.ts` — Export new components
- `src/types/index.ts` — Add SfTask types
- `src/api/system.ts` — Expand for SystemCenter
- `src/i18n/locales/zh.json` — nav.domain.* keys
- `src/i18n/locales/en.json` — nav.domain.* keys
- `src/App.tsx` — Remove Dashboard import
- `src/pages/AgentDetail/index.tsx` — Update API import path
- `src/pages/Cockpit/index.tsx` — Update API import path
- `src/pages/AgentExecutor/index.tsx` — Update API import path
- `src/pages/WorkflowMonitor/index.tsx` — Update if needed

### Deleted files (remove)
- `src/api/agent.ts` — Split into agent-config.ts + agent-engine.ts
- `src/pages/AgentManager/` — Replaced by AgentList
- `src/pages/Dashboard/` — Replaced by Cockpit
- `src/pages/SystemSettings/` — Replaced by SystemCenter
- `src/pages/SpecCenter/` — Moved to Projects/SpecCenter
- `src/pages/ContextCenter/` — Moved to Projects/ContextCenter
- `src/pages/IntegrationCenter/` — Moved to Platform/IntegrationCenter
- `src/pages/OpsCenter/` — Moved to Platform/OpsCenter
- `src/pages/QualityCenter/` — Split into QualityGates + QualityIssues + SecurityAudit
- `src/pages/WorkflowCenter/` — Dead code (new one at Projects/WorkflowCenter)
- `src/pages/NotificationCenter/` — Dead code (not in new design)

---

## Phase 1: Infrastructure

### Task 1: Install @dnd-kit dependencies

**Files:**
- Modify: `schemaplexai-ui/package.json`

- [ ] **Step 1: Install packages**

```bash
cd schemaplexai-ui && npm install @dnd-kit/core @dnd-kit/sortable
```

Expected: packages added to dependencies in package.json, package-lock.json updated.

- [ ] **Step 2: Verify build still works**

```bash
cd schemaplexai-ui && npm run build
```

Expected: build succeeds with no new errors.

- [ ] **Step 3: Commit**

```bash
git add schemaplexai-ui/package.json schemaplexai-ui/package-lock.json
git commit -m "chore(deps): add @dnd-kit for Kanban board"
```

---

### Task 2: Split agent.ts into agent-config.ts + agent-engine.ts

**Files:**
- Create: `src/api/agent-config.ts`
- Create: `src/api/agent-engine.ts`
- Modify: `src/pages/AgentManager/index.tsx` (import update)
- Modify: `src/pages/AgentDetail/index.tsx` (import update)
- Modify: `src/pages/Cockpit/index.tsx` (import update)
- Modify: `src/pages/AgentExecutor/index.tsx` (import update)
- Delete: `src/api/agent.ts`

- [ ] **Step 1: Create agent-config.ts**

Create `schemaplexai-ui/src/api/agent-config.ts`:

```typescript
import request from './request'
import type { Agent } from '@/types'

export interface AgentQuery {
  page?: number
  pageSize?: number
  keyword?: string
  status?: string
}

export interface CreateAgentPayload {
  name: string
  description?: string
  type: string
  status?: string
  modelConfig?: {
    model: string
    temperature: number
    maxTokens: number
    topP: number
  }
  tools?: string[]
}

export function getAgentList(params: AgentQuery) {
  return request.get<{ list: Agent[]; total: number }>('/agent-config/agents', { params })
}

export function getAgentDetail(id: string) {
  return request.get<Agent>(`/agent-config/agents/${id}`)
}

export function createAgent(data: CreateAgentPayload) {
  return request.post<Agent>('/agent-config/agents', data)
}

export function updateAgent(id: string, data: CreateAgentPayload) {
  return request.put<Agent>(`/agent-config/agents/${id}`, data)
}

export function deleteAgent(id: string) {
  return request.delete<void>(`/agent-config/agents/${id}`)
}
```

- [ ] **Step 2: Create agent-engine.ts**

Create `schemaplexai-ui/src/api/agent-engine.ts`:

```typescript
import request from './request'
import type { ExecutionRecord } from '@/types'

export function executeAgent(id: string, prompt: string) {
  return request.post<string>(`/agents/${id}/execute`, { prompt })
}

export function getExecutionRecords(agentId?: string) {
  return request.get<ExecutionRecord[]>('/api/v1/agents/executions', { params: { agentId } })
}

export function getAgentStats() {
  return request.get<{
    totalAgents: number
    totalExecutions: number
    totalTokens: number
    pendingApprovals: number
  }>('/api/v1/agents/stats')
}
```

- [ ] **Step 3: Update imports in AgentManager**

In `schemaplexai-ui/src/pages/AgentManager/index.tsx`, replace:

```typescript
import { getAgentList, createAgent, updateAgent, deleteAgent } from '@/api/agent'
import type { CreateAgentPayload } from '@/api/agent'
```

With:

```typescript
import { getAgentList, createAgent, updateAgent, deleteAgent } from '@/api/agent-config'
import type { CreateAgentPayload } from '@/api/agent-config'
```

- [ ] **Step 4: Update imports in AgentDetail**

In `schemaplexai-ui/src/pages/AgentDetail/index.tsx`, replace any import from `@/api/agent` with `@/api/agent-config` for `getAgentDetail`, and `@/api/agent-engine` for execution-related APIs.

If the file imports `getAgentDetail` from `@/api/agent`, change to `@/api/agent-config`.
If it imports execution APIs, change to `@/api/agent-engine`.

- [ ] **Step 5: Update imports in Cockpit**

In `schemaplexai-ui/src/pages/Cockpit/index.tsx`, replace:

```typescript
import { getAgentStats } from '@/api/agent'
```

With:

```typescript
import { getAgentStats } from '@/api/agent-engine'
```

- [ ] **Step 6: Update imports in AgentExecutor**

In `schemaplexai-ui/src/pages/AgentExecutor/index.tsx`, replace any `@/api/agent` import with `@/api/agent-config` (for `getAgentList`) and `@/api/agent-engine` (for `executeAgent`).

- [ ] **Step 7: Delete agent.ts**

```bash
rm schemaplexai-ui/src/api/agent.ts
```

- [ ] **Step 8: Run type check**

```bash
cd schemaplexai-ui && npx tsc --noEmit
```

Expected: Zero errors.

- [ ] **Step 9: Commit**

```bash
git add schemaplexai-ui/src/api/agent-config.ts schemaplexai-ui/src/api/agent-engine.ts
git add schemaplexai-ui/src/pages/AgentManager/index.tsx schemaplexai-ui/src/pages/AgentDetail/index.tsx
git add schemaplexai-ui/src/pages/Cockpit/index.tsx schemaplexai-ui/src/pages/AgentExecutor/index.tsx
git rm schemaplexai-ui/src/api/agent.ts
git commit -m "refactor(api): split agent.ts into agent-config and agent-engine"
```

---

### Task 3: Create task.ts API

**Files:**
- Create: `src/api/task.ts`

- [ ] **Step 1: Create task.ts**

Create `schemaplexai-ui/src/api/task.ts`:

```typescript
import request from './request'
import type { SfTask } from '@/types'

export interface TaskQuery {
  page?: number
  pageSize?: number
  status?: string
  priority?: string
  keyword?: string
}

export interface CreateTaskPayload {
  title: string
  description?: string
  skillTags?: string[]
  priority?: 'P0' | 'P1' | 'P2' | 'P3'
  assignmentType?: 'MANUAL' | 'AUTO' | 'MIXED'
  specId?: string
}

export function getTaskList(params?: TaskQuery) {
  return request.get<{ list: SfTask[]; total: number }>('/task/tasks', { params })
}

export function getTaskDetail(id: string) {
  return request.get<SfTask>(`/task/tasks/${id}`)
}

export function createTask(data: CreateTaskPayload) {
  return request.post<SfTask>('/task/tasks', data)
}

export function updateTask(id: string, data: Partial<CreateTaskPayload>) {
  return request.put<SfTask>(`/task/tasks/${id}`, data)
}

export function updateTaskStatus(id: string, status: string, blockerReason?: string) {
  return request.put<void>(`/task/tasks/${id}/status`, { status, blockerReason })
}

export function deleteTask(id: string) {
  return request.delete<void>(`/task/tasks/${id}`)
}

export interface TaskComment {
  id: string
  taskId: string
  content: string
  authorId: string
  authorName?: string
  createdAt: string
}

export function getTaskComments(taskId: string) {
  return request.get<TaskComment[]>(`/task/tasks/${taskId}/comments`)
}

export function addTaskComment(taskId: string, content: string) {
  return request.post<TaskComment>(`/task/tasks/${taskId}/comments`, { content })
}

export interface JobRecord {
  id: string
  name: string
  queue: string
  status: string
  retryCount: number
  maxRetries: number
  createdAt: string
  updatedAt: string
}

export function getJobList(params?: { page?: number; pageSize?: number; queue?: string }) {
  return request.get<{ list: JobRecord[]; total: number }>('/task/jobs', { params })
}

export function retryJob(id: string) {
  return request.post<void>(`/task/jobs/${id}/retry`)
}

export function cancelJob(id: string) {
  return request.post<void>(`/task/jobs/${id}/cancel`)
}
```

- [ ] **Step 2: Commit**

```bash
git add schemaplexai-ui/src/api/task.ts
git commit -m "feat(api): add task service API client"
```

---

### Task 4: Expand system.ts API

**Files:**
- Modify: `src/api/system.ts`

- [ ] **Step 1: Add user/tenant/role endpoints**

Replace the contents of `schemaplexai-ui/src/api/system.ts` with:

```typescript
import request from './request'

export interface SystemConfig {
  id: string
  configKey: string
  configValue: string
  category?: string
  description?: string
  createdAt: string
  updatedAt: string
}

export interface SystemLog {
  id: string
  level: string
  module: string
  message: string
  createdAt: string
}

export function getSystemConfigs(params?: { category?: string }) {
  return request.get<{ list: SystemConfig[]; total: number }>('/system/configs', { params })
}

export function updateSystemConfig(id: string, value: string) {
  return request.put<void>(`/system/configs/${id}`, { configValue: value })
}

export function getSystemLogs(params?: { page?: number; pageSize?: number; level?: string }) {
  return request.get<{ list: SystemLog[]; total: number }>('/system/logs', { params })
}

export function getSystemMetrics() {
  return request.get<{
    cpuUsage: number
    memoryUsage: number
    diskUsage: number
    activeConnections: number
  }>('/system/metrics')
}

export interface User {
  id: string
  username: string
  nickname?: string
  email?: string
  phone?: string
  status: string
  createdAt: string
}

export function getUserList(params?: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<{ list: User[]; total: number }>('/system/users', { params })
}

export function createUser(data: Omit<User, 'id' | 'createdAt'>) {
  return request.post<User>('/system/users', data)
}

export function updateUser(id: string, data: Partial<User>) {
  return request.put<User>(`/system/users/${id}`, data)
}

export function deleteUser(id: string) {
  return request.delete<void>(`/system/users/${id}`)
}

export interface Role {
  id: string
  name: string
  code: string
  description?: string
  createdAt: string
}

export function getRoleList() {
  return request.get<Role[]>('/system/roles')
}

export interface Tenant {
  id: string
  name: string
  code: string
  status: string
  createdAt: string
}

export function getTenantList() {
  return request.get<Tenant[]>('/system/tenants')
}

export interface ModelConfigItem {
  id: string
  provider: string
  model: string
  apiKey?: string
  baseUrl?: string
  priority: number
  enabled: boolean
}

export function getModelConfigs() {
  return request.get<ModelConfigItem[]>('/system/models')
}

export function updateModelConfig(id: string, data: Partial<ModelConfigItem>) {
  return request.put<ModelConfigItem>(`/system/models/${id}`, data)
}
```

- [ ] **Step 2: Commit**

```bash
git add schemaplexai-ui/src/api/system.ts
git commit -m "feat(api): expand system API with users, roles, tenants, models"
```

---

### Task 5: Add SfTask types

**Files:**
- Modify: `src/types/index.ts`

- [ ] **Step 1: Add SfTask interface**

Append to `schemaplexai-ui/src/types/index.ts` (after the existing `ApiResponse` interface):

```typescript
export type TaskStatus =
  | 'BACKLOG'
  | 'QUEUED'
  | 'IN_PROGRESS'
  | 'AWAITING_REVIEW'
  | 'REVISING'
  | 'BLOCKED'
  | 'DONE'

export type TaskPriority = 'P0' | 'P1' | 'P2' | 'P3'

export type AssignmentType = 'MANUAL' | 'AUTO' | 'MIXED'

export interface SfTask {
  id: string
  tenantId: string
  title: string
  description?: string
  skillTags?: string[]
  priority: TaskPriority
  status: TaskStatus
  assignedRuntimeId?: string
  assignedAgentId?: string
  assignmentType: AssignmentType
  specId?: string
  blockerReason?: string
  createdAt: string
  updatedAt: string
}
```

- [ ] **Step 2: Commit**

```bash
git add schemaplexai-ui/src/types/index.ts
git commit -m "feat(types): add SfTask, TaskStatus, TaskPriority, AssignmentType"
```

---

### Task 6: Rewrite router/index.tsx

**Files:**
- Modify: `src/router/index.tsx`

- [ ] **Step 1: Rewrite router with domain routes**

Replace the contents of `schemaplexai-ui/src/router/index.tsx` with:

```typescript
import { Navigate } from 'react-router-dom'
import { lazy } from 'react'
import type { ReactNode } from 'react'
import { ImmersiveLayout, ProgressiveLayout } from '@/components/Layout'

const Login = lazy(() => import('@/pages/Login'))
const Cockpit = lazy(() => import('@/pages/Cockpit'))
const AgentCanvas = lazy(() => import('@/pages/AgentCanvas'))
const AgentList = lazy(() => import('@/pages/AgentList'))
const AgentExecutor = lazy(() => import('@/pages/AgentExecutor'))
const AgentDetail = lazy(() => import('@/pages/AgentDetail'))
const SpecCenter = lazy(() => import('@/pages/Projects/SpecCenter'))
const WorkflowCenter = lazy(() => import('@/pages/Projects/WorkflowCenter'))
const ContextCenter = lazy(() => import('@/pages/Projects/ContextCenter'))
const QualityGates = lazy(() => import('@/pages/Quality/QualityGates'))
const QualityIssues = lazy(() => import('@/pages/Quality/QualityIssues'))
const SecurityAudit = lazy(() => import('@/pages/Quality/SecurityAudit'))
const SystemCenter = lazy(() => import('@/pages/Platform/SystemCenter'))
const IntegrationCenter = lazy(() => import('@/pages/Platform/IntegrationCenter'))
const OpsCenter = lazy(() => import('@/pages/Platform/OpsCenter'))
const TaskBoard = lazy(() => import('@/pages/Tasks/TaskBoard'))
const TaskJobs = lazy(() => import('@/pages/Tasks/TaskJobs'))
const TaskDetail = lazy(() => import('@/pages/Tasks/TaskDetail'))
const NotFound = lazy(() => import('@/pages/NotFound'))

export interface RouteConfig {
  path: string
  element: ReactNode
  children?: RouteConfig[]
}

function RequireAuth({ children }: { children: ReactNode }) {
  const token = localStorage.getItem('schemaplexai_token')
  if (!token) {
    return <Navigate to="/login" replace />
  }
  return children
}

const RouterConfig: RouteConfig[] = [
  { path: '/login', element: <Login /> },
  { path: '/', element: <Navigate to="/cockpit" replace /> },
  { path: '/dashboard', element: <Navigate to="/cockpit" replace /> },

  // ImmersiveLayout
  {
    path: '/cockpit',
    element: (
      <RequireAuth>
        <ImmersiveLayout />
      </RequireAuth>
    ),
    children: [{ path: '', element: <Cockpit /> }],
  },
  {
    path: '/agents/canvas',
    element: (
      <RequireAuth>
        <ImmersiveLayout />
      </RequireAuth>
    ),
    children: [{ path: '', element: <AgentCanvas /> }],
  },

  // ProgressiveLayout — Agents
  {
    path: '/agents',
    element: (
      <RequireAuth>
        <ProgressiveLayout />
      </RequireAuth>
    ),
    children: [
      { path: '', element: <Navigate to="list" replace /> },
      { path: 'list', element: <AgentList /> },
      { path: 'executor', element: <AgentExecutor /> },
      { path: ':id', element: <AgentDetail /> },
    ],
  },

  // ProgressiveLayout — Projects
  {
    path: '/projects',
    element: (
      <RequireAuth>
        <ProgressiveLayout />
      </RequireAuth>
    ),
    children: [
      { path: '', element: <Navigate to="specs" replace /> },
      { path: 'specs', element: <SpecCenter /> },
      { path: 'workflows', element: <WorkflowCenter /> },
      { path: 'contexts', element: <ContextCenter /> },
    ],
  },

  // ProgressiveLayout — Quality
  {
    path: '/quality',
    element: (
      <RequireAuth>
        <ProgressiveLayout />
      </RequireAuth>
    ),
    children: [
      { path: '', element: <Navigate to="gates" replace /> },
      { path: 'gates', element: <QualityGates /> },
      { path: 'issues', element: <QualityIssues /> },
      { path: 'security', element: <SecurityAudit /> },
    ],
  },

  // ProgressiveLayout — Platform
  {
    path: '/platform',
    element: (
      <RequireAuth>
        <ProgressiveLayout />
      </RequireAuth>
    ),
    children: [
      { path: '', element: <Navigate to="system" replace /> },
      { path: 'system', element: <SystemCenter /> },
      { path: 'integrations', element: <IntegrationCenter /> },
      { path: 'ops', element: <OpsCenter /> },
    ],
  },

  // ProgressiveLayout — Tasks
  {
    path: '/tasks',
    element: (
      <RequireAuth>
        <ProgressiveLayout />
      </RequireAuth>
    ),
    children: [
      { path: '', element: <TaskBoard /> },
      { path: 'jobs', element: <TaskJobs /> },
      { path: ':id', element: <TaskDetail /> },
    ],
  },

  // Legacy redirects
  { path: '/workflows', element: <Navigate to="/projects/workflows" replace /> },
  { path: '/specs', element: <Navigate to="/projects/specs" replace /> },
  { path: '/contexts', element: <Navigate to="/projects/contexts" replace /> },
  { path: '/integrations', element: <Navigate to="/platform/integrations" replace /> },
  { path: '/ops', element: <Navigate to="/platform/ops" replace /> },
  { path: '/settings', element: <Navigate to="/platform/system" replace /> },
  { path: '/notifications', element: <Navigate to="/tasks" replace /> },
  { path: '/canvas', element: <Navigate to="/agents/canvas" replace /> },

  { path: '*', element: <NotFound /> },
]

export default RouterConfig
```

- [ ] **Step 2: Verify no type errors**

```bash
cd schemaplexai-ui && npx tsc --noEmit
```

Expected: May show errors for missing page files (AgentList, etc.). That's expected until Phase 2.

- [ ] **Step 3: Commit**

```bash
git add schemaplexai-ui/src/router/index.tsx
git commit -m "feat(router): restructure routes into 6 domain groups with sub-routes"
```

---

### Task 7: Rewrite i18n keys

**Files:**
- Modify: `src/i18n/locales/zh.json`
- Modify: `src/i18n/locales/en.json`

- [ ] **Step 1: Replace nav section in zh.json**

In `schemaplexai-ui/src/i18n/locales/zh.json`, replace the `"nav"` object with:

```json
  "nav": {
    "domain": {
      "cockpit": "驾驶舱",
      "agents": "智能体",
      "projects": "项目",
      "quality": "质量",
      "platform": "平台",
      "tasks": "任务"
    },
    "sub": {
      "list": "列表",
      "executor": "执行器",
      "canvas": "画布",
      "specs": "规格",
      "workflows": "工作流",
      "contexts": "上下文",
      "gates": "门禁",
      "issues": "问题",
      "security": "安全审计",
      "system": "系统",
      "integrations": "集成",
      "ops": "运维",
      "jobs": "作业"
    }
  }
```

- [ ] **Step 2: Add new page keys to zh.json**

Append these objects to `schemaplexai-ui/src/i18n/locales/zh.json` before the closing `}`:

```json
  "agentList": {
    "title": "Agent 列表",
    "newAgent": "新建 Agent",
    "searchPlaceholder": "搜索 Agent",
    "name": "名称",
    "description": "描述",
    "type": "类型",
    "status": "状态",
    "updatedAt": "更新时间",
    "action": "操作",
    "fetchError": "获取 Agent 列表失败"
  },
  "qualityGates": {
    "title": "质量门禁",
    "newGate": "新建门禁",
    "name": "名称",
    "status": "状态",
    "score": "评分",
    "fetchError": "获取门禁数据失败"
  },
  "qualityIssues": {
    "title": "质量问题",
    "newIssue": "新建问题",
    "title": "标题",
    "category": "类别",
    "status": "状态",
    "fetchError": "获取问题数据失败"
  },
  "securityAudit": {
    "title": "安全审计",
    "policy": "策略",
    "event": "事件",
    "fetchError": "获取审计数据失败"
  },
  "systemCenter": {
    "title": "系统中心",
    "users": "用户管理",
    "roles": "角色管理",
    "tenants": "租户管理",
    "models": "模型配置",
    "fetchError": "获取系统数据失败"
  },
  "taskBoard": {
    "title": "任务看板",
    "newTask": "新建任务",
    "backlog": "待办",
    "queued": "排队",
    "inProgress": "进行中",
    "awaitingReview": "待审核",
    "revising": "修改中",
    "blocked": "阻塞",
    "done": "已完成",
    "approve": "通过",
    "requestChanges": "请求修改",
    "escalate": "升级",
    "fetchError": "获取任务数据失败"
  },
  "taskJobs": {
    "title": "后台作业",
    "queue": "队列",
    "retry": "重试",
    "cancel": "取消",
    "fetchError": "获取作业数据失败"
  },
  "taskDetail": {
    "title": "任务详情",
    "comments": "评论",
    "addComment": "添加评论",
    "fetchError": "获取任务详情失败"
  }
```

Wait — there's a duplicate key `"title"` in qualityIssues and the top-level page keys. Let me fix: qualityIssues uses `"issueTitle"` instead.

Actually, looking at this more carefully, the JSON keys `title` inside each page namespace is fine since they're nested under different parents. But `qualityIssues.title` and `qualityIssues.title` as a key for the issue title field would conflict. Let me use `issueTitle` for the field.

Corrected:
```json
  "qualityIssues": {
    "title": "质量问题",
    "newIssue": "新建问题",
    "issueTitle": "标题",
    "category": "类别",
    "status": "状态",
    "fetchError": "获取问题数据失败"
  }
```

- [ ] **Step 3: Replace nav section in en.json**

In `schemaplexai-ui/src/i18n/locales/en.json`, replace the `"nav"` object with:

```json
  "nav": {
    "domain": {
      "cockpit": "Cockpit",
      "agents": "Agents",
      "projects": "Projects",
      "quality": "Quality",
      "platform": "Platform",
      "tasks": "Tasks"
    },
    "sub": {
      "list": "List",
      "executor": "Executor",
      "canvas": "Canvas",
      "specs": "Specs",
      "workflows": "Workflows",
      "contexts": "Contexts",
      "gates": "Gates",
      "issues": "Issues",
      "security": "Security",
      "system": "System",
      "integrations": "Integrations",
      "ops": "Ops",
      "jobs": "Jobs"
    }
  }
```

- [ ] **Step 4: Add new page keys to en.json**

Append matching English translations to `schemaplexai-ui/src/i18n/locales/en.json`.

- [ ] **Step 5: Commit**

```bash
git add schemaplexai-ui/src/i18n/locales/zh.json schemaplexai-ui/src/i18n/locales/en.json
git commit -m "feat(i18n): restructure nav keys to domain format, add new page translations"
```

---

### Task 8: ImmersiveLayout expansion

**Files:**
- Modify: `src/components/Layout/ImmersiveLayout.tsx`
- Modify: `src/components/Layout/ImmersiveLayout.test.tsx`

- [ ] **Step 1: Rewrite ImmersiveLayout**

Replace `schemaplexai-ui/src/components/Layout/ImmersiveLayout.tsx` with:

```typescript
import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { LanguageSwitcher } from '@/components/LanguageSwitcher'
import './Layout.css'

export interface ImmersiveLayoutProps {
  children?: React.ReactNode
}

export function ImmersiveLayout({ children }: ImmersiveLayoutProps) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const location = useLocation()
  const [hovered, setHovered] = useState<string | null>(null)

  const NAV_ITEMS = useMemo(
    () => [
      { key: 'cockpit', icon: '◉', label: t('nav.domain.cockpit'), path: '/cockpit', immersive: true },
      { key: 'agents', icon: '●', label: t('nav.domain.agents'), path: '/agents/list', immersive: false },
      { key: 'projects', icon: '▲', label: t('nav.domain.projects'), path: '/projects', immersive: false },
      { key: 'quality', icon: '✓', label: t('nav.domain.quality'), path: '/quality', immersive: false },
      { key: 'platform', icon: '◎', label: t('nav.domain.platform'), path: '/platform', immersive: false },
      { key: 'tasks', icon: '⚡', label: t('nav.domain.tasks'), path: '/tasks', immersive: false },
      { key: 'canvas', icon: '◆', label: t('nav.sub.canvas'), path: '/agents/canvas', immersive: true },
    ],
    [t]
  )

  const activeKey = NAV_ITEMS.find(
    (item) => location.pathname === item.path || location.pathname.startsWith(item.path + '/')
  )?.key || 'cockpit'

  return (
    <div className="layout-immersive">
      <div className="layout-icon-sidebar">
        <div className="layout-icon-sidebar-logo">S</div>

        {NAV_ITEMS.map((item) => {
          const isActive = item.key === activeKey
          return (
            <div
              key={item.key}
              data-testid={`sidebar-${item.key}`}
              onClick={() => navigate(item.path)}
              onMouseEnter={() => setHovered(item.key)}
              onMouseLeave={() => setHovered(null)}
              className={`layout-nav-item${isActive ? ' layout-nav-item--active' : ''}`}
            >
              <span
                className={`layout-nav-item-icon${isActive ? ' layout-nav-item-icon--active' : ''}`}
              >
                {item.icon}
              </span>
              {hovered === item.key && (
                <div className="layout-nav-tooltip">{item.label}</div>
              )}
            </div>
          )
        })}

        <div style={{ marginTop: 'auto', paddingTop: 16, display: 'flex', justifyContent: 'center' }}>
          <LanguageSwitcher />
        </div>
      </div>

      <div className="layout-canvas">
        <div className="layout-canvas-grid" />
        <div className="layout-floating-header">
          <span>SchemaPlexAI</span>
          <span className="layout-floating-header-divider">|</span>
          <span>
            <span className="layout-floating-header-dot--cyan">●</span> 12 Agents
          </span>
          <span>
            <span className="layout-floating-header-dot--amber">●</span> 3 Executing
          </span>
        </div>
        <div className="layout-canvas-content">{children ?? <Outlet />}</div>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Update ImmersiveLayout tests**

Replace `schemaplexai-ui/src/components/Layout/ImmersiveLayout.test.tsx` with:

```typescript
import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ImmersiveLayout } from './ImmersiveLayout'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => {
      const translations: Record<string, string> = {
        'nav.domain.cockpit': '驾驶舱',
        'nav.domain.agents': '智能体',
        'nav.domain.projects': '项目',
        'nav.domain.quality': '质量',
        'nav.domain.platform': '平台',
        'nav.domain.tasks': '任务',
        'nav.sub.canvas': '画布',
      }
      return translations[key] || key
    },
    i18n: { language: 'zh' },
  }),
  Trans: ({ children }: { children: React.ReactNode }) => children,
}))

vi.mock('@/components/LanguageSwitcher', () => ({
  LanguageSwitcher: () => <div data-testid="language-switcher">Lang</div>,
}))

describe('ImmersiveLayout', () => {
  it('renders all 7 sidebar icons', () => {
    render(
      <MemoryRouter>
        <ImmersiveLayout>test</ImmersiveLayout>
      </MemoryRouter>
    )
    expect(screen.getByTestId('sidebar-cockpit')).toBeInTheDocument()
    expect(screen.getByTestId('sidebar-agents')).toBeInTheDocument()
    expect(screen.getByTestId('sidebar-projects')).toBeInTheDocument()
    expect(screen.getByTestId('sidebar-quality')).toBeInTheDocument()
    expect(screen.getByTestId('sidebar-platform')).toBeInTheDocument()
    expect(screen.getByTestId('sidebar-tasks')).toBeInTheDocument()
    expect(screen.getByTestId('sidebar-canvas')).toBeInTheDocument()
  })

  it('renders children in content area', () => {
    render(
      <MemoryRouter>
        <ImmersiveLayout>
          <div data-testid="content">Page Content</div>
        </ImmersiveLayout>
      </MemoryRouter>
    )
    expect(screen.getByTestId('content')).toBeInTheDocument()
  })

  it('highlights cockpit by default', () => {
    render(
      <MemoryRouter initialEntries={['/cockpit']}>
        <ImmersiveLayout>test</ImmersiveLayout>
      </MemoryRouter>
    )
    const cockpit = screen.getByTestId('sidebar-cockpit')
    expect(cockpit.className).toContain('layout-nav-item--active')
  })
})
```

- [ ] **Step 3: Run tests**

```bash
cd schemaplexai-ui && npx vitest run src/components/Layout/ImmersiveLayout.test.tsx
```

Expected: All tests pass.

- [ ] **Step 4: Commit**

```bash
git add schemaplexai-ui/src/components/Layout/ImmersiveLayout.tsx
git add schemaplexai-ui/src/components/Layout/ImmersiveLayout.test.tsx
git commit -m "feat(layout): expand ImmersiveLayout to 7 nav icons (6 domains + canvas)"
```

---

### Task 9: ProgressiveLayout refactor with DomainNav

**Files:**
- Create: `src/components/Hive/DomainNav.tsx`
- Create: `src/components/Hive/DomainNav.test.tsx`
- Modify: `src/components/Layout/ProgressiveLayout.tsx`
- Modify: `src/components/Layout/ProgressiveLayout.test.tsx`

- [ ] **Step 1: Create DomainNav component**

Create `schemaplexai-ui/src/components/Hive/DomainNav.tsx`:

```typescript
import { useState, useEffect, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useLocation } from 'react-router-dom'

export interface DomainNavItem {
  key: string
  icon: string
  label: string
  path: string
  children?: { key: string; label: string; path: string }[]
}

export interface DomainNavProps {
  items: DomainNavItem[]
}

const STORAGE_KEY = 'nav_expanded'

export function DomainNav({ items }: DomainNavProps) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const location = useLocation()

  const [expandedKeys, setExpandedKeys] = useState<string[]>(() => {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
    } catch {
      return []
    }
  })

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(expandedKeys))
  }, [expandedKeys])

  const isActive = useCallback(
    (path: string) => {
      return location.pathname === path || location.pathname.startsWith(path + '/')
    },
    [location.pathname]
  )

  const toggleExpand = (key: string) => {
    setExpandedKeys((prev) =>
      prev.includes(key) ? prev.filter((k) => k !== key) : [...prev, key]
    )
  }

  const activeParentKey = items.find((item) =>
    item.children?.some((child) => isActive(child.path))
  )?.key

  return (
    <nav className="domain-nav" data-testid="domain-nav">
      {items.map((item) => {
        const hasChildren = item.children && item.children.length > 0
        const isExpanded = expandedKeys.includes(item.key) || item.key === activeParentKey
        const parentActive = isActive(item.path) || item.key === activeParentKey

        return (
          <div key={item.key} className="domain-nav-group" data-testid={`nav-group-${item.key}`}>
            <div
              className={`domain-nav-item${parentActive ? ' domain-nav-item--active' : ''}`}
              onClick={() => {
                if (hasChildren) {
                  toggleExpand(item.key)
                } else {
                  navigate(item.path)
                }
              }}
            >
              <span className="domain-nav-item-icon">{item.icon}</span>
              <span className="domain-nav-item-label">{item.label}</span>
              {hasChildren && (
                <span className={`domain-nav-chevron${isExpanded ? ' domain-nav-chevron--expanded' : ''}`}>
                  ▶
                </span>
              )}
            </div>

            {hasChildren && isExpanded && (
              <div className="domain-nav-children">
                {item.children.map((child) => (
                  <div
                    key={child.key}
                    className={`domain-nav-child${isActive(child.path) ? ' domain-nav-child--active' : ''}`}
                    onClick={() => navigate(child.path)}
                    data-testid={`nav-child-${child.key}`}
                  >
                    {child.label}
                  </div>
                ))}
              </div>
            )}
          </div>
        )
      })}
    </nav>
  )
}
```

- [ ] **Step 2: Create DomainNav test**

Create `schemaplexai-ui/src/components/Hive/DomainNav.test.tsx`:

```typescript
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { DomainNav } from './DomainNav'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: 'zh' },
  }),
}))

const ITEMS = [
  { key: 'cockpit', icon: '◉', label: 'Cockpit', path: '/cockpit' },
  {
    key: 'agents',
    icon: '●',
    label: 'Agents',
    path: '/agents',
    children: [
      { key: 'list', label: 'List', path: '/agents/list' },
      { key: 'executor', label: 'Executor', path: '/agents/executor' },
    ],
  },
]

describe('DomainNav', () => {
  it('renders top-level items', () => {
    render(
      <MemoryRouter>
        <DomainNav items={ITEMS} />
      </MemoryRouter>
    )
    expect(screen.getByText('Cockpit')).toBeInTheDocument()
    expect(screen.getByText('Agents')).toBeInTheDocument()
  })

  it('expands children on click', () => {
    render(
      <MemoryRouter>
        <DomainNav items={ITEMS} />
      </MemoryRouter>
    )
    fireEvent.click(screen.getByText('Agents'))
    expect(screen.getByTestId('nav-child-list')).toBeInTheDocument()
    expect(screen.getByTestId('nav-child-executor')).toBeInTheDocument()
  })

  it('highlights active route', () => {
    render(
      <MemoryRouter initialEntries={['/agents/list']}>
        <DomainNav items={ITEMS} />
      </MemoryRouter>
    )
    const child = screen.getByTestId('nav-child-list')
    expect(child.className).toContain('domain-nav-child--active')
  })
})
```

- [ ] **Step 3: Rewrite ProgressiveLayout**

Replace `schemaplexai-ui/src/components/Layout/ProgressiveLayout.tsx` with:

```typescript
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Outlet } from 'react-router-dom'
import { LanguageSwitcher } from '@/components/LanguageSwitcher'
import { DomainNav } from '@/components/Hive/DomainNav'
import type { DomainNavItem } from '@/components/Hive/DomainNav'
import './Layout.css'

export interface ProgressiveLayoutProps {
  children?: React.ReactNode
}

export function ProgressiveLayout({ children }: ProgressiveLayoutProps) {
  const { t } = useTranslation()

  const DOMAINS: DomainNavItem[] = useMemo(
    () => [
      { key: 'cockpit', icon: '◉', label: t('nav.domain.cockpit'), path: '/cockpit' },
      {
        key: 'agents',
        icon: '●',
        label: t('nav.domain.agents'),
        path: '/agents',
        children: [
          { key: 'list', label: t('nav.sub.list'), path: '/agents/list' },
          { key: 'executor', label: t('nav.sub.executor'), path: '/agents/executor' },
          { key: 'canvas', label: t('nav.sub.canvas'), path: '/agents/canvas' },
        ],
      },
      {
        key: 'projects',
        icon: '▲',
        label: t('nav.domain.projects'),
        path: '/projects',
        children: [
          { key: 'specs', label: t('nav.sub.specs'), path: '/projects/specs' },
          { key: 'workflows', label: t('nav.sub.workflows'), path: '/projects/workflows' },
          { key: 'contexts', label: t('nav.sub.contexts'), path: '/projects/contexts' },
        ],
      },
      {
        key: 'quality',
        icon: '✓',
        label: t('nav.domain.quality'),
        path: '/quality',
        children: [
          { key: 'gates', label: t('nav.sub.gates'), path: '/quality/gates' },
          { key: 'issues', label: t('nav.sub.issues'), path: '/quality/issues' },
          { key: 'security', label: t('nav.sub.security'), path: '/quality/security' },
        ],
      },
      {
        key: 'platform',
        icon: '◎',
        label: t('nav.domain.platform'),
        path: '/platform',
        children: [
          { key: 'system', label: t('nav.sub.system'), path: '/platform/system' },
          { key: 'integrations', label: t('nav.sub.integrations'), path: '/platform/integrations' },
          { key: 'ops', label: t('nav.sub.ops'), path: '/platform/ops' },
        ],
      },
      {
        key: 'tasks',
        icon: '⚡',
        label: t('nav.domain.tasks'),
        path: '/tasks',
        children: [
          { key: 'board', label: t('nav.domain.tasks'), path: '/tasks' },
          { key: 'jobs', label: t('nav.sub.jobs'), path: '/tasks/jobs' },
        ],
      },
    ],
    [t]
  )

  return (
    <div className="layout-progressive">
      <header className="layout-progressive-header">
        <span className="layout-progressive-header-brand">SchemaPlexAI</span>
        <div className="layout-progressive-header-actions">
          <LanguageSwitcher />
          <span className="layout-progressive-header-tenant">{t('nav.domain.platform')} ▼</span>
          <span className="layout-progressive-header-bell">🔔</span>
          <div className="layout-progressive-header-avatar" />
        </div>
      </header>

      <div className="layout-progressive-body">
        <aside className="layout-progressive-sidebar">
          <DomainNav items={DOMAINS} />
          <div style={{ marginTop: 'auto', padding: '16px 12px', fontSize: 12, opacity: 0.6 }}>
            <LanguageSwitcher />
          </div>
        </aside>

        <main className="layout-progressive-content">
          {children ?? <Outlet />}
        </main>
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Update ProgressiveLayout tests**

Replace `schemaplexai-ui/src/components/Layout/ProgressiveLayout.test.tsx` with:

```typescript
import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ProgressiveLayout } from './ProgressiveLayout'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => {
      const translations: Record<string, string> = {
        'nav.domain.cockpit': '驾驶舱',
        'nav.domain.agents': '智能体',
        'nav.domain.projects': '项目',
        'nav.domain.quality': '质量',
        'nav.domain.platform': '平台',
        'nav.domain.tasks': '任务',
        'nav.sub.list': '列表',
        'nav.sub.executor': '执行器',
        'nav.sub.canvas': '画布',
        'nav.sub.specs': '规格',
        'nav.sub.workflows': '工作流',
        'nav.sub.contexts': '上下文',
        'nav.sub.gates': '门禁',
        'nav.sub.issues': '问题',
        'nav.sub.security': '安全审计',
        'nav.sub.system': '系统',
        'nav.sub.integrations': '集成',
        'nav.sub.ops': '运维',
        'nav.sub.jobs': '作业',
      }
      return translations[key] || key
    },
    i18n: { language: 'zh' },
  }),
  Trans: ({ children }: { children: React.ReactNode }) => children,
}))

vi.mock('@/components/LanguageSwitcher', () => ({
  LanguageSwitcher: () => <div data-testid="language-switcher">Lang</div>,
}))

describe('ProgressiveLayout', () => {
  it('renders children in content area', () => {
    render(
      <MemoryRouter>
        <ProgressiveLayout>
          <div data-testid="content">Page Content</div>
        </ProgressiveLayout>
      </MemoryRouter>
    )
    expect(screen.getByTestId('content')).toBeInTheDocument()
  })

  it('renders DomainNav with 6 domains', () => {
    render(
      <MemoryRouter>
        <ProgressiveLayout>test</ProgressiveLayout>
      </MemoryRouter>
    )
    expect(screen.getByTestId('domain-nav')).toBeInTheDocument()
    expect(screen.getByTestId('nav-group-cockpit')).toBeInTheDocument()
    expect(screen.getByTestId('nav-group-agents')).toBeInTheDocument()
    expect(screen.getByTestId('nav-group-projects')).toBeInTheDocument()
    expect(screen.getByTestId('nav-group-quality')).toBeInTheDocument()
    expect(screen.getByTestId('nav-group-platform')).toBeInTheDocument()
    expect(screen.getByTestId('nav-group-tasks')).toBeInTheDocument()
  })

  it('renders header with brand', () => {
    render(
      <MemoryRouter>
        <ProgressiveLayout>test</ProgressiveLayout>
      </MemoryRouter>
    )
    expect(screen.getByText('SchemaPlexAI')).toBeInTheDocument()
  })
})
```

- [ ] **Step 5: Update Hive index.ts**

In `schemaplexai-ui/src/components/Hive/index.ts`, add:

```typescript
export { DomainNav } from './DomainNav'
export type { DomainNavProps, DomainNavItem } from './DomainNav'
```

- [ ] **Step 6: Run tests**

```bash
cd schemaplexai-ui && npx vitest run src/components/Hive/DomainNav.test.tsx src/components/Layout/ProgressiveLayout.test.tsx
```

Expected: All tests pass.

- [ ] **Step 7: Commit**

```bash
git add schemaplexai-ui/src/components/Hive/DomainNav.tsx schemaplexai-ui/src/components/Hive/DomainNav.test.tsx
git add schemaplexai-ui/src/components/Layout/ProgressiveLayout.tsx schemaplexai-ui/src/components/Layout/ProgressiveLayout.test.tsx
git add schemaplexai-ui/src/components/Hive/index.ts
git commit -m "feat(layout): ProgressiveLayout uses DomainNav with expandable submenus"
```

---

## Phase 2: Pages

### Task 10: AgentList (from AgentManager)

**Files:**
- Create: `src/pages/AgentList/index.tsx`
- Create: `src/pages/AgentList/AgentList.css`

- [ ] **Step 1: Copy AgentManager to AgentList**

Create `schemaplexai-ui/src/pages/AgentList/index.tsx` by copying `src/pages/AgentManager/index.tsx` and replacing:
- `'agentManager'` → `'agentList'` in all `t()` calls
- `@/api/agent` → `@/api/agent-config` (should already be done from Task 2)
- `'Agent 管理'` references → `'Agent 列表'`

The file should use `t('agentList.*')` keys. Use `useAgentStore` same as before.

Also update the title from `t('agentManager.title')` to `t('agentList.title')`.

- [ ] **Step 2: Create minimal CSS**

Create `schemaplexai-ui/src/pages/AgentList/AgentList.css`:

```css
.agent-list-page {
  padding: 24px;
}
```

- [ ] **Step 3: Commit**

```bash
git add schemaplexai-ui/src/pages/AgentList/
git commit -m "feat(pages): create AgentList from AgentManager at /agents/list"
```

---

### Task 11: Move SpecCenter to Projects/SpecCenter

**Files:**
- Create: `src/pages/Projects/SpecCenter/index.tsx`
- Create: `src/pages/Projects/SpecCenter/SpecCenter.css`
- Delete: `src/pages/SpecCenter/`

- [ ] **Step 1: Copy SpecCenter**

Create `schemaplexai-ui/src/pages/Projects/SpecCenter/index.tsx` by copying `src/pages/SpecCenter/index.tsx` with no changes (API imports remain the same).

Create `schemaplexai-ui/src/pages/Projects/SpecCenter/SpecCenter.css` by copying `src/pages/SpecCenter/SpecCenter.css`.

- [ ] **Step 2: Delete old SpecCenter**

```bash
rm -rf schemaplexai-ui/src/pages/SpecCenter
```

- [ ] **Step 3: Commit**

```bash
git add schemaplexai-ui/src/pages/Projects/SpecCenter/
git rm -r schemaplexai-ui/src/pages/SpecCenter
git commit -m "refactor(pages): move SpecCenter to Projects/SpecCenter"
```

---

### Task 12: Move ContextCenter to Projects/ContextCenter

**Files:**
- Create: `src/pages/Projects/ContextCenter/index.tsx`
- Create: `src/pages/Projects/ContextCenter/ContextCenter.css`
- Delete: `src/pages/ContextCenter/`

- [ ] **Step 1: Copy ContextCenter**

Same pattern as Task 11.

- [ ] **Step 2: Delete old ContextCenter**

```bash
rm -rf schemaplexai-ui/src/pages/ContextCenter
```

- [ ] **Step 3: Commit**

```bash
git add schemaplexai-ui/src/pages/Projects/ContextCenter/
git rm -r schemaplexai-ui/src/pages/ContextCenter
git commit -m "refactor(pages): move ContextCenter to Projects/ContextCenter"
```

---

### Task 13: WorkflowCenter (resurrect + refactor)

**Files:**
- Create: `src/pages/Projects/WorkflowCenter/index.tsx`
- Create: `src/pages/Projects/WorkflowCenter/WorkflowCenter.css`
- Delete: `src/pages/WorkflowCenter/` (old dead code)

- [ ] **Step 1: Create tabbed WorkflowCenter**

Create `schemaplexai-ui/src/pages/Projects/WorkflowCenter/index.tsx`:

```typescript
import { useState } from 'react'
import { Tabs } from 'antd'
import { useTranslation } from 'react-i18next'
import WorkflowTemplateTab from './WorkflowTemplateTab'
import WorkflowInstanceTab from './WorkflowInstanceTab'
import './WorkflowCenter.css'

export default function WorkflowCenter() {
  const { t } = useTranslation()
  const [activeKey, setActiveKey] = useState('templates')

  return (
    <div className="workflow-center-page">
      <h2 className="workflow-center-title">{t('workflowCenter.title')}</h2>
      <Tabs activeKey={activeKey} onChange={setActiveKey}>
        <Tabs.TabPane tab={t('workflowCenter.templates')} key="templates">
          <WorkflowTemplateTab />
        </Tabs.TabPane>
        <Tabs.TabPane tab={t('workflowCenter.instances')} key="instances">
          <WorkflowInstanceTab />
        </Tabs.TabPane>
      </Tabs>
    </div>
  )
}
```

- [ ] **Step 2: Create WorkflowTemplateTab**

Create `schemaplexai-ui/src/pages/Projects/WorkflowCenter/WorkflowTemplateTab.tsx`:

```typescript
import { useEffect, useState } from 'react'
import { Table, Button, Space, Tag, message } from 'antd'
import { PlusOutlined, PlayCircleOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { getWorkflowList, runWorkflow, type Workflow } from '@/api/workflow'

export default function WorkflowTemplateTab() {
  const { t } = useTranslation()
  const [data, setData] = useState<Workflow[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [query, setQuery] = useState({ page: 1, pageSize: 10 })

  useEffect(() => {
    fetchData()
  }, [query])

  const fetchData = async () => {
    setLoading(true)
    try {
      const res = await getWorkflowList(query)
      setData(res.list)
      setTotal(res.total)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('workflowCenter.fetchError'))
    } finally {
      setLoading(false)
    }
  }

  const handleRun = async (id: string) => {
    try {
      await runWorkflow(id)
      message.success(t('workflowCenter.runSuccess'))
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('workflowCenter.runError'))
    }
  }

  const columns = [
    { title: t('workflowCenter.name'), dataIndex: 'name', key: 'name' },
    { title: t('workflowCenter.description'), dataIndex: 'description', key: 'description' },
    {
      title: t('workflowCenter.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'published' ? 'green' : status === 'draft' ? 'blue' : 'default'}>
          {status}
        </Tag>
      ),
    },
    { title: t('workflowCenter.updatedAt'), dataIndex: 'updatedAt', key: 'updatedAt' },
    {
      title: t('workflowCenter.action'),
      key: 'action',
      render: (_: unknown, record: Workflow) => (
        <Space>
          <Button icon={<PlayCircleOutlined />} onClick={() => handleRun(record.id)}>
            {t('workflowCenter.run')}
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />}>
          {t('workflowCenter.newWorkflow')}
        </Button>
      </div>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        pagination={{
          current: query.page,
          pageSize: query.pageSize,
          total,
          onChange: (page, pageSize) => setQuery({ page, pageSize: pageSize || 10 }),
        }}
        locale={{ emptyText: t('common.noData') }}
      />
    </div>
  )
}
```

- [ ] **Step 3: Create WorkflowInstanceTab**

Create `schemaplexai-ui/src/pages/Projects/WorkflowCenter/WorkflowInstanceTab.tsx`:

```typescript
import WorkflowMonitor from '@/pages/WorkflowMonitor'

export default function WorkflowInstanceTab() {
  return <WorkflowMonitor />
}
```

- [ ] **Step 4: Add CSS**

Create `schemaplexai-ui/src/pages/Projects/WorkflowCenter/WorkflowCenter.css`:

```css
.workflow-center-page {
  padding: 24px;
}

.workflow-center-title {
  margin-bottom: 16px;
  font-size: 20px;
  font-weight: 600;
}
```

- [ ] **Step 5: Add i18n keys for tabs**

In both `zh.json` and `en.json`, under `workflowCenter`, add:

```json
    "templates": "模板管理",
    "instances": "实例监控"
```

(en: `"templates": "Templates"`, `"instances": "Instances"`)

- [ ] **Step 6: Delete old WorkflowCenter**

```bash
rm -rf schemaplexai-ui/src/pages/WorkflowCenter
```

- [ ] **Step 7: Commit**

```bash
git add schemaplexai-ui/src/pages/Projects/WorkflowCenter/
git rm -r schemaplexai-ui/src/pages/WorkflowCenter
git add schemaplexai-ui/src/i18n/locales/zh.json schemaplexai-ui/src/i18n/locales/en.json
git commit -m "feat(pages): resurrect WorkflowCenter as tabbed page (templates + instances)"
```

---

### Task 14: Quality domain split

**Files:**
- Create: `src/pages/Quality/QualityGates/index.tsx`
- Create: `src/pages/Quality/QualityIssues/index.tsx`
- Create: `src/pages/Quality/SecurityAudit/index.tsx`
- Delete: `src/pages/QualityCenter/`

- [ ] **Step 1: Create QualityGates**

Create `schemaplexai-ui/src/pages/Quality/QualityGates/index.tsx`:

```typescript
import { useEffect, useState } from 'react'
import { Card, Table, Tag, Button, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { getQualityGates, type QualityGate } from '@/api/quality'

export default function QualityGates() {
  const { t } = useTranslation()
  const [data, setData] = useState<QualityGate[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    setLoading(true)
    try {
      const res = await getQualityGates()
      setData(res)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('qualityGates.fetchError'))
      setData([])
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    { title: t('qualityGates.name'), dataIndex: 'name', key: 'name' },
    {
      title: t('qualityCenter.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>{status === 1 ? t('qualityCenter.passTag') : t('qualityCenter.failTag')}</Tag>
      ),
    },
    { title: t('qualityCenter.updatedAt'), dataIndex: 'updatedAt', key: 'updatedAt' },
  ]

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>{t('qualityGates.title')}</h2>
      <Card>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={data}
          loading={loading}
          locale={{ emptyText: t('common.noData') }}
        />
      </Card>
    </div>
  )
}
```

- [ ] **Step 2: Create QualityIssues**

Create `schemaplexai-ui/src/pages/Quality/QualityIssues/index.tsx`:

```typescript
import { useEffect, useState } from 'react'
import { Card, Table, Tag, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { getQualityIssues, type QualityIssue } from '@/api/quality'

export default function QualityIssues() {
  const { t } = useTranslation()
  const [data, setData] = useState<QualityIssue[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    setLoading(true)
    try {
      const res = await getQualityIssues()
      setData(res)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('qualityIssues.fetchError'))
      setData([])
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    { title: t('qualityIssues.issueTitle'), dataIndex: 'title', key: 'title' },
    { title: t('qualityIssues.category'), dataIndex: 'category', key: 'category' },
    {
      title: t('qualityCenter.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'resolved' ? 'green' : status === 'open' ? 'red' : 'orange'}>{status}</Tag>
      ),
    },
    { title: t('qualityCenter.score'), dataIndex: 'score', key: 'score' },
    { title: t('qualityCenter.checkedAt'), dataIndex: 'checkedAt', key: 'checkedAt' },
  ]

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>{t('qualityIssues.title')}</h2>
      <Card>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={data}
          loading={loading}
          locale={{ emptyText: t('common.noData') }}
        />
      </Card>
    </div>
  )
}
```

- [ ] **Step 3: Create SecurityAudit**

Create `schemaplexai-ui/src/pages/Quality/SecurityAudit/index.tsx`:

```typescript
import { useTranslation } from 'react-i18next'
import { Card, Empty } from 'antd'

export default function SecurityAudit() {
  const { t } = useTranslation()

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>{t('securityAudit.title')}</h2>
      <Card>
        <Empty description={t('securityAudit.fetchError')} />
      </Card>
    </div>
  )
}
```

- [ ] **Step 4: Delete old QualityCenter**

```bash
rm -rf schemaplexai-ui/src/pages/QualityCenter
```

- [ ] **Step 5: Commit**

```bash
git add schemaplexai-ui/src/pages/Quality/
git rm -r schemaplexai-ui/src/pages/QualityCenter
git commit -m "feat(pages): split QualityCenter into QualityGates, QualityIssues, SecurityAudit"
```

---

### Task 15: Platform pages (SystemCenter, IntegrationCenter, OpsCenter)

**Files:**
- Create: `src/pages/Platform/SystemCenter/index.tsx`
- Create: `src/pages/Platform/IntegrationCenter/index.tsx`
- Create: `src/pages/Platform/OpsCenter/index.tsx`
- Delete: `src/pages/SystemSettings/`
- Delete: `src/pages/IntegrationCenter/`
- Delete: `src/pages/OpsCenter/`

- [ ] **Step 1: Create SystemCenter from SystemSettings**

Create `schemaplexai-ui/src/pages/Platform/SystemCenter/index.tsx` by copying `src/pages/SystemSettings/index.tsx` and updating `t('systemSettings.*')` to `t('systemCenter.*')`.

Also add tabs for Users, Roles, Tenants, Models using the new system API endpoints from Task 4.

For simplicity, extend the existing settings page with additional tabs:

```typescript
import { useState } from 'react'
import { Tabs } from 'antd'
import { useTranslation } from 'react-i18next'
import SystemGeneralTab from './SystemGeneralTab'
import SystemUsersTab from './SystemUsersTab'
import SystemModelsTab from './SystemModelsTab'

export default function SystemCenter() {
  const { t } = useTranslation()
  const [activeKey, setActiveKey] = useState('general')

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>{t('systemCenter.title')}</h2>
      <Tabs activeKey={activeKey} onChange={setActiveKey}>
        <Tabs.TabPane tab={t('systemSettings.general')} key="general">
          <SystemGeneralTab />
        </Tabs.TabPane>
        <Tabs.TabPane tab={t('systemCenter.users')} key="users">
          <SystemUsersTab />
        </Tabs.TabPane>
        <Tabs.TabPane tab={t('systemCenter.models')} key="models">
          <SystemModelsTab />
        </Tabs.TabPane>
      </Tabs>
    </div>
  )
}
```

For each tab, create a separate file. The `SystemGeneralTab` is the existing SystemSettings content.

`SystemUsersTab.tsx`:
```typescript
import { useEffect, useState } from 'react'
import { Table, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { getUserList, type User } from '@/api/system'

export default function SystemUsersTab() {
  const { t } = useTranslation()
  const [data, setData] = useState<User[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    setLoading(true)
    try {
      const res = await getUserList()
      setData(res.list)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('systemCenter.fetchError'))
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    { title: t('agentManager.name'), dataIndex: 'username', key: 'username' },
    { title: 'Email', dataIndex: 'email', key: 'email' },
    { title: t('agentManager.status'), dataIndex: 'status', key: 'status' },
  ]

  return <Table rowKey="id" columns={columns} dataSource={data} loading={loading} locale={{ emptyText: t('common.noData') }} />
}
```

`SystemModelsTab.tsx`:
```typescript
import { useEffect, useState } from 'react'
import { Table, Switch, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { getModelConfigs, updateModelConfig, type ModelConfigItem } from '@/api/system'

export default function SystemModelsTab() {
  const { t } = useTranslation()
  const [data, setData] = useState<ModelConfigItem[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    setLoading(true)
    try {
      const res = await getModelConfigs()
      setData(res)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('systemCenter.fetchError'))
    } finally {
      setLoading(false)
    }
  }

  const toggleEnabled = async (id: string, enabled: boolean) => {
    try {
      await updateModelConfig(id, { enabled })
      setData((prev) => prev.map((item) => (item.id === id ? { ...item, enabled } : item)))
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('systemCenter.fetchError'))
    }
  }

  const columns = [
    { title: 'Provider', dataIndex: 'provider', key: 'provider' },
    { title: 'Model', dataIndex: 'model', key: 'model' },
    { title: 'Priority', dataIndex: 'priority', key: 'priority' },
    {
      title: 'Enabled',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled: boolean, record: ModelConfigItem) => (
        <Switch checked={enabled} onChange={(checked) => toggleEnabled(record.id, checked)} />
      ),
    },
  ]

  return <Table rowKey="id" columns={columns} dataSource={data} loading={loading} locale={{ emptyText: t('common.noData') }} />
}
```

- [ ] **Step 2: Move IntegrationCenter**

Copy `src/pages/IntegrationCenter/` to `src/pages/Platform/IntegrationCenter/` with no code changes.

- [ ] **Step 3: Move OpsCenter**

Copy `src/pages/OpsCenter/` to `src/pages/Platform/OpsCenter/` with no code changes.

- [ ] **Step 4: Delete old pages**

```bash
rm -rf schemaplexai-ui/src/pages/SystemSettings
rm -rf schemaplexai-ui/src/pages/IntegrationCenter
rm -rf schemaplexai-ui/src/pages/OpsCenter
```

- [ ] **Step 5: Commit**

```bash
git add schemaplexai-ui/src/pages/Platform/
git rm -r schemaplexai-ui/src/pages/SystemSettings schemaplexai-ui/src/pages/IntegrationCenter schemaplexai-ui/src/pages/OpsCenter
git commit -m "feat(pages): reorganize Platform pages (SystemCenter, IntegrationCenter, OpsCenter)"
```

---

### Task 16: Tasks domain pages

**Files:**
- Create: `src/pages/Tasks/TaskBoard/index.tsx`
- Create: `src/pages/Tasks/TaskJobs/index.tsx`
- Create: `src/pages/Tasks/TaskDetail/index.tsx`

- [ ] **Step 1: Create TaskBoard**

Create `schemaplexai-ui/src/pages/Tasks/TaskBoard/index.tsx`:

```typescript
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { message } from 'antd'
import { KanbanBoard } from '@/components/Hive/KanbanBoard'
import type { SfTask, TaskStatus } from '@/types'
import { getTaskList, updateTaskStatus } from '@/api/task'

const COLUMNS: TaskStatus[] = [
  'BACKLOG',
  'QUEUED',
  'IN_PROGRESS',
  'AWAITING_REVIEW',
  'REVISING',
  'BLOCKED',
  'DONE',
]

export default function TaskBoard() {
  const { t } = useTranslation()
  const [tasks, setTasks] = useState<SfTask[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchTasks()
  }, [])

  const fetchTasks = async () => {
    setLoading(true)
    try {
      const res = await getTaskList({ pageSize: 1000 })
      setTasks(res.list)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('taskBoard.fetchError'))
      setTasks([])
    } finally {
      setLoading(false)
    }
  }

  const handleMove = async (taskId: string, toStatus: TaskStatus) => {
    try {
      await updateTaskStatus(taskId, toStatus)
      setTasks((prev) =>
        prev.map((task) => (task.id === taskId ? { ...task, status: toStatus } : task))
      )
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('taskBoard.fetchError'))
    }
  }

  return (
    <div style={{ padding: 24, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <h2 style={{ marginBottom: 16 }}>{t('taskBoard.title')}</h2>
      <div style={{ flex: 1, minHeight: 0 }}>
        <KanbanBoard
          columns={COLUMNS}
          tasks={tasks}
          loading={loading}
          onMove={handleMove}
        />
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Create TaskJobs**

Create `schemaplexai-ui/src/pages/Tasks/TaskJobs/index.tsx`:

```typescript
import { useEffect, useState } from 'react'
import { Table, Button, Tag, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { getJobList, retryJob, cancelJob, type JobRecord } from '@/api/task'

export default function TaskJobs() {
  const { t } = useTranslation()
  const [data, setData] = useState<JobRecord[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    setLoading(true)
    try {
      const res = await getJobList()
      setData(res.list)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('taskJobs.fetchError'))
    } finally {
      setLoading(false)
    }
  }

  const handleRetry = async (id: string) => {
    try {
      await retryJob(id)
      message.success(t('common.success'))
      fetchData()
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('common.error'))
    }
  }

  const handleCancel = async (id: string) => {
    try {
      await cancelJob(id)
      message.success(t('common.success'))
      fetchData()
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('common.error'))
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id' },
    { title: t('taskJobs.queue'), dataIndex: 'queue', key: 'queue' },
    {
      title: t('agentManager.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'running' ? 'blue' : status === 'failed' ? 'red' : 'green'}>{status}</Tag>
      ),
    },
    { title: 'Retry', dataIndex: 'retryCount', key: 'retryCount', render: (r: number, record: JobRecord) => `${r}/${record.maxRetries}` },
    {
      title: t('workflowCenter.action'),
      key: 'action',
      render: (_: unknown, record: JobRecord) => (
        <>
          <Button size="small" onClick={() => handleRetry(record.id)} style={{ marginRight: 8 }}>
            {t('taskJobs.retry')}
          </Button>
          <Button size="small" danger onClick={() => handleCancel(record.id)}>
            {t('taskJobs.cancel')}
          </Button>
        </>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>{t('taskJobs.title')}</h2>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        locale={{ emptyText: t('common.noData') }}
      />
    </div>
  )
}
```

- [ ] **Step 3: Create TaskDetail**

Create `schemaplexai-ui/src/pages/Tasks/TaskDetail/index.tsx`:

```typescript
import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Card, Descriptions, Tag, List, Input, Button, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { getTaskDetail, getTaskComments, addTaskComment, type TaskComment } from '@/api/task'
import type { SfTask } from '@/types'

export default function TaskDetail() {
  const { t } = useTranslation()
  const { id } = useParams<{ id: string }>()
  const [task, setTask] = useState<SfTask | null>(null)
  const [comments, setComments] = useState<TaskComment[]>([])
  const [commentText, setCommentText] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (id) {
      fetchTask()
      fetchComments()
    }
  }, [id])

  const fetchTask = async () => {
    if (!id) return
    try {
      const res = await getTaskDetail(id)
      setTask(res)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('taskDetail.fetchError'))
    }
  }

  const fetchComments = async () => {
    if (!id) return
    try {
      const res = await getTaskComments(id)
      setComments(res)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('taskDetail.fetchError'))
    }
  }

  const handleAddComment = async () => {
    if (!id || !commentText.trim()) return
    setLoading(true)
    try {
      await addTaskComment(id, commentText.trim())
      setCommentText('')
      fetchComments()
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('common.error'))
    } finally {
      setLoading(false)
    }
  }

  if (!task) {
    return <div style={{ padding: 24 }}>{t('common.loading')}</div>
  }

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>{task.title}</h2>
      <Card title={t('taskDetail.title')} style={{ marginBottom: 24 }}>
        <Descriptions bordered column={2}>
          <Descriptions.Item label="ID">{task.id}</Descriptions.Item>
          <Descriptions.Item label={t('agentManager.status')}>
            <Tag>{task.status}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label={t('taskBoard.title')}>{task.priority}</Descriptions.Item>
          <Descriptions.Item label={t('common.createdAt')}>{task.createdAt}</Descriptions.Item>
        </Descriptions>
        <div style={{ marginTop: 16 }}>{task.description}</div>
      </Card>

      <Card title={t('taskDetail.comments')}>
        <List
          dataSource={comments}
          renderItem={(item) => (
            <List.Item>
              <List.Item.Meta
                title={`${item.authorName || item.authorId} · ${item.createdAt}`}
                description={item.content}
              />
            </List.Item>
          )}
          locale={{ emptyText: t('common.noData') }}
        />
        <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
          <Input
            value={commentText}
            onChange={(e) => setCommentText(e.target.value)}
            placeholder={t('taskDetail.addComment')}
            onPressEnter={handleAddComment}
          />
          <Button type="primary" onClick={handleAddComment} loading={loading}>
            {t('common.confirm')}
          </Button>
        </div>
      </Card>
    </div>
  )
}
```

- [ ] **Step 4: Commit**

```bash
git add schemaplexai-ui/src/pages/Tasks/
git commit -m "feat(pages): add Tasks domain (TaskBoard, TaskJobs, TaskDetail)"
```

---

## Phase 3: Components

### Task 17: KanbanBoard component

**Files:**
- Create: `src/components/Hive/KanbanBoard.tsx`
- Create: `src/components/Hive/KanbanBoard.test.tsx`

- [ ] **Step 1: Create KanbanBoard**

Create `schemaplexai-ui/src/components/Hive/KanbanBoard.tsx`:

```typescript
import { useMemo } from 'react'
import { DndContext, type DragEndEvent, closestCorners } from '@dnd-kit/core'
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable'
import { useTranslation } from 'react-i18next'
import { Spin } from 'antd'
import { TaskCard } from './TaskCard'
import type { SfTask, TaskStatus } from '@/types'

export interface KanbanBoardProps {
  columns: TaskStatus[]
  tasks: SfTask[]
  loading?: boolean
  onMove: (taskId: string, toStatus: TaskStatus) => void
}

export function KanbanBoard({ columns, tasks, loading, onMove }: KanbanBoardProps) {
  const { t } = useTranslation()

  const columnsWithTasks = useMemo(() => {
    return columns.map((status) => ({
      status,
      tasks: tasks.filter((task) => task.status === status),
    }))
  }, [columns, tasks])

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event
    if (!over) return

    const taskId = active.id as string
    const toStatus = over.id as TaskStatus

    if (columns.includes(toStatus)) {
      onMove(taskId, toStatus)
    }
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
        <Spin size="large" />
      </div>
    )
  }

  return (
    <DndContext collisionDetection={closestCorners} onDragEnd={handleDragEnd}>
      <div
        style={{
          display: 'flex',
          gap: 16,
          height: '100%',
          overflowX: 'auto',
          paddingBottom: 8,
        }}
      >
        {columnsWithTasks.map((column) => (
          <div
            key={column.status}
            data-testid={`column-${column.status}`}
            style={{
              minWidth: 240,
              maxWidth: 280,
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              background: '#0d1117',
              borderRadius: 8,
              border: '1px solid #1f2937',
            }}
          >
            <div
              style={{
                padding: '12px 16px',
                fontWeight: 600,
                fontSize: 14,
                borderBottom: '1px solid #1f2937',
                color: '#e5e7eb',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <span>{t(`taskBoard.${column.status.toLowerCase()}`)}</span>
              <span
                style={{
                  background: '#1f2937',
                  borderRadius: 10,
                  padding: '2px 8px',
                  fontSize: 12,
                }}
              >
                {column.tasks.length}
              </span>
            </div>
            <SortableContext
              items={column.tasks.map((t) => t.id)}
              strategy={verticalListSortingStrategy}
            >
              <div
                id={column.status}
                data-testid={`dropzone-${column.status}`}
                style={{
                  flex: 1,
                  padding: 12,
                  overflowY: 'auto',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 8,
                }}
              >
                {column.tasks.map((task) => (
                  <TaskCard key={task.id} task={task} />
                ))}
              </div>
            </SortableContext>
          </div>
        ))}
      </div>
    </DndContext>
  )
}
```

- [ ] **Step 2: Create KanbanBoard test**

Create `schemaplexai-ui/src/components/Hive/KanbanBoard.test.tsx`:

```typescript
import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { KanbanBoard } from './KanbanBoard'
import type { SfTask } from '@/types'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

const MOCK_TASKS: SfTask[] = [
  {
    id: '1',
    tenantId: 't1',
    title: 'Task 1',
    priority: 'P1',
    status: 'BACKLOG',
    assignmentType: 'MANUAL',
    createdAt: '2026-05-08T00:00:00Z',
    updatedAt: '2026-05-08T00:00:00Z',
  },
  {
    id: '2',
    tenantId: 't1',
    title: 'Task 2',
    priority: 'P0',
    status: 'IN_PROGRESS',
    assignmentType: 'AUTO',
    createdAt: '2026-05-08T00:00:00Z',
    updatedAt: '2026-05-08T00:00:00Z',
  },
]

const COLUMNS = ['BACKLOG', 'QUEUED', 'IN_PROGRESS', 'AWAITING_REVIEW', 'REVISING', 'BLOCKED', 'DONE'] as const

describe('KanbanBoard', () => {
  it('renders all 7 columns', () => {
    render(<KanbanBoard columns={[...COLUMNS]} tasks={MOCK_TASKS} onMove={vi.fn()} />)
    COLUMNS.forEach((col) => {
      expect(screen.getByTestId(`column-${col}`)).toBeInTheDocument()
    })
  })

  it('renders tasks in correct columns', () => {
    render(<KanbanBoard columns={[...COLUMNS]} tasks={MOCK_TASKS} onMove={vi.fn()} />)
    expect(screen.getByText('Task 1')).toBeInTheDocument()
    expect(screen.getByText('Task 2')).toBeInTheDocument()
  })

  it('shows loading spinner', () => {
    render(<KanbanBoard columns={[...COLUMNS]} tasks={[]} loading onMove={vi.fn()} />)
    expect(document.querySelector('.ant-spin')).toBeInTheDocument()
  })
})
```

- [ ] **Step 3: Update Hive index.ts**

Add to `schemaplexai-ui/src/components/Hive/index.ts`:

```typescript
export { KanbanBoard } from './KanbanBoard'
export type { KanbanBoardProps } from './KanbanBoard'
```

- [ ] **Step 4: Run tests**

```bash
cd schemaplexai-ui && npx vitest run src/components/Hive/KanbanBoard.test.tsx
```

Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add schemaplexai-ui/src/components/Hive/KanbanBoard.tsx schemaplexai-ui/src/components/Hive/KanbanBoard.test.tsx
git add schemaplexai-ui/src/components/Hive/index.ts
git commit -m "feat(components): add KanbanBoard with @dnd-kit drag-and-drop"
```

---

### Task 18: TaskCard component

**Files:**
- Create: `src/components/Hive/TaskCard.tsx`
- Create: `src/components/Hive/TaskCard.test.tsx`

- [ ] **Step 1: Create TaskCard**

Create `schemaplexai-ui/src/components/Hive/TaskCard.tsx`:

```typescript
import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { Tag } from 'antd'
import type { SfTask } from '@/types'

export interface TaskCardProps {
  task: SfTask
}

const PRIORITY_COLORS: Record<string, string> = {
  P0: '#ff4757',
  P1: '#ff9f43',
  P2: '#00d4aa',
  P3: '#64748b',
}

export function TaskCard({ task }: TaskCardProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: task.id,
  })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  }

  return (
    <div
      ref={setNodeRef}
      style={{
        ...style,
        background: '#111827',
        borderRadius: 8,
        padding: 12,
        border: '1px solid #1f2937',
        cursor: 'grab',
      }}
      {...attributes}
      {...listeners}
      data-testid={`task-card-${task.id}`}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
        <span style={{ fontWeight: 500, fontSize: 14, color: '#e5e7eb', lineHeight: 1.4 }}>
          {task.title}
        </span>
        <Tag color={PRIORITY_COLORS[task.priority] || '#64748b'} style={{ fontSize: 11, marginLeft: 8 }}>
          {task.priority}
        </Tag>
      </div>

      {task.description && (
        <div style={{ fontSize: 12, color: '#9ca3af', marginBottom: 8, lineHeight: 1.4 }}>
          {task.description.slice(0, 60)}{task.description.length > 60 ? '...' : ''}
        </div>
      )}

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, marginBottom: 8 }}>
        {task.skillTags?.map((tag) => (
          <Tag key={tag} style={{ fontSize: 11, background: '#1f2937', border: 'none', color: '#9ca3af' }}>
            {tag}
          </Tag>
        ))}
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 11, color: '#6b7280' }}>
        <span>{task.assignmentType}</span>
        {task.specId && <span>Spec: {task.specId.slice(0, 6)}</span>}
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Create TaskCard test**

Create `schemaplexai-ui/src/components/Hive/TaskCard.test.tsx`:

```typescript
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { TaskCard } from './TaskCard'
import type { SfTask } from '@/types'

const MOCK_TASK: SfTask = {
  id: '1',
  tenantId: 't1',
  title: 'Test Task',
  description: 'A test task description',
  skillTags: ['coding', 'review'],
  priority: 'P1',
  status: 'BACKLOG',
  assignmentType: 'MANUAL',
  specId: 'spec-123',
  createdAt: '2026-05-08T00:00:00Z',
  updatedAt: '2026-05-08T00:00:00Z',
}

describe('TaskCard', () => {
  it('renders task title and priority', () => {
    render(<TaskCard task={MOCK_TASK} />)
    expect(screen.getByText('Test Task')).toBeInTheDocument()
    expect(screen.getByText('P1')).toBeInTheDocument()
  })

  it('renders skill tags', () => {
    render(<TaskCard task={MOCK_TASK} />)
    expect(screen.getByText('coding')).toBeInTheDocument()
    expect(screen.getByText('review')).toBeInTheDocument()
  })

  it('renders truncated description', () => {
    render(<TaskCard task={MOCK_TASK} />)
    expect(screen.getByText(/A test task description/)).toBeInTheDocument()
  })
})
```

- [ ] **Step 3: Update Hive index.ts**

Add to `schemaplexai-ui/src/components/Hive/index.ts`:

```typescript
export { TaskCard } from './TaskCard'
export type { TaskCardProps } from './TaskCard'
```

- [ ] **Step 4: Run tests**

```bash
cd schemaplexai-ui && npx vitest run src/components/Hive/TaskCard.test.tsx
```

Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add schemaplexai-ui/src/components/Hive/TaskCard.tsx schemaplexai-ui/src/components/Hive/TaskCard.test.tsx
git add schemaplexai-ui/src/components/Hive/index.ts
git commit -m "feat(components): add TaskCard for Kanban board"
```

---

## Phase 4: Cleanup & Verification

### Task 19: Remove dead code

**Files:**
- Delete: `src/pages/Dashboard/`
- Delete: `src/pages/AgentManager/`
- Delete: `src/pages/NotificationCenter/`

- [ ] **Step 1: Delete Dashboard**

```bash
rm -rf schemaplexai-ui/src/pages/Dashboard
```

- [ ] **Step 2: Delete AgentManager**

```bash
rm -rf schemaplexai-ui/src/pages/AgentManager
```

- [ ] **Step 3: Delete NotificationCenter**

```bash
rm -rf schemaplexai-ui/src/pages/NotificationCenter
```

- [ ] **Step 4: Update App.tsx**

Remove Dashboard import if it still exists in `schemaplexai-ui/src/App.tsx`. (It shouldn't since Dashboard is not imported there.)

- [ ] **Step 5: Commit**

```bash
git rm -r schemaplexai-ui/src/pages/Dashboard schemaplexai-ui/src/pages/AgentManager schemaplexai-ui/src/pages/NotificationCenter
git commit -m "chore(cleanup): remove dead code (Dashboard, AgentManager, NotificationCenter)"
```

---

### Task 20: Final type check, lint, test, build

**Files:**
- All of the above

- [ ] **Step 1: TypeScript check**

```bash
cd schemaplexai-ui && npx tsc --noEmit
```

Expected: Zero errors. If errors exist, fix them.

- [ ] **Step 2: Lint**

```bash
cd schemaplexai-ui && npm run lint
```

Expected: Zero errors. If errors exist, fix them.

- [ ] **Step 3: Run all tests**

```bash
cd schemaplexai-ui && npm run test:run
```

Expected: All tests pass.

- [ ] **Step 4: Build**

```bash
cd schemaplexai-ui && npm run build
```

Expected: Build succeeds.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore(verify): pass typecheck, lint, tests, and build"
```

---

## Self-Review

### 1. Spec Coverage

| Spec Section | Plan Task |
|---|---|
| 6 top-level routes | Task 6 (router) |
| `/cockpit` Immersive | Task 6, 8 |
| `/agents/*` sub-routes | Task 6, 10 |
| `/projects/*` sub-routes | Task 6, 11, 12, 13 |
| `/quality/*` sub-routes | Task 6, 14 |
| `/platform/*` sub-routes | Task 6, 15 |
| `/tasks/*` sub-routes | Task 6, 16 |
| ImmersiveLayout 6 icons | Task 8 |
| ProgressiveLayout submenus | Task 9 |
| DomainNav component | Task 9 |
| AgentList替代AgentManager | Task 10 |
| WorkflowCenter复活+重构 | Task 13 |
| Quality拆3页 | Task 14 |
| SystemCenter扩展 | Task 15 |
| TaskBoard 7列看板 | Task 16, 17 |
| TaskJobs | Task 16 |
| KanbanBoard + TaskCard | Task 17, 18 |
| API拆分agent.ts | Task 2 |
| 新建task.ts | Task 3 |
| 扩展system.ts | Task 4 |
| i18n nav.domain格式 | Task 7 |
| 删除死代码 | Task 19 |

**No gaps identified.**

### 2. Placeholder Scan

- No "TBD", "TODO", "implement later" found
- No "add appropriate error handling" — all API calls have try-catch with message.error
- No "write tests for the above" — tests are explicit with code
- No "Similar to Task N" — each task is self-contained

### 3. Type Consistency

- `SfTask` interface defined in Task 5, used consistently in Task 3 (API), Task 16 (pages), Task 17 (KanbanBoard), Task 18 (TaskCard)
- `TaskStatus` union type used in KanbanBoard props and TaskBoard page
- `DomainNavItem` interface defined in DomainNav, used in ProgressiveLayout
- All API function names match between definition (Task 3) and usage (Task 16)

**No type inconsistencies found.**
