import { Suspense } from 'react'
import { Routes, Route } from 'react-router-dom'
import { Spin } from 'antd'
import { ErrorBoundary } from '@/components/ErrorBoundary'
import RouterConfig from './router'
import './App.css'

function AppFallback() {
  return (
    <div className="app-suspense">
      <Spin size="large" />
    </div>
  )
}

function App() {
  return (
    <ErrorBoundary>
      <Suspense fallback={<AppFallback />}>
        <Routes>
          {RouterConfig.map((route) => (
            <Route key={route.path} path={route.path} element={route.element}>
              {route.children?.map((child) => (
                <Route key={child.path} path={child.path} element={child.element} />
              ))}
            </Route>
          ))}
        </Routes>
      </Suspense>
    </ErrorBoundary>
  )
}

export default App
