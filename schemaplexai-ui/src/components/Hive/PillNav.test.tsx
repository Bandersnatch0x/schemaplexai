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
    render(<PillNav items={items} activeKey="a" onChange={vi.fn()} />)
    expect(screen.getByText('Topology')).toBeInTheDocument()
    expect(screen.getByText('List')).toBeInTheDocument()
    expect(screen.getByText('Code')).toBeInTheDocument()
  })

  it('highlights active item with cyan background', () => {
    render(<PillNav items={items} activeKey="b" onChange={vi.fn()} />)
    const active = screen.getByText('List')
    expect(active).toHaveStyle({ backgroundColor: '#00d4aa' })
  })

  it('calls onChange when clicking inactive item', () => {
    const onChange = vi.fn()
    render(<PillNav items={items} activeKey="a" onChange={onChange} />)
    fireEvent.click(screen.getByText('List'))
    expect(onChange).toHaveBeenCalledWith('b')
  })
})
