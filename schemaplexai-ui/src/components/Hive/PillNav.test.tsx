import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { PillNav } from './PillNav'

describe('PillNav', () => {
  const items = [
    { key: 'a', label: 'Topology' },
    { key: 'b', label: 'List' },
    { key: 'c', label: 'Code' },
  ]

  it('renders all items', () => {
    render(<PillNav items={items} activeKey="a" onChange={vi.fn()} data-testid="pill-nav" />)
    expect(screen.getByTestId('pill-nav')).toBeInTheDocument()
    expect(screen.getByText('Topology')).toBeInTheDocument()
    expect(screen.getByText('List')).toBeInTheDocument()
    expect(screen.getByText('Code')).toBeInTheDocument()
  })

  it('highlights active item with cyan background', () => {
    render(<PillNav items={items} activeKey="b" onChange={vi.fn()} />)
    const buttons = screen.getAllByRole('button')
    expect(buttons[1]).toHaveStyle({ backgroundColor: '#00d4aa' })
    expect(buttons[1]).toHaveStyle({ color: '#0a0e1a' })
    expect(buttons[1]).toHaveStyle({ fontWeight: '600' })
  })

  it('inactive items have muted style', () => {
    render(<PillNav items={items} activeKey="a" onChange={vi.fn()} />)
    const buttons = screen.getAllByRole('button')
    expect(buttons[1]).toHaveStyle({ background: 'transparent' })
    expect(buttons[1]).toHaveStyle({ color: '#64748b' })
  })

  it('calls onChange when clicking inactive item', () => {
    const onChange = vi.fn()
    render(<PillNav items={items} activeKey="a" onChange={onChange} />)
    fireEvent.click(screen.getByText('List'))
    expect(onChange).toHaveBeenCalledTimes(1)
    expect(onChange).toHaveBeenCalledWith('b')
  })

  it('does not call onChange when clicking active item', () => {
    const onChange = vi.fn()
    render(<PillNav items={items} activeKey="a" onChange={onChange} />)
    fireEvent.click(screen.getByText('Topology'))
    expect(onChange).not.toHaveBeenCalled()
  })

  it('applies custom className', () => {
    render(<PillNav items={items} activeKey="a" onChange={vi.fn()} className="my-nav" data-testid="pill-nav" />)
    expect(screen.getByTestId('pill-nav')).toHaveClass('my-nav')
  })

  it('renders container with correct base styling', () => {
    render(<PillNav items={items} activeKey="a" onChange={vi.fn()} data-testid="pill-nav" />)
    const nav = screen.getByTestId('pill-nav')
    expect(nav).toHaveStyle({
      display: 'flex',
      backgroundColor: '#0d1117',
      border: '1px solid #1e2a33',
      borderRadius: '20px',
    })
  })

  it('renders empty without items', () => {
    render(<PillNav items={[]} activeKey="" onChange={vi.fn()} data-testid="pill-nav" />)
    expect(screen.getByTestId('pill-nav')).toBeInTheDocument()
    expect(screen.queryAllByRole('button')).toHaveLength(0)
  })
})
