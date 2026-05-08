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
