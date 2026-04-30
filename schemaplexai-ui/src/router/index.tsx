import { Navigate } from 'react-router-dom'
import { lazy } from 'react'
import type { ReactNode } from 'react'

const Login = lazy(() => import('@/pages/Login'))
const Layout = lazy(() => import('@/components/Layout'))
const Dashboard = lazy(() => import('@/pages/Dashboard'))
const AgentManager = lazy(() => import('@/pages/AgentManager'))
const AgentExecutor = lazy(() => import('@/pages/AgentExecutor'))
const SpecCenter = lazy(() => import('@/pages/SpecCenter'))
const WorkflowCenter = lazy(() => import('@/pages/WorkflowCenter'))
const ContextCenter = lazy(() => import('@/pages/ContextCenter'))
const QualityCenter = lazy(() => import('@/pages/QualityCenter'))
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
  {
    path: '/login',
    element: <Login />,
  },
  {
    path: '/',
    element: (
      <RequireAuth>
        <Layout />
      </RequireAuth>
    ),
    children: [
      { path: '', element: <Navigate to="/dashboard" replace /> },
      { path: 'dashboard', element: <Dashboard /> },
      { path: 'agents', element: <AgentManager /> },
      { path: 'agents/executor', element: <AgentExecutor /> },
      { path: 'specs', element: <SpecCenter /> },
      { path: 'workflows', element: <WorkflowCenter /> },
      { path: 'contexts', element: <ContextCenter /> },
      { path: 'quality', element: <QualityCenter /> },
      { path: 'integrations', element: <IntegrationCenter /> },
      { path: 'ops', element: <OpsCenter /> },
      { path: 'notifications', element: <NotificationCenter /> },
      { path: 'settings', element: <SystemSettings /> },
    ],
  },
  {
    path: '*',
    element: <NotFound />,
  },
]

export default RouterConfig
