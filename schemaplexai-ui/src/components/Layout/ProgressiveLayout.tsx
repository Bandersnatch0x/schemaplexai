import { Outlet, useNavigate, useLocation } from 'react-router-dom'

export interface ProgressiveLayoutProps {
  children?: React.ReactNode
}

const NAV_ITEMS = [
  { key: 'cockpit', icon: '◉', label: '驾驶舱', path: '/cockpit' },
  { key: 'canvas', icon: '◆', label: '编排画布', path: '/canvas' },
  { key: 'workflows', icon: '▲', label: '工作流', path: '/workflows' },
  { key: 'agents', icon: '●', label: 'Agent', path: '/agents' },
]

export function ProgressiveLayout({ children }: ProgressiveLayoutProps) {
  const navigate = useNavigate()
  const location = useLocation()

  const activeKey = NAV_ITEMS.find(item => location.pathname.startsWith(item.path))?.key || 'cockpit'

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', background: '#0a0e1a' }}>
      {/* Header */}
      <header
        style={{
          height: 48,
          background: '#0d1117',
          borderBottom: '1px solid #1e2a33',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 24px',
        }}
      >
        <span style={{ color: '#00d4aa', fontSize: 14, fontWeight: 600 }}>SchemaPlexAI</span>
        <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
          <span style={{ color: '#64748b', fontSize: 12 }}>租户 ▼</span>
          <span style={{ color: '#64748b', fontSize: 14 }}>🔔</span>
          <div style={{ width: 28, height: 28, borderRadius: '50%', background: '#1e2a33' }} />
        </div>
      </header>

      {/* Body */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* Sidebar */}
        <aside
          style={{
            width: 200,
            background: '#0d1117',
            borderRight: '1px solid #1e2a33',
            display: 'flex',
            flexDirection: 'column',
            padding: '12px 0',
          }}
        >
          {NAV_ITEMS.map(item => {
            const isActive = item.key === activeKey
            return (
              <div
                key={item.key}
                onClick={() => navigate(item.path)}
                style={{
                  padding: '10px 20px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12,
                  cursor: 'pointer',
                  background: isActive ? '#111827' : 'transparent',
                  borderLeft: `3px solid ${isActive ? '#00d4aa' : 'transparent'}`,
                  color: isActive ? '#e2e8f0' : '#64748b',
                  fontSize: 13,
                  transition: 'all 0.15s ease',
                }}
              >
                <span style={{ color: isActive ? '#00d4aa' : '#64748b', fontSize: 14 }}>
                  {item.icon}
                </span>
                {item.label}
              </div>
            )
          })}
        </aside>

        {/* Content */}
        <main style={{ flex: 1, padding: 24, overflow: 'auto' }}>
          {children ?? <Outlet />}
        </main>
      </div>
    </div>
  )
}
