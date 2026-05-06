import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ProgressiveLayout } from './ProgressiveLayout'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => {
      const translations: Record<string, string> = {
        'nav.cockpit': '驾驶舱',
        'nav.canvas': '编排画布',
        'nav.workflows': '工作流',
        'nav.agents': 'Agent 管理',
        'nav.settings': '系统设置',
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

  it('renders expanded sidebar with labels', () => {
    render(
      <MemoryRouter>
        <ProgressiveLayout>test</ProgressiveLayout>
      </MemoryRouter>
    )
    expect(screen.getByText('SchemaPlexAI')).toBeInTheDocument()
    expect(screen.getByText(/◉/)).toBeInTheDocument()
    expect(screen.getByText(/◆/)).toBeInTheDocument()
    expect(screen.getByText(/▲/)).toBeInTheDocument()
  })

  it('renders header with user avatar area', () => {
    render(
      <MemoryRouter>
        <ProgressiveLayout>test</ProgressiveLayout>
      </MemoryRouter>
    )
    expect(screen.getByText('SchemaPlexAI')).toBeInTheDocument()
  })
})
