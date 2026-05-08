import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ProgressiveLayout } from './ProgressiveLayout'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => {
      const translations: Record<string, string> = {
        'nav.domain.cockpit': '驾驶舱',
        'nav.domain.agents': '智能体',
        'nav.domain.projects': '项目',
        'nav.domain.quality': '质量',
        'nav.domain.platform': '平台',
        'nav.domain.tasks': '任务',
        'nav.sub.list': '列表',
        'nav.sub.executor': '执行器',
        'nav.sub.canvas': '画布',
        'nav.sub.specs': '规格',
        'nav.sub.workflows': '工作流',
        'nav.sub.contexts': '上下文',
        'nav.sub.gates': '门禁',
        'nav.sub.issues': '问题',
        'nav.sub.security': '安全审计',
        'nav.sub.system': '系统',
        'nav.sub.integrations': '集成',
        'nav.sub.ops': '运维',
        'nav.sub.jobs': '作业',
      }
      return translations[key] || key
    },
    i18n: { language: 'zh' },
  }),
  Trans: ({ children }: { children: React.ReactNode }) => children,
}))

vi.mock('@/components/LanguageSwitcher', () => ({
  LanguageSwitcher: () => <div data-testid="language-switcher">Lang</div>,
}))

describe('ProgressiveLayout', () => {
  it('renders children in content area', () => {
    render(
      <MemoryRouter>
        <ProgressiveLayout>
          <div data-testid="content">Page Content</div>
        </ProgressiveLayout>
      </MemoryRouter>
    )
    expect(screen.getByTestId('content')).toBeInTheDocument()
  })

  it('renders DomainNav with 6 domains', () => {
    render(
      <MemoryRouter>
        <ProgressiveLayout>test</ProgressiveLayout>
      </MemoryRouter>
    )
    expect(screen.getByTestId('domain-nav')).toBeInTheDocument()
    expect(screen.getByTestId('nav-group-cockpit')).toBeInTheDocument()
    expect(screen.getByTestId('nav-group-agents')).toBeInTheDocument()
    expect(screen.getByTestId('nav-group-projects')).toBeInTheDocument()
    expect(screen.getByTestId('nav-group-quality')).toBeInTheDocument()
    expect(screen.getByTestId('nav-group-platform')).toBeInTheDocument()
    expect(screen.getByTestId('nav-group-tasks')).toBeInTheDocument()
  })

  it('renders header with brand', () => {
    render(
      <MemoryRouter>
        <ProgressiveLayout>test</ProgressiveLayout>
      </MemoryRouter>
    )
    expect(screen.getByText('SchemaPlexAI')).toBeInTheDocument()
  })
})
