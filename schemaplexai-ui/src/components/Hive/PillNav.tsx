import React from 'react'

export interface PillNavItem {
  key: string
  label: string
}

export interface PillNavProps {
  items: PillNavItem[]
  activeKey: string
  onChange: (key: string) => void
  className?: string
}

export const PillNav: React.FC<PillNavProps> = ({
  items,
  activeKey,
  onChange,
  className = '',
}) => {
  return (
    <div
      className={className}
      style={{
        display: 'flex',
        background: '#0d1117',
        border: '1px solid #1e2a33',
        borderRadius: 20,
        padding: 3,
        gap: 3,
      }}
    >
      {items.map((item) => {
        const isActive = item.key === activeKey
        return (
          <button
            key={item.key}
            onClick={() => onChange(item.key)}
            style={{
              padding: '5px 16px',
              borderRadius: 16,
              border: 'none',
              background: isActive ? '#00d4aa' : 'transparent',
              color: isActive ? '#0a0e1a' : '#64748b',
              fontSize: 12,
              fontWeight: isActive ? 600 : 400,
              cursor: 'pointer',
              transition: 'all 0.2s ease',
              fontFamily: 'inherit',
            }}
          >
            {item.label}
          </button>
        )
      })}
    </div>
  )
}
