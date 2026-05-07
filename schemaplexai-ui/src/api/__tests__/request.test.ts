import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import axios from 'axios'

// Mock axios before importing request module
vi.mock('axios', async () => {
  const actual = await vi.importActual<typeof import('axios')>('axios')
  return {
    ...actual,
    default: {
      create: vi.fn(() => ({
        interceptors: {
          request: { use: vi.fn() },
          response: { use: vi.fn() },
        },
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        delete: vi.fn(),
      })),
      post: vi.fn(),
    },
  }
})

// Mock token utilities
vi.mock('@/utils/token', () => ({
  getToken: vi.fn(),
  getTenantId: vi.fn(),
  getRefreshToken: vi.fn(),
  setToken: vi.fn(),
  clearAuth: vi.fn(),
}))

describe('request.ts - axios instance and interceptors', () => {
  let localStorageMock: Record<string, string> = {}

  beforeEach(() => {
    localStorageMock = {}
    vi.stubGlobal('localStorage', {
      getItem: (key: string) => localStorageMock[key] ?? null,
      setItem: (key: string, value: string) => { localStorageMock[key] = value },
      removeItem: (key: string) => { delete localStorageMock[key] },
      clear: () => { localStorageMock = {} },
    })
    vi.clearAllMocks()
    vi.resetModules()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('should create axios instance with correct base config', async () => {
    // Arrange
    const { default: axiosMock } = await import('axios')

    // Act
    await import('@/api/request')

    // Assert
    expect(axiosMock.create).toHaveBeenCalledWith(
      expect.objectContaining({
        baseURL: expect.any(String),
        timeout: 30000,
        headers: {
          'Content-Type': 'application/json',
        },
      })
    )
  })

  it('should add Authorization header from localStorage token', async () => {
    // Arrange
    const { getToken } = await import('@/utils/token')
    vi.mocked(getToken).mockReturnValue('test-token-123')

    // Capture the request interceptor
    const requestInterceptors: Array<(config: unknown) => unknown> = []
    const { default: axiosMock } = await import('axios')
    vi.mocked(axiosMock.create).mockReturnValue({
      interceptors: {
        request: { use: (onFulfilled: (config: unknown) => unknown) => { requestInterceptors.push(onFulfilled) } },
        response: { use: vi.fn() },
      },
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
      request: vi.fn(),
    } as unknown as ReturnType<typeof axios.create>)

    // Act
    await import('@/api/request')
    const config = { headers: {} }
    const result = requestInterceptors[0](config)

    // Assert
    expect(result).toEqual(expect.objectContaining({
      headers: expect.objectContaining({
        Authorization: 'Bearer test-token-123',
      }),
    }))
  })

  it('should add X-Tenant-Id header from store', async () => {
    // Arrange
    const { getToken, getTenantId } = await import('@/utils/token')
    vi.mocked(getToken).mockReturnValue(null)
    vi.mocked(getTenantId).mockReturnValue('tenant-42')

    const requestInterceptors: Array<(config: unknown) => unknown> = []
    const { default: axiosMock } = await import('axios')
    vi.mocked(axiosMock.create).mockReturnValue({
      interceptors: {
        request: { use: (onFulfilled: (config: unknown) => unknown) => { requestInterceptors.push(onFulfilled) } },
        response: { use: vi.fn() },
      },
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
      request: vi.fn(),
    } as unknown as ReturnType<typeof axios.create>)

    // Act
    await import('@/api/request')
    const config = { headers: {} }
    const result = requestInterceptors[0](config)

    // Assert
    expect(result).toEqual(expect.objectContaining({
      headers: expect.objectContaining({
        'X-Tenant-Id': 'tenant-42',
      }),
    }))
  })

  it('should trigger refresh token flow on 401 response', async () => {
    // Arrange
    const { getRefreshToken, setToken } = await import('@/utils/token')
    vi.mocked(getRefreshToken).mockReturnValue('refresh-abc')

    const responseInterceptors: Array<{
      onFulfilled: (response: unknown) => unknown
      onRejected: (error: unknown) => unknown
    }> = []
    const mockPost = vi.fn().mockResolvedValue({
      data: { data: { token: 'new-token-xyz' } },
    })
    const { default: axiosMock } = await import('axios')
    vi.mocked(axiosMock.create).mockReturnValue({
      interceptors: {
        request: { use: vi.fn() },
        response: {
          use: (onFulfilled: (response: unknown) => unknown, onRejected: (error: unknown) => unknown) => {
            responseInterceptors.push({ onFulfilled, onRejected })
          },
        },
      },
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
      request: vi.fn(),
    } as unknown as ReturnType<typeof axios.create>)
    vi.mocked(axiosMock.post).mockImplementation(mockPost)

    // Act
    await import('@/api/request')
    const error = {
      config: { url: '/test', headers: {} },
      response: { status: 401 },
    }

    // Assert
    const rejectionPromise = responseInterceptors[0].onRejected(error)
    await expect(rejectionPromise).resolves.toBeUndefined()
    expect(mockPost).toHaveBeenCalledWith(
      '/auth/refresh',
      {},
      expect.objectContaining({
        headers: expect.objectContaining({ 'X-Refresh-Token': 'refresh-abc' }),
      })
    )
    expect(setToken).toHaveBeenCalledWith('new-token-xyz')
  })

  it('should redirect to /login when refresh token fails', async () => {
    // Arrange
    const { getRefreshToken, clearAuth } = await import('@/utils/token')
    vi.mocked(getRefreshToken).mockReturnValue('refresh-abc')

    const responseInterceptors: Array<{
      onFulfilled: (response: unknown) => unknown
      onRejected: (error: unknown) => unknown
    }> = []
    const { default: axiosMock } = await import('axios')
    vi.mocked(axiosMock.create).mockReturnValue({
      interceptors: {
        request: { use: vi.fn() },
        response: {
          use: (onFulfilled: (response: unknown) => unknown, onRejected: (error: unknown) => unknown) => {
            responseInterceptors.push({ onFulfilled, onRejected })
          },
        },
      },
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
      request: vi.fn(),
    } as unknown as ReturnType<typeof axios.create>)
    vi.mocked(axiosMock.post).mockRejectedValue(new Error('refresh failed'))

    const locationSpy = vi.spyOn(window, 'location', 'get').mockReturnValue({ href: '' } as Location)

    // Act
    await import('@/api/request')
    const error = {
      config: { url: '/test', headers: {} },
      response: { status: 401 },
    }

    // Assert
    await expect(responseInterceptors[0].onRejected(error)).rejects.toThrow('refresh failed')
    expect(clearAuth).toHaveBeenCalled()
    expect(window.location.href).toBe('/login')

    locationSpy.mockRestore()
  })
})
