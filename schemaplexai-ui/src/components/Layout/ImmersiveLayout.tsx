import { useState } from 'react'
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

  const NAV_ITEMS = [
    { key: 'cockpit', icon: '◉', label: t('nav.cockpit'), path: '/cockpit' },
    { key: 'canvas', icon: '◆', label: t('nav.canvas'), path: '/canvas' },
    { key: 'workflows', icon: '▲', label: t('nav.workflows'), path: '/workflows' },
    { key: 'agents', icon: '●', label: t('nav.agents'), path: '/agents' },
  ]

  const activeKey = NAV_ITEMS.find(item => location.pathname.startsWith(item.path))?.key || 'cockpit'

  return (
    <div className="layout-immersive">
      {/* Left Icon Sidebar */}
      <div className="layout-icon-sidebar">
        <div className="layout-icon-sidebar-logo">
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
              className={`layout-nav-item${isActive ? ' layout-nav-item--active' : ''}`}
            >
              <span className={`layout-nav-item-icon${isActive ? ' layout-nav-item-icon--active' : ''}`}>
                {item.icon}
              </span>
              {hovered === item.key && (
                <div className="layout-nav-tooltip">
                  {item.label}
                </div>
              )}
            </div>
          )
        })}

        <div style={{ marginTop: 'auto', paddingTop: 16, display: 'flex', justifyContent: 'center' }}>
          <LanguageSwitcher />
        </div>
      </div>

      {/* Main Canvas */}
      <div className="layout-canvas">
        {/* Subtle grid background */}
        <div className="layout-canvas-grid" />

        {/* Floating Header */}
        <div className="layout-floating-header">
          <span>SchemaPlexAI</span>
          <span className="layout-floating-header-divider">|</span>
          <span><span className="layout-floating-header-dot--cyan">●</span> 12 Agents</span>
          <span><span className="layout-floating-header-dot--amber">●</span> 3 Executing</span>
        </div>

        {/* Content */}
        <div className="layout-canvas-content">
          {children ?? <Outlet />}
        </div>
      </div>
    </div>
  )
}
