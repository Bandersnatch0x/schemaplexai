import React from 'react'

export interface HexIconProps {
  size?: number
  color?: string
  active?: boolean
  className?: string
  style?: React.CSSProperties
  children?: React.ReactNode
  'data-testid'?: string
}

export const HexIcon: React.FC<HexIconProps> = ({
  size = 36,
  color = '#00d4aa',
  active = false,
  className = '',
  style,
  children,
  'data-testid': testId,
}) => {
  const baseStyle: React.CSSProperties = {
    width: size,
    height: size,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 8,
    border: `1px solid ${active ? color : '#1e2a33'}`,
    background: active ? `${color}20` : '#111827',
    boxShadow: active ? `0 0 8px ${color}30` : 'none',
    transition: 'all 0.2s ease',
    cursor: 'pointer',
    ...style,
  }

  return (
    <div className={className} style={baseStyle} data-testid={testId}>
      {children || (
        <span style={{ color, fontSize: size * 0.45 }}>⬡</span>
      )}
    </div>
  )
}
