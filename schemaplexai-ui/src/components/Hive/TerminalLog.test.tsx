import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { TerminalLog } from './TerminalLog'

describe('TerminalLog', () => {
  const logs = [
    { timestamp: '14:30:01', level: 'INFO' as const, message: 'Agent initialized' },
    { timestamp: '14:30:15', level: 'WARN' as const, message: 'Queue depth high' },
    { timestamp: '14:30:22', level: 'ERROR' as const, message: 'Connection failed' },
    { timestamp: '14:30:30', level: 'DEBUG' as const, message: 'Retry attempt 3' },
  ]

  it('renders all log entries', () => {
    render(<TerminalLog logs={logs} data-testid="terminal-log" />)
    expect(screen.getByTestId('terminal-log')).toBeInTheDocument()
    expect(screen.getByText('Agent initialized')).toBeInTheDocument()
    expect(screen.getByText('Queue depth high')).toBeInTheDocument()
    expect(screen.getByText('Connection failed')).toBeInTheDocument()
    expect(screen.getByText('Retry attempt 3')).toBeInTheDocument()
  })

  it('renders timestamps', () => {
    render(<TerminalLog logs={logs} data-testid="terminal-log" />)
    expect(screen.getByText('[14:30:01]')).toBeInTheDocument()
    expect(screen.getByText('[14:30:15]')).toBeInTheDocument()
  })

  it('renders level labels', () => {
    render(<TerminalLog logs={logs} data-testid="terminal-log" />)
    expect(screen.getByText('INFO')).toBeInTheDocument()
    expect(screen.getByText('WARN')).toBeInTheDocument()
    expect(screen.getByText('ERROR')).toBeInTheDocument()
    expect(screen.getByText('DEBUG')).toBeInTheDocument()
  })

  it('renders INFO level in cyan color', () => {
    render(<TerminalLog logs={[logs[0]]} data-testid="terminal-log" />)
    const infoLevel = screen.getByText('INFO')
    expect(infoLevel).toHaveStyle({ color: '#00d4aa' })
  })

  it('renders WARN level in amber color', () => {
    render(<TerminalLog logs={[logs[1]]} data-testid="terminal-log" />)
    const warnLevel = screen.getByText('WARN')
    expect(warnLevel).toHaveStyle({ color: '#ff9f43' })
  })

  it('renders ERROR level in red color', () => {
    render(<TerminalLog logs={[logs[2]]} data-testid="terminal-log" />)
    const errorLevel = screen.getByText('ERROR')
    expect(errorLevel).toHaveStyle({ color: '#ff4757' })
  })

  it('renders DEBUG level in muted gray color', () => {
    render(<TerminalLog logs={[logs[3]]} data-testid="terminal-log" />)
    const debugLevel = screen.getByText('DEBUG')
    expect(debugLevel).toHaveStyle({ color: '#64748b' })
  })

  it('renders blinking cursor at bottom', () => {
    render(<TerminalLog logs={logs} data-testid="terminal-log" />)
    expect(screen.getByText('▌')).toBeInTheDocument()
  })

  it('renders empty state without logs', () => {
    render(<TerminalLog logs={[]} data-testid="terminal-log" />)
    expect(screen.getByTestId('terminal-log')).toBeInTheDocument()
    expect(screen.getByText('▌')).toBeInTheDocument()
  })

  it('applies custom className', () => {
    render(<TerminalLog logs={logs} className="my-log" data-testid="terminal-log" />)
    expect(screen.getByTestId('terminal-log')).toHaveClass('my-log')
  })

  it('applies custom inline styles', () => {
    render(<TerminalLog logs={logs} style={{ maxHeight: 200 }} data-testid="terminal-log" />)
    expect(screen.getByTestId('terminal-log')).toHaveStyle({ maxHeight: '200px' })
  })

  it('renders with correct base styling', () => {
    render(<TerminalLog logs={logs} data-testid="terminal-log" />)
    const terminal = screen.getByTestId('terminal-log')
    expect(terminal).toHaveStyle({
      background: '#0a0e1a',
      border: '1px solid #1e2a33',
      fontFamily: "'JetBrains Mono', monospace",
      fontSize: '11px',
    })
  })

  it('renders each log on its own line', () => {
    render(<TerminalLog logs={logs} data-testid="terminal-log" />)
    const terminal = screen.getByTestId('terminal-log')
    const logLines = terminal.querySelectorAll('div > div')
    expect(logLines.length).toBeGreaterThanOrEqual(4)
  })
})
