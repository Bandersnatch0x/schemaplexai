import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import Login from '@/pages/Login'
import * as authApi from '@/api/auth'
import { useUserStore } from '@/stores/userStore'

// vi.hoisted() ensures these are available when vi.mock factories execute
const { mockNavigate, mockMessage } = vi.hoisted(() => ({
  mockNavigate: vi.fn(),
  mockMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  },
}))

// Mock react-i18next to return Chinese translations
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => {
      const translations: Record<string, string> = {
        'nav.cockpit': '驾驶舱',
        'nav.canvas': '编排画布',
        'nav.workflows': '工作流',
        'nav.agents': 'Agent 管理',
        'nav.specs': 'Spec 中心',
        'nav.contexts': '上下文',
        'nav.quality': '质量与安全',
        'nav.integrations': '集成与工具',
        'nav.ops': '交付与运营',
        'nav.notifications': '通知中心',
        'nav.settings': '系统设置',
        'login.heroTitle': 'Where agents converge & cooperate.',
        'login.identify': 'Identify yourself, operator.',
        'login.tenantWorkspace': 'Tenant Workspace',
        'login.enterHive': 'Enter Hive',
        'login.inputIdentifier': '请输入身份标识',
        'login.inputPassword': '请输入信息素密钥',
        'login.welcomeSuccess': '已唤醒蜂巢 · Welcome operator',
        'login.loginFailed': '登录失败',
        'login.username': '用户名',
        'login.password': '密码',
        'login.forgotPassword': '忘记密码?',
        'login.rememberMe': 'Remember on this hive',
        'login.sessionTtl': 'SESSION TTL',
        'login.awakening': 'Awakening',
        'login.orSignalVia': 'or signal via',
      }
      return translations[key] || key
    },
    i18n: { language: 'zh' },
  }),
  Trans: ({ children }: { children: React.ReactNode }) => children,
}))

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  getTenantList: vi.fn(),
  saveAuth: vi.fn(),
}))

vi.mock('@/stores/userStore', () => ({
  useUserStore: vi.fn(),
}))

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>()
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

vi.mock('antd', async (importOriginal) => {
  const actual = await importOriginal<typeof import('antd')>()
  return {
    ...actual,
    message: mockMessage,
  }
})

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <Login />
    </MemoryRouter>,
  )
}

