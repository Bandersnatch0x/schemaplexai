import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { DomainNav } from './DomainNav'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: 'zh' },
  }),
}))

const ITEMS = [
  { key: 'cockpit', icon: '◉', label: 'Cockpit', path: '/cockpit' },
  {
    key: 'agents',
    icon: '●',
    label: 'Agents',
    path: '/agents',
    children: [
      { key: 'list', label: 'List', path: '/agents/list' },
      { key: 'executor', label: 'Executor', path: '/agents/executor' },
    ],
  },
]

describe('DomainNav', () => {
  it('renders top-level items', () => {
    render(
      <MemoryRouter>
        <DomainNav items={ITEMS} />
      </MemoryRouter>
    )
    expect(screen.getByText('Cockpit')).toBeInTheDocument()
    expect(screen.getByText('Agents')).toBeInTheDocument()
  })

  it('expands children on click', () => {
    render(
      <MemoryRouter>
        <DomainNav items={ITEMS} />
      </MemoryRouter>
    )
    fireEvent.click(screen.getByText('Agents'))
    expect(screen.getByTestId('nav-child-list')).toBeInTheDocument()
    expect(screen.getByTestId('nav-child-executor')).toBeInTheDocument()
  })

  it('highlights active route', () => {
    render(
      <MemoryRouter initialEntries={['/agents/list']}>
        <DomainNav items={ITEMS} />
      </MemoryRouter>
    )
    const child = screen.getByTestId('nav-child-list')
    expect(child.className).toContain('domain-nav-child--active')
  })
})
