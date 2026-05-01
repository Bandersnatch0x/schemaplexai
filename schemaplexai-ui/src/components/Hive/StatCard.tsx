import React from 'react'

export interface StatCardProps {
  label: string
  value: string | number
  change?: number
  unit?: string
  sparkline?: number[]
  color?: 'cyan' | 'amber' | 'red'
  className?: string
}

const COLOR_MAP = {
  cyan: '#00d4aa',
  amber: '#ff9f43',
  red: '#ff4757',
}

export const StatCard: React.FC<StatCardProps> = ({
  label,
  value,
  change,
  unit,
  sparkline,
  color = 'cyan',
  className = '',
}) => {
  const c = COLOR_MAP[color]
  const changeText = change !== undefined
    ? `${change >= 0 ? '↑' : '↓'} ${Math.abs(change)}%`
    : null

  return (
    <div
      className={className}
      style={{
        background: '#111827',
        borderRadius: 8,
        padding: 16,
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* Left accent bar */}
      <div
        style={{
          position: 'absolute',
          left: 0,
          top: 0,
          bottom: 0,
          width: 3,
          background: c,
        }}
      />

      <div style={{ color: '#64748b', fontSize: 11, fontWeight: 500, letterSpacing: '0.05em', marginBottom: 8 }}>
        {label.toUpperCase()}
      </div>

      <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginBottom: 4 }}>
        <span
          style={{
            color: '#e2e8f0',
            fontSize: 28,
            fontWeight: 700,
            fontFamily: "'JetBrains Mono', monospace",
          }}
        >
          {value}
        </span>
        {changeText && (
          <span style={{ color: c, fontSize: 12 }}>{changeText}</span>
        )}
      </div>

      {unit && (
        <div style={{ color: '#64748b', fontSize: 10 }}>{unit}</div>
      )}

      {sparkline && sparkline.length > 0 && (
        <div style={{ display: 'flex', alignItems: 'flex-end', gap: 2, marginTop: 12, height: 32 }}>
          {sparkline.map((h, i) => {
            const max = Math.max(...sparkline)
            const pct = max > 0 ? (h / max) * 100 : 0
            return (
              <div
                key={i}
                data-testid="spark-bar"
                style={{
                  width: 8,
                  height: `${Math.max(pct, 10)}%`,
                  background: `${c}${Math.floor(30 + (i / sparkline.length) * 40).toString(16).padStart(2, '0')}`,
                  borderRadius: 1,
                  minHeight: 2,
                }}
              />
            )
          })}
        </div>
      )}
    </div>
  )
}
