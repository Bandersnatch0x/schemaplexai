import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { HexIcon } from './HexIcon'

describe('HexIcon', () => {
  it('renders with default size and color', () => {
    render(<HexIcon data-testid="hex" />)
    const icon = screen.getByTestId('hex')
    expect(icon).toBeInTheDocument()
    expect(icon).toHaveStyle({ width: '36px', height: '36px' })
  })

  it('renders with custom size', () => {
    render(<HexIcon size={48} data-testid="hex-large" />)
    const icon = screen.getByTestId('hex-large')
    expect(icon).toHaveStyle({ width: '48px', height: '48px' })
  })

  it('applies active styles when active=true', () => {
    render(<HexIcon active data-testid="hex-active" />)
    const icon = screen.getByTestId('hex-active')
    expect(icon).toHaveStyle({ borderColor: '#00d4aa' })
  })
})
