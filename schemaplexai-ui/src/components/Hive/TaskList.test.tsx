import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { TaskList } from './TaskList'
import type { SfTask } from '@/types'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

const MOCK_TASKS: SfTask[] = [
  {
    id: '1',
    tenantId: 't1',
    title: 'Alpha Task',
    description: 'Do alpha things',
    skillTags: ['coding'],
    priority: 'P0',
    status: 'BACKLOG',
    assignmentType: 'MANUAL',
    createdAt: '2026-05-08T00:00:00Z',
    updatedAt: '2026-05-08T00:00:00Z',
  },
  {
    id: '2',
    tenantId: 't1',
    title: 'Beta Task',
    description: 'Do beta things',
    skillTags: ['review'],
    priority: 'P1',
    status: 'IN_PROGRESS',
    assignmentType: 'AUTO',
    createdAt: '2026-05-08T00:00:00Z',
    updatedAt: '2026-05-08T00:00:00Z',
  },
]

describe('TaskList', () => {
  it('renders all tasks', () => {
    render(<TaskList tasks={MOCK_TASKS} />)
    expect(screen.getByText('Alpha Task')).toBeInTheDocument()
    expect(screen.getByText('Beta Task')).toBeInTheDocument()
  })

  it('filters tasks by title', async () => {
    render(<TaskList tasks={MOCK_TASKS} />)
    const input = screen.getByPlaceholderText('taskBoard.search')
    await userEvent.type(input, 'Alpha')
    expect(screen.getByText('Alpha Task')).toBeInTheDocument()
    expect(screen.queryByText('Beta Task')).not.toBeInTheDocument()
  })

  it('filters tasks by skill tag', async () => {
    render(<TaskList tasks={MOCK_TASKS} />)
    const input = screen.getByPlaceholderText('taskBoard.search')
    await userEvent.type(input, 'review')
    expect(screen.queryByText('Alpha Task')).not.toBeInTheDocument()
    expect(screen.getByText('Beta Task')).toBeInTheDocument()
  })

  it('shows empty state when no tasks match filter', async () => {
    render(<TaskList tasks={MOCK_TASKS} />)
    const input = screen.getByPlaceholderText('taskBoard.search')
    await userEvent.type(input, 'nonexistent')
    expect(screen.queryByText('Alpha Task')).not.toBeInTheDocument()
    expect(screen.queryByText('Beta Task')).not.toBeInTheDocument()
    expect(screen.getByText('common.noData')).toBeInTheDocument()
  })

  it('shows loading skeleton', () => {
    render(<TaskList tasks={[]} loading />)
    expect(screen.queryByText('Alpha Task')).not.toBeInTheDocument()
  })
})
