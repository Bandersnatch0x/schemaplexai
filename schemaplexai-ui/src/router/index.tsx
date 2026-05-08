import { Navigate } from 'react-router-dom'
import { lazy } from 'react'
import type { ReactNode } from 'react'
import { ImmersiveLayout, ProgressiveLayout } from '@/components/Layout'

const Login = lazy(() => import('@/pages/Login'))
const Cockpit = lazy(() => import('@/pages/Cockpit'))
const AgentCanvas = lazy(() => import('@/pages/AgentCanvas'))
const AgentManager = lazy(() => import('@/pages/AgentManager'))
const AgentExecutor = lazy(() => import('@/pages/AgentExecutor'))
const AgentDetail = lazy(() => import('@/pages/AgentDetail'))
const SpecCenter = lazy(() => import('@/pages/SpecCenter'))
const WorkflowMonitor = lazy(() => import('@/pages/WorkflowMonitor'))
const ContextCenter = lazy(() => import('@/pages/ContextCenter'))
const QualityCenter = lazy(() => import('@/pages/Quality/QualityCenter'))
const IntegrationCenter = lazy(() => import('@/pages/IntegrationCenter'))
const OpsCenter = lazy(() => import('@/pages/OpsCenter'))
const SystemSettings = lazy(() => import('@/pages/SystemSettings'))
const NotificationCenter = lazy(() => import('@/pages/NotificationCenter'))
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

  // ImmersiveLayout — full-screen dashboards
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
    path: '/canvas',
    element: (
      <RequireAuth>
        <ImmersiveLayout />
      </RequireAuth>
    ),
    children: [{ path: '', element: <AgentCanvas /> }],
  },

  // ProgressiveLayout — detail / config pages
  {
    path: '/agents',
    element: (
      <RequireAuth>
        <ProgressiveLayout />
      </RequireAuth>
    ),
    children: [
      { path: '', element: <AgentManager /> },
      { path: 'executor', element: <AgentExecutor /> },
      { path: ':id', element: <AgentDetail /> },
    ],
  },
  {
    path: '/workflows',
    element: (
      <RequireAuth>
        <ProgressiveLayout />
      </RequireAuth>
    ),
    children: [{ path: '', element: <WorkflowMonitor /> }],
  },
  {
    path: '/specs',
    element: (
      <RequireAuth>
        <ProgressiveLayout />
      </RequireAuth>
    ),
    children: [{ path: '', element: <SpecCenter /> }],
  },
  {
    path: '/contexts',
    element: (
      <RequireAuth>
        <ProgressiveLayout />
      </RequireAuth>
    ),
    children: [{ path: '', element: <ContextCenter /> }],
  },
  {
    path: '/quality',
    element: (
      <RequireAuth>
        <ProgressiveLayout />
      </RequireAuth>
    ),
    children: [{ path: '', element: <QualityCenter /> }],
  },
  {
    path: '/integrations',
    element: (
      <RequireAuth>
        <ProgressiveLayout />
      </RequireAuth>
    ),
    children: [{ path: '', element: <IntegrationCenter /> }],
  },
  {
    path: '/ops',
    element: (
      <RequireAuth>
        <ProgressiveLayout />
      </RequireAuth>
    ),
    children: [{ path: '', element: <OpsCenter /> }],
  },
  {
    path: '/notifications',
    element: (
      <RequireAuth>
        <ProgressiveLayout />
      </RequireAuth>
    ),
    children: [{ path: '', element: <NotificationCenter /> }],
  },
  {
    path: '/settings',
    element: (
      <RequireAuth>
        <ProgressiveLayout />
      </RequireAuth>
    ),
    children: [{ path: '', element: <SystemSettings /> }],
  },

  { path: '*', element: <NotFound /> },
]

export default RouterConfig
