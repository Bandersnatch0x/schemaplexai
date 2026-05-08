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
