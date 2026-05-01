import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { TerminalLog } from './TerminalLog'

describe('TerminalLog', () => {
  const logs = [
    { timestamp: '14:30:01', level: 'INFO' as const, message: 'Agent initialized' },
    { timestamp: '14:30:15', level: 'WARN' as const, message: 'Queue depth high' },
    { timestamp: '14:30:22', level: 'ERROR' as const, message: 'Connection failed' },
  ]

  it('renders all log entries', () => {
    render(<TerminalLog logs={logs} />)
    expect(screen.getByText('Agent initialized')).toBeInTheDocument()
    expect(screen.getByText('Queue depth high')).toBeInTheDocument()
    expect(screen.getByText('Connection failed')).toBeInTheDocument()
  })

  it('renders blinking cursor at bottom', () => {
    render(<TerminalLog logs={logs} />)
    expect(screen.getByText('▌')).toBeInTheDocument()
  })
})
