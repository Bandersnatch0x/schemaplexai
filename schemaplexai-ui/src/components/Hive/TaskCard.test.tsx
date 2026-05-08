import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { TaskCard } from './TaskCard'
import type { SfTask } from '@/types'

const MOCK_TASK: SfTask = {
  id: '1',
  tenantId: 't1',
  title: 'Test Task',
  description: 'A test task description',
  skillTags: ['coding', 'review'],
  priority: 'P1',
  status: 'BACKLOG',
  assignmentType: 'MANUAL',
  specId: 'spec-123',
  createdAt: '2026-05-08T00:00:00Z',
  updatedAt: '2026-05-08T00:00:00Z',
}

describe('TaskCard', () => {
  it('renders task title and priority', () => {
    render(<TaskCard task={MOCK_TASK} />)
    expect(screen.getByText('Test Task')).toBeInTheDocument()
    expect(screen.getByText('P1')).toBeInTheDocument()
  })

  it('renders skill tags', () => {
    render(<TaskCard task={MOCK_TASK} />)
    expect(screen.getByText('coding')).toBeInTheDocument()
    expect(screen.getByText('review')).toBeInTheDocument()
  })

  it('renders truncated description', () => {
    render(<TaskCard task={MOCK_TASK} />)
    expect(screen.getByText(/A test task description/)).toBeInTheDocument()
  })
})
