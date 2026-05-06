import request from './request'
import { setToken, setRefreshToken, setTenantId } from '@/utils/token'
import type { UserInfo, Tenant } from '@/types'

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
}

export function login(payload: LoginPayload) {
  return request.post<LoginResult>('/auth/login', payload)
}

export function refreshToken(refreshToken: string) {
  return request.post<LoginResult>('/auth/refresh', { refreshToken })
}

export function logout() {
  return request.post<void>('/auth/logout')
}

export function getCurrentUser(): Promise<UserInfo> {
  return request.get<UserInfo>('/system/users/current')
}

export function getTenantList(): Promise<Tenant[]> {
  return request.get<Tenant[]>('/system/tenants')
}

export function saveAuth(result: LoginResult, tenantId: string) {
  setToken(result.accessToken)
  setRefreshToken(result.refreshToken)
  setTenantId(tenantId)
}
