import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StatCard } from './StatCard'

describe('StatCard', () => {
  it('renders label and value', () => {
    render(<StatCard label="Active Agents" value="7,843" />)
    expect(screen.getByText('ACTIVE AGENTS')).toBeInTheDocument()
    expect(screen.getByText('7,843')).toBeInTheDocument()
  })

  it('renders change indicator', () => {
    render(<StatCard label="Test" value="100" change={12.5} />)
    expect(screen.getByText('↑ 12.5%')).toBeInTheDocument()
  })

  it('renders sparkline bars', () => {
    render(<StatCard label="Test" value="100" sparkline={[40, 60, 45, 80, 55, 70, 90]} />)
    const bars = screen.getAllByTestId('spark-bar')
    expect(bars).toHaveLength(7)
  })
})
