import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'

export interface ImmersiveLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { key: 'cockpit', icon: '◉', label: '驾驶舱', path: '/cockpit' },
  { key: 'canvas', icon: '◆', label: '编排画布', path: '/canvas' },
  { key: 'workflows', icon: '▲', label: '工作流', path: '/workflows' },
  { key: 'agents', icon: '●', label: 'Agent', path: '/agents' },
]

export function ImmersiveLayout({ children }: ImmersiveLayoutProps) {
  const navigate = useNavigate()
  const location = useLocation()
  const [hovered, setHovered] = useState<string | null>(null)

  const activeKey = NAV_ITEMS.find(item => location.pathname.startsWith(item.path))?.key || 'cockpit'

  return (
    <div style={{ display: 'flex', height: '100vh', background: '#0a0e1a' }}>
      {/* Left Icon Sidebar */}
      <div
        style={{
          width: 52,
          background: '#0d1117',
          borderRight: '1px solid #1e2a33',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          padding: '10px 0',
          gap: 10,
          zIndex: 10,
        }}
      >
        <div
          style={{
            width: 32,
            height: 32,
            background: '#00d4aa',
            borderRadius: 6,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#0a0e1a',
            fontSize: 12,
            fontWeight: 'bold',
          }}
        >
          S
        </div>

        {NAV_ITEMS.map(item => {
          const isActive = item.key === activeKey
          return (
            <div
              key={item.key}
              data-testid={`sidebar-${item.key}`}
              onClick={() => navigate(item.path)}
              onMouseEnter={() => setHovered(item.key)}
              onMouseLeave={() => setHovered(null)}
              style={{
                width: 36,
                height: 36,
                borderRadius: 8,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
                background: isActive ? '#00d4aa20' : '#1e2a33',
                border: `1px solid ${isActive ? '#00d4aa' : 'transparent'}`,
                boxShadow: isActive ? '0 0 8px #00d4aa30' : 'none',
                transition: 'all 0.2s ease',
                position: 'relative',
              }}
            >
              <span style={{ color: isActive ? '#00d4aa' : '#64748b', fontSize: 14 }}>
                {item.icon}
              </span>
              {hovered === item.key && (
                <div
                  style={{
                    position: 'absolute',
                    left: 44,
                    background: '#111827',
                    border: '1px solid #1e2a33',
                    borderRadius: 6,
                    padding: '4px 10px',
                    color: '#e2e8f0',
                    fontSize: 12,
                    whiteSpace: 'nowrap',
                    zIndex: 20,
                  }}
                >
                  {item.label}
                </div>
              )}
            </div>
          )
        })}
      </div>

      {/* Main Canvas */}
      <div style={{ flex: 1, position: 'relative', overflow: 'hidden' }}>
        {/* Subtle grid background */}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            opacity: 0.02,
            backgroundImage: 'linear-gradient(#1e2a33 1px, transparent 1px), linear-gradient(90deg, #1e2a33 1px, transparent 1px)',
            backgroundSize: '40px 40px',
            pointerEvents: 'none',
          }}
        />

        {/* Floating Header */}
        <div
          style={{
            position: 'absolute',
            top: 12,
            left: '50%',
            transform: 'translateX(-50%)',
            background: 'rgba(17, 24, 39, 0.8)',
            border: '1px solid #1e2a33',
            borderRadius: 20,
            padding: '8px 24px',
            color: '#64748b',
            fontSize: 12,
            backdropFilter: 'blur(12px)',
            display: 'flex',
            gap: 20,
            alignItems: 'center',
            zIndex: 5,
          }}
        >
          <span>SchemaPlexAI</span>
          <span style={{ color: '#1e2a33' }}>|</span>
          <span><span style={{ color: '#00d4aa' }}>●</span> 12 Agents</span>
          <span><span style={{ color: '#ff9f43' }}>●</span> 3 Executing</span>
        </div>

        {/* Content */}
        <div style={{ position: 'relative', zIndex: 1, height: '100%' }}>
          {children}
        </div>
      </div>
    </div>
  )
}
