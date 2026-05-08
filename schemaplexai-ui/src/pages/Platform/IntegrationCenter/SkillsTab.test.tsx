import { describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import SkillsTab from './SkillsTab'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('@/api/skill', () => ({
  getSkillList: vi.fn(() =>
    Promise.resolve([
      {
        id: '1',
        name: 'Code Review',
        code: 'code-review',
        description: 'Automated code review skill',
        status: 1,
        createdAt: '2026-05-08T00:00:00Z',
        updatedAt: '2026-05-08T00:00:00Z',
      },
      {
        id: '2',
        name: 'Doc Gen',
        code: 'doc-gen',
        description: 'Documentation generation',
        status: 0,
        createdAt: '2026-05-08T00:00:00Z',
        updatedAt: '2026-05-08T00:00:00Z',
      },
    ])
  ),
  createSkill: vi.fn(() => Promise.resolve('3')),
  updateSkill: vi.fn(() => Promise.resolve(true)),
  deleteSkill: vi.fn(() => Promise.resolve(true)),
}))

describe('SkillsTab', () => {
  it('renders skill list', async () => {
    render(<SkillsTab />)
    await waitFor(() => {
      expect(screen.getByText('Code Review')).toBeInTheDocument()
      expect(screen.getByText('Doc Gen')).toBeInTheDocument()
    })
  })

  it('opens create drawer', async () => {
    render(<SkillsTab />)
    await waitFor(() => screen.getByText('Code Review'))
    const btn = screen.getByText('skill.create')
    await userEvent.click(btn)
    expect(screen.getByLabelText('skill.name')).toBeInTheDocument()
  })
})
