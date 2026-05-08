import { useState, useEffect, useCallback } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'

export interface DomainNavItem {
  key: string
  icon: string
  label: string
  path: string
  children?: { key: string; label: string; path: string }[]
}

export interface DomainNavProps {
  items: DomainNavItem[]
}

const STORAGE_KEY = 'nav_expanded'

export function DomainNav({ items }: DomainNavProps) {
  const navigate = useNavigate()
  const location = useLocation()

  const [expandedKeys, setExpandedKeys] = useState<string[]>(() => {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
    } catch {
      return []
    }
  })

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(expandedKeys))
  }, [expandedKeys])

  const isActive = useCallback(
    (path: string) => {
      return location.pathname === path || location.pathname.startsWith(path + '/')
    },
    [location.pathname]
  )

  const toggleExpand = (key: string) => {
    setExpandedKeys((prev) =>
      prev.includes(key) ? prev.filter((k) => k !== key) : [...prev, key]
    )
  }

  const activeParentKey = items.find((item) =>
    item.children?.some((child) => isActive(child.path))
  )?.key

  return (
    <nav className="domain-nav" data-testid="domain-nav">
      {items.map((item) => {
        const hasChildren = item.children && item.children.length > 0
        const isExpanded = expandedKeys.includes(item.key) || item.key === activeParentKey
        const parentActive = isActive(item.path) || item.key === activeParentKey

        return (
          <div key={item.key} className="domain-nav-group" data-testid={`nav-group-${item.key}`}>
            <div
              className={`domain-nav-item${parentActive ? ' domain-nav-item--active' : ''}`}
              onClick={() => {
                if (hasChildren) {
                  toggleExpand(item.key)
                } else {
                  navigate(item.path)
                }
              }}
            >
              <span className="domain-nav-item-icon">{item.icon}</span>
              <span className="domain-nav-item-label">{item.label}</span>
              {hasChildren && (
                <span className={`domain-nav-chevron${isExpanded ? ' domain-nav-chevron--expanded' : ''}`}>
                  ▶
                </span>
              )}
            </div>

            {hasChildren && isExpanded && (
              <div className="domain-nav-children">
                {item.children?.map((child) => (
                  <div
                    key={child.key}
                    className={`domain-nav-child${isActive(child.path) ? ' domain-nav-child--active' : ''}`}
                    onClick={() => navigate(child.path)}
                    data-testid={`nav-child-${child.key}`}
                  >
                    {child.label}
                  </div>
                ))}
              </div>
            )}
          </div>
        )
      })}
    </nav>
  )
}
