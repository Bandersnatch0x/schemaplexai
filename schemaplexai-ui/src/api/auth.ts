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

export function getCurrentUser(userId: string): Promise<UserInfo> {
  return request.get<UserInfo>(`/system/users/${userId}`)
}

export async function getTenantList(): Promise<Tenant[]> {
  const res = await request.get<{ records: Tenant[]; total: number }>('/system/tenants')
  return res.records
}

export function changePassword(oldPassword: string, newPassword: string) {
  return request.post<void>('/auth/change-password', { oldPassword, newPassword })
}

export function saveAuth(result: LoginResult, tenantId: string) {
  setToken(result.accessToken)
  setRefreshToken(result.refreshToken)
  setTenantId(tenantId)
}
