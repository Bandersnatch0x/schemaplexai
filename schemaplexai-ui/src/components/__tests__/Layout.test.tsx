import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import Layout from '@/components/Layout'

// Mock zustand store
vi.mock('@/stores/userStore', () => ({
  useUserStore: vi.fn(() => ({
    userInfo: null,
    setUserInfo: vi.fn(),
  })),
}))

// Mock token utilities
vi.mock('@/utils/token', () => ({
  clearAuth: vi.fn(),
}))

// Mock TenantSelector to avoid its own store dependencies
vi.mock('@/components/TenantSelector', () => ({
  default: () => <div data-testid="tenant-selector">TenantSelector</div>,
}))

describe('Layout component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  function renderWithRouter(initialRoute = '/dashboard') {
    return render(
      <ConfigProvider>
        <MemoryRouter initialEntries={[initialRoute]}>
          <Routes>
            <Route path="/*" element={<Layout />} />
          </Routes>
        </MemoryRouter>
      </ConfigProvider>
    )
  }

  it('renders header with logo text', () => {
    renderWithRouter('/dashboard')
    expect(screen.getByText('SchemaPlexAI')).toBeInTheDocument()
  })

  it('renders sidebar with menu items', () => {
    renderWithRouter('/dashboard')
    expect(screen.getByText('工作台')).toBeInTheDocument()
    expect(screen.getByText('Spec 中心')).toBeInTheDocument()
    expect(screen.getByText('Agent 管理')).toBeInTheDocument()
  })

  it('renders TenantSelector in header', () => {
    renderWithRouter('/dashboard')
    expect(screen.getByTestId('tenant-selector')).toBeInTheDocument()
  })

  it('renders content area for nested routes', () => {
    const { container } = renderWithRouter('/dashboard')
    const content = container.querySelector('.ant-layout-content')
    expect(content).toBeInTheDocument()
  })
})
