import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { HexIcon } from './HexIcon'

describe('HexIcon', () => {
  it('renders with default props', () => {
    render(<HexIcon data-testid="hex-icon">A</HexIcon>)
    const hex = screen.getByTestId('hex-icon')
    expect(hex).toBeInTheDocument()
    expect(hex).toHaveTextContent('A')
  })

  it('renders children content', () => {
    render(<HexIcon data-testid="hex-icon">Icon</HexIcon>)
    expect(screen.getByTestId('hex-icon')).toHaveTextContent('Icon')
  })

  it('applies custom size', () => {
    render(<HexIcon size={64} data-testid="hex-icon">B</HexIcon>)
    const hex = screen.getByTestId('hex-icon')
    expect(hex).toHaveStyle({ width: '64px', height: '64px' })
  })

  it('applies default size of 40 when not specified', () => {
    render(<HexIcon data-testid="hex-icon">C</HexIcon>)
    const hex = screen.getByTestId('hex-icon')
    expect(hex).toHaveStyle({ width: '40px', height: '40px' })
  })

  it('applies custom color', () => {
    render(<HexIcon color="#ff4757" data-testid="hex-icon">D</HexIcon>)
    const hex = screen.getByTestId('hex-icon')
    expect(hex).toHaveStyle({ borderColor: '#ff4757' })
  })

  it('applies default color when not specified', () => {
    render(<HexIcon data-testid="hex-icon">E</HexIcon>)
    const hex = screen.getByTestId('hex-icon')
    expect(hex).toHaveStyle({ borderColor: '#00d4aa' })
  })

  it('applies active state styles', () => {
    render(<HexIcon active data-testid="hex-icon">F</HexIcon>)
    const hex = screen.getByTestId('hex-icon')
    expect(hex).toHaveStyle({
      backgroundColor: '#00d4aa20',
      boxShadow: '0 0 12px #00d4aa30',
    })
  })

  it('does not apply active styles when inactive', () => {
    render(<HexIcon data-testid="hex-icon">G</HexIcon>)
    const hex = screen.getByTestId('hex-icon')
    expect(hex).toHaveStyle({ boxShadow: 'none' })
    expect(hex).not.toHaveStyle({ backgroundColor: '#00d4aa20' })
  })

  it('applies custom className', () => {
    render(<HexIcon className="my-class" data-testid="hex-icon">H</HexIcon>)
    expect(screen.getByTestId('hex-icon')).toHaveClass('my-class')
  })

  it('applies custom inline styles', () => {
    render(<HexIcon style={{ marginTop: 10 }} data-testid="hex-icon">I</HexIcon>)
    expect(screen.getByTestId('hex-icon')).toHaveStyle({ marginTop: '10px' })
  })

  it('renders with hexagonal clip-path', () => {
    render(<HexIcon data-testid="hex-icon">J</HexIcon>)
    const hex = screen.getByTestId('hex-icon')
    expect(hex).toHaveStyle({
      clipPath: 'polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%)',
    })
  })

  it('centers children content', () => {
    render(<HexIcon data-testid="hex-icon">K</HexIcon>)
    const hex = screen.getByTestId('hex-icon')
    expect(hex).toHaveStyle({
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
    })
  })
})
