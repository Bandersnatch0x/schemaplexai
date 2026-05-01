import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StatCard } from './StatCard'

describe('StatCard', () => {
  it('renders label and value', () => {
    render(<StatCard label="Total Users" value={1234} data-testid="stat-card" />)
    expect(screen.getByTestId('stat-card')).toBeInTheDocument()
    expect(screen.getByText('TOTAL USERS')).toBeInTheDocument()
    expect(screen.getByText('1,234')).toBeInTheDocument()
  })

  it('renders with unit', () => {
    render(<StatCard label="Latency" value={45} unit="ms" data-testid="stat-card" />)
    expect(screen.getByText('ms')).toBeInTheDocument()
  })

  it('renders positive change with upward indicator', () => {
    render(<StatCard label="Revenue" value={99.5} change={12.3} data-testid="stat-card" />)
    expect(screen.getByText('↑ 12.3%')).toBeInTheDocument()
  })

  it('renders negative change with downward indicator', () => {
    render(<StatCard label="Errors" value={5} change={-8.5} data-testid="stat-card" />)
    expect(screen.getByText('↓ 8.5%')).toBeInTheDocument()
  })

  it('renders sparkline bars when provided', () => {
    const sparkline = [10, 20, 15, 30, 25, 40]
    render(<StatCard label="Trend" value={100} sparkline={sparkline} data-testid="stat-card" />)
    const bars = screen.getAllByTestId('spark-bar')
    expect(bars).toHaveLength(6)
  })

  it('does not render sparkline when not provided', () => {
    render(<StatCard label="No Trend" value={50} data-testid="stat-card" />)
    expect(screen.queryAllByTestId('spark-bar')).toHaveLength(0)
  })

  it('applies custom color', () => {
    render(<StatCard label="Custom" value={42} color="red" data-testid="stat-card" />)
    const card = screen.getByTestId('stat-card')
    expect(card).toHaveStyle({ borderLeftColor: '#ff4757' })
  })

  it('formats value with commas for large numbers', () => {
    render(<StatCard label="Large" value={1234567} data-testid="stat-card" />)
    expect(screen.getByText('1,234,567')).toBeInTheDocument()
  })

  it('formats decimal values', () => {
    render(<StatCard label="Decimal" value={99.99} data-testid="stat-card" />)
    expect(screen.getByText('99.99')).toBeInTheDocument()
  })

  it('renders with correct base styling', () => {
    render(<StatCard label="Style" value={1} data-testid="stat-card" />)
    const card = screen.getByTestId('stat-card')
    expect(card).toHaveStyle({
      backgroundColor: '#111827',
      border: '1px solid #1e2a33',
    })
  })

  it('renders string value as-is', () => {
    render(<StatCard label="String" value="N/A" data-testid="stat-card" />)
    expect(screen.getByText('N/A')).toBeInTheDocument()
  })
})
