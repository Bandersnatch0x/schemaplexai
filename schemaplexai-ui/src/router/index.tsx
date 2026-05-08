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
