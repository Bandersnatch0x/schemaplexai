import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { KanbanBoard } from './KanbanBoard'
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
    title: 'Task 1',
    priority: 'P1',
    status: 'BACKLOG',
    assignmentType: 'MANUAL',
    createdAt: '2026-05-08T00:00:00Z',
    updatedAt: '2026-05-08T00:00:00Z',
  },
  {
    id: '2',
    tenantId: 't1',
    title: 'Task 2',
    priority: 'P0',
    status: 'IN_PROGRESS',
    assignmentType: 'AUTO',
    createdAt: '2026-05-08T00:00:00Z',
    updatedAt: '2026-05-08T00:00:00Z',
  },
]

const COLUMNS = ['BACKLOG', 'QUEUED', 'IN_PROGRESS', 'AWAITING_REVIEW', 'REVISING', 'BLOCKED', 'DONE'] as const

describe('KanbanBoard', () => {
  it('renders all 7 columns', () => {
    render(<KanbanBoard columns={[...COLUMNS]} tasks={MOCK_TASKS} onMove={vi.fn()} />)
    COLUMNS.forEach((col) => {
      expect(screen.getByTestId(`column-${col}`)).toBeInTheDocument()
    })
  })

  it('renders tasks in correct columns', () => {
    render(<KanbanBoard columns={[...COLUMNS]} tasks={MOCK_TASKS} onMove={vi.fn()} />)
    expect(screen.getByText('Task 1')).toBeInTheDocument()
    expect(screen.getByText('Task 2')).toBeInTheDocument()
  })

  it('shows loading spinner', () => {
    render(<KanbanBoard columns={[...COLUMNS]} tasks={[]} loading onMove={vi.fn()} />)
    expect(document.querySelector('.ant-spin')).toBeInTheDocument()
  })
})
