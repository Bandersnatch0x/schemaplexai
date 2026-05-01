import React, { useRef, useEffect } from 'react'

export type LogLevel = 'INFO' | 'WARN' | 'ERROR' | 'DEBUG'

export interface LogEntry {
  timestamp: string
  level: LogLevel
  message: string
}

export interface TerminalLogProps {
  logs: LogEntry[]
  className?: string
  style?: React.CSSProperties
  'data-testid'?: string
}

const LEVEL_COLORS: Record<LogLevel, string> = {
  INFO: '#00d4aa',
  WARN: '#ff9f43',
  ERROR: '#ff4757',
  DEBUG: '#64748b',
}

export const TerminalLog: React.FC<TerminalLogProps> = ({
  logs,
  className = '',
  style,
  'data-testid': testId,
}) => {
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [logs])

  return (
    <div
      ref={scrollRef}
      className={className}
      data-testid={testId}
      style={{
        background: '#0a0e1a',
        border: '1px solid #1e2a33',
        borderRadius: 8,
        padding: 16,
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: 11,
        lineHeight: 1.8,
        overflow: 'auto',
        maxHeight: 400,
        ...style,
      }}
    >
      {logs.map((log, i) => (
        <div key={i} style={{ display: 'flex', gap: 8 }}>
          <span style={{ color: '#64748b' }}>[{log.timestamp}]</span>
          <span style={{ color: LEVEL_COLORS[log.level], fontWeight: 500 }}>
            {log.level}
          </span>
          <span style={{ color: '#e2e8f0' }}>{log.message}</span>
        </div>
      ))}
      <div style={{ marginTop: 8 }}>
        <span style={{ color: '#00d4aa' }}>▌</span>
      </div>
    </div>
  )
}
