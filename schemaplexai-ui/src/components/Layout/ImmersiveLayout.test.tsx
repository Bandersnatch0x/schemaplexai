import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ImmersiveLayout } from './ImmersiveLayout'

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
        'nav.sub.canvas': '画布',
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

describe('ImmersiveLayout', () => {
  it('renders all 7 sidebar icons', () => {
    render(
      <MemoryRouter>
        <ImmersiveLayout>test</ImmersiveLayout>
      </MemoryRouter>
    )
    expect(screen.getByTestId('sidebar-cockpit')).toBeInTheDocument()
    expect(screen.getByTestId('sidebar-agents')).toBeInTheDocument()
    expect(screen.getByTestId('sidebar-projects')).toBeInTheDocument()
    expect(screen.getByTestId('sidebar-quality')).toBeInTheDocument()
    expect(screen.getByTestId('sidebar-platform')).toBeInTheDocument()
    expect(screen.getByTestId('sidebar-tasks')).toBeInTheDocument()
    expect(screen.getByTestId('sidebar-canvas')).toBeInTheDocument()
  })

  it('renders children in content area', () => {
    render(
      <MemoryRouter>
        <ImmersiveLayout>
          <div data-testid="content">Page Content</div>
        </ImmersiveLayout>
      </MemoryRouter>
    )
    expect(screen.getByTestId('content')).toBeInTheDocument()
  })

  it('highlights cockpit by default', () => {
    render(
      <MemoryRouter initialEntries={['/cockpit']}>
        <ImmersiveLayout>test</ImmersiveLayout>
      </MemoryRouter>
    )
    const cockpit = screen.getByTestId('sidebar-cockpit')
    expect(cockpit.className).toContain('layout-nav-item--active')
  })
})
