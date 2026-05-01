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
  size = 40,
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
    clipPath: 'polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%)',
    border: `1px solid ${color}`,
    backgroundColor: active ? `${color}20` : 'transparent',
    boxShadow: active ? `0 0 12px ${color}30` : 'none',
    color,
    fontSize: size * 0.4,
    fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
    transition: 'all 0.2s ease',
    cursor: 'pointer',
    ...style,
  }

  return (
    <div className={className} style={baseStyle} data-testid={testId}>
      {children}
    </div>
  )
}
