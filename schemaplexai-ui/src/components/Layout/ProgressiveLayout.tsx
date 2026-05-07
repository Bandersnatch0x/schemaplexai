import { useTranslation } from 'react-i18next'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { LanguageSwitcher } from '@/components/LanguageSwitcher'
import './Layout.css'

export interface ProgressiveLayoutProps {
  children?: React.ReactNode
}

export function ProgressiveLayout({ children }: ProgressiveLayoutProps) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const location = useLocation()

  const NAV_ITEMS = [
    { key: 'cockpit', icon: '◉', label: t('nav.cockpit'), path: '/cockpit' },
    { key: 'canvas', icon: '◆', label: t('nav.canvas'), path: '/canvas' },
    { key: 'workflows', icon: '▲', label: t('nav.workflows'), path: '/workflows' },
    { key: 'agents', icon: '●', label: t('nav.agents'), path: '/agents' },
    { key: 'specs', icon: '≡', label: t('nav.specs'), path: '/specs' },
    { key: 'contexts', icon: '⊞', label: t('nav.contexts'), path: '/contexts' },
    { key: 'quality', icon: '✓', label: t('nav.quality'), path: '/quality' },
    { key: 'integrations', icon: '⚡', label: t('nav.integrations'), path: '/integrations' },
    { key: 'ops', icon: '☁', label: t('nav.ops'), path: '/ops' },
    { key: 'notifications', icon: '◐', label: t('nav.notifications'), path: '/notifications' },
    { key: 'settings', icon: '◎', label: t('nav.settings'), path: '/settings' },
  ]

  const activeKey = NAV_ITEMS.find(item => location.pathname.startsWith(item.path))?.key || 'cockpit'

  return (
    <div className="layout-progressive">
      {/* Header */}
      <header className="layout-progressive-header">
        <span className="layout-progressive-header-brand">SchemaPlexAI</span>
        <div className="layout-progressive-header-actions">
          <LanguageSwitcher />
          <span className="layout-progressive-header-tenant">{t('nav.settings')} ▼</span>
          <span className="layout-progressive-header-bell">🔔</span>
          <div className="layout-progressive-header-avatar" />
        </div>
      </header>

      {/* Body */}
      <div className="layout-progressive-body">
        {/* Sidebar */}
        <aside className="layout-progressive-sidebar">
          {NAV_ITEMS.map(item => {
            const isActive = item.key === activeKey
            return (
              <div
                key={item.key}
                onClick={() => navigate(item.path)}
                className={`layout-progressive-nav-item${isActive ? ' layout-progressive-nav-item--active' : ''}`}
              >
                <span className={`layout-progressive-nav-item-icon${isActive ? ' layout-progressive-nav-item-icon--active' : ''}`}>
                  {item.icon}
                </span>
                {item.label}
              </div>
            )
          })}
        </aside>

        {/* Content */}
        <main className="layout-progressive-content">
          {children ?? <Outlet />}
        </main>
      </div>
    </div>
  )
}
