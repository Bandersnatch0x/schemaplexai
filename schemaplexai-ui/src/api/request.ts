import axios, { AxiosInstance, AxiosRequestConfig } from 'axios'
import { getToken, getTenantId, clearAuth, setToken, getRefreshToken } from '@/utils/token'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const instance: AxiosInstance = axios.create({
  baseURL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

instance.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    const tenantId = getTenantId()
    if (tenantId) {
      config.headers['X-Tenant-Id'] = tenantId
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

let isRefreshing = false
let refreshSubscribers: ((token: string) => void)[] = []

instance.interceptors.response.use(
  (response) => {
    // Parse XSS protection headers if provided by backend
    const cspHeader = response.headers['content-security-policy']
    const xssProtection = response.headers['x-xss-protection']
    if (cspHeader || xssProtection) {
      // Backend has enabled XSS protection response headers
      // These are handled automatically by modern browsers
    }

    const { data } = response
    if (data.code !== undefined && data.code !== 200 && data.code !== 0) {
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data.data ?? data
  },
  async (error) => {
    const { config, response } = error
    if (response?.status === 401 && !config?.__retry) {
      if (!isRefreshing) {
        isRefreshing = true
        try {
          const res = await axios.post('/auth/refresh', {}, {
            headers: { 'X-Refresh-Token': getRefreshToken() || '' },
          })
          const newToken = res.data.data.token
          setToken(newToken)
          refreshSubscribers.forEach((cb) => cb(newToken))
          refreshSubscribers = []
          config.headers.Authorization = `Bearer ${newToken}`
          config.__retry = true
          return instance.request(config)
        } catch (refreshError) {
          clearAuth()
          window.location.href = '/login'
          return Promise.reject(refreshError)
        } finally {
          isRefreshing = false
        }
      }

      return new Promise((resolve) => {
        refreshSubscribers.push((token: string) => {
          config.headers.Authorization = `Bearer ${token}`
          config.__retry = true
          resolve(instance.request(config))
        })
      })
    }

    if (error.response) {
      const { status, data } = error.response
      const message = data?.message || `请求错误: ${status}`
      return Promise.reject(new Error(message))
    }
    return Promise.reject(new Error('网络错误，请检查网络连接'))
  }
)

interface RequestInstance {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T>
}

const request = instance as RequestInstance

export function sseRequest(url: string, _body?: Record<string, unknown>): EventSource {
  const wsBase = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8080'
  const fullUrl = `${wsBase}${url}`
  const eventSource = new EventSource(fullUrl, { withCredentials: true })
  return eventSource
}

export default request
