import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ImmersiveLayout } from './ImmersiveLayout'

describe('ImmersiveLayout', () => {
  it('renders children in canvas area', () => {
    render(
      <MemoryRouter>
        <ImmersiveLayout>
          <div data-testid="canvas-content">Canvas</div>
        </ImmersiveLayout>
      </MemoryRouter>
    )
    expect(screen.getByTestId('canvas-content')).toBeInTheDocument()
  })

  it('renders floating header with system name', () => {
    render(
      <MemoryRouter>
        <ImmersiveLayout>test</ImmersiveLayout>
      </MemoryRouter>
    )
    expect(screen.getByText('SchemaPlexAI')).toBeInTheDocument()
  })

  it('renders sidebar icons', () => {
    render(
      <MemoryRouter>
        <ImmersiveLayout>test</ImmersiveLayout>
      </MemoryRouter>
    )
    expect(screen.getByTestId('sidebar-cockpit')).toBeInTheDocument()
    expect(screen.getByTestId('sidebar-canvas')).toBeInTheDocument()
  })
})