describe('Login component', () => {
  const mockSetUserInfo = vi.fn()
  const mockSetCurrentTenant = vi.fn()
  const mockSetTenants = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()

    vi.mocked(useUserStore).mockReturnValue({
      userInfo: null,
      currentTenant: null,
      tenants: [],
      setUserInfo: mockSetUserInfo,
      setCurrentTenant: mockSetCurrentTenant,
      setTenants: mockSetTenants,
      isLoggedIn: false,
    } as ReturnType<typeof useUserStore>)
  })

  // ---- Render tests ----

  it('renders the login stage with all key elements', () => {
    renderLogin()

    expect(screen.getByTestId('login-page')).toBeInTheDocument()
    expect(screen.getByTestId('login-username')).toBeInTheDocument()
    expect(screen.getByTestId('login-password')).toBeInTheDocument()
    expect(screen.getByTestId('login-submit')).toBeInTheDocument()
    expect(screen.getByTestId('login-remember')).toBeInTheDocument()

    expect(screen.getByText(/Where agents/)).toBeInTheDocument()
    expect(screen.getByText(/converge/)).toBeInTheDocument()

    expect(screen.getByText('Enter Hive')).toBeInTheDocument()
  })

  it('renders auth form title and tenant workspace badge', () => {
    renderLogin()

    expect(screen.getByRole('heading', { name: /Identify yourself/i })).toBeInTheDocument()
    expect(screen.getByText('Tenant Workspace')).toBeInTheDocument()
  })

  // ---- Input interaction tests ----

  it('allows typing into username and password fields', async () => {
    const user = userEvent.setup()
    renderLogin()

    const usernameInput = screen.getByTestId('login-username')
    const passwordInput = screen.getByTestId('login-password')

    await user.clear(usernameInput)
    await user.type(usernameInput, 'aurora.han@aerolabs.cn')
    await user.clear(passwordInput)
    await user.type(passwordInput, 'secureP@ss1')

    expect(usernameInput).toHaveValue('aurora.han@aerolabs.cn')
    expect(passwordInput).toHaveValue('secureP@ss1')
  })

  it('toggles password visibility when eye button is clicked', async () => {
    const user = userEvent.setup()
    renderLogin()

    const passwordInput = screen.getByTestId('login-password')
    const toggleBtn = screen.getByLabelText('显示密码')

    await user.click(toggleBtn)
    expect(passwordInput).toHaveAttribute('type', 'text')

    const toggleBtnHidden = screen.getByLabelText('隐藏密码')
    await user.click(toggleBtnHidden)
    expect(passwordInput).toHaveAttribute('type', 'password')
  })

  it('calls remember checkbox toggle', async () => {
    const user = userEvent.setup()
    renderLogin()

    const checkbox = screen.getByTestId('login-remember')
    expect(checkbox).toBeChecked()

    await user.click(checkbox)
    expect(checkbox).not.toBeChecked()
  })

  // ---- Validation tests ----

  it('shows warning when username is empty on submit', async () => {
    const user = userEvent.setup()
    renderLogin()

    await user.click(screen.getByTestId('login-submit'))

    expect(mockMessage.warning).toHaveBeenCalledWith('请输入身份标识')
    expect(authApi.login).not.toHaveBeenCalled()
  })

  it('shows warning when password is empty on submit', async () => {
    const user = userEvent.setup()
    renderLogin()

    await user.type(screen.getByTestId('login-username'), 'aurora.han@aerolabs.cn')
    await user.click(screen.getByTestId('login-submit'))

    expect(mockMessage.warning).toHaveBeenCalledWith('请输入信息素密钥')
    expect(authApi.login).not.toHaveBeenCalled()
  })

  // ---- Successful submit flow ----

  it('calls login API, saves auth, fetches tenants, sets store, and navigates on success', async () => {
    const user = userEvent.setup()
    const loginResult = { accessToken: 'tok-abc', refreshToken: 'rt-xyz', tokenType: 'Bearer' }
    const tenants = [
      { id: 'default', name: '默认工作区', code: 'DEFAULT' },
      { id: 'rd-1', name: '研发一部', code: 'RD1' },
    ]

    vi.mocked(authApi.login).mockResolvedValueOnce(loginResult)
    vi.mocked(authApi.getTenantList).mockResolvedValueOnce(tenants)

    renderLogin()

    await user.type(screen.getByTestId('login-username'), 'aurora.han@aerolabs.cn')
    await user.type(screen.getByTestId('login-password'), 'P@ssw0rd!')
    await user.click(screen.getByTestId('login-submit'))

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalledWith({
        username: 'aurora.han@aerolabs.cn',
        password: 'P@ssw0rd!',
      })
      expect(authApi.saveAuth).toHaveBeenCalledWith(loginResult, 'default')
      expect(authApi.getTenantList).toHaveBeenCalled()
      expect(mockSetTenants).toHaveBeenCalledWith(tenants)
      expect(mockSetCurrentTenant).toHaveBeenCalledWith(
        expect.objectContaining({ id: 'default' }),
      )
      expect(mockSetUserInfo).toHaveBeenCalledWith(
        expect.objectContaining({ username: 'aurora.han@aerolabs.cn', roles: ['admin'] }),
      )
      expect(localStorage.getItem('schemaplexai_last_user')).toBe('aurora.han@aerolabs.cn')
      expect(mockMessage.success).toHaveBeenCalledWith('已唤醒蜂巢 · Welcome operator')
      expect(mockNavigate).toHaveBeenCalledWith('/cockpit')
    })
  })

  // ---- Error handling ----

  it('shows error message when login API fails', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.login).mockRejectedValueOnce(new Error('Invalid credentials'))

    renderLogin()

    await user.type(screen.getByTestId('login-username'), 'aurora.han@aerolabs.cn')
    await user.type(screen.getByTestId('login-password'), 'wrong-password')
    await user.click(screen.getByTestId('login-submit'))

    await waitFor(() => {
      expect(mockMessage.error).toHaveBeenCalledWith('Invalid credentials')
      expect(mockNavigate).not.toHaveBeenCalled()
    })
  })

  it('shows generic error for non-Error rejection', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.login).mockRejectedValueOnce('Network timeout')

    renderLogin()

    await user.type(screen.getByTestId('login-username'), 'aurora.han@aerolabs.cn')
    await user.type(screen.getByTestId('login-password'), 'P@ssw0rd!')
    await user.click(screen.getByTestId('login-submit'))

    await waitFor(() => {
      expect(mockMessage.error).toHaveBeenCalledWith('登录失败')
    })
  })

  // ---- Remember-me restore ----

  it('restores remembered username from localStorage on mount', () => {
    localStorage.setItem('schemaplexai_last_user', 'saved.user@aerolabs.cn')

    renderLogin()

    expect(screen.getByTestId('login-username')).toHaveValue('saved.user@aerolabs.cn')
  })

  // ---- Accessibility ----

  it('uses aria-label attributes on key interactive elements', () => {
    renderLogin()

    expect(screen.getByLabelText('用户名')).toBeInTheDocument()
    expect(screen.getByLabelText('密码')).toBeInTheDocument()
    expect(screen.getByLabelText('硬件密钥登录')).toBeInTheDocument()
  })
})
