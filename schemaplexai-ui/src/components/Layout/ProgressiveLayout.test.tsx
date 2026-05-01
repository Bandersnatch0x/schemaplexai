import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ProgressiveLayout } from './ProgressiveLayout'

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
    expect(screen.getByText('驾驶舱')).toBeInTheDocument()
    expect(screen.getByText('编排画布')).toBeInTheDocument()
    expect(screen.getByText('工作流')).toBeInTheDocument()
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
