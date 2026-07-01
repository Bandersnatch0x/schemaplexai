import request from './request'

export interface SystemConfig {
  id: string
  configKey: string
  configValue: string
  category?: string
  description?: string
  createdAt: string
  updatedAt: string
}

export function getSystemConfigs(params?: { category?: string }) {
  return request.get<{ list: SystemConfig[]; total: number }>('/system/configs', { params })
}

export function updateSystemConfig(id: string, value: string) {
  return request.put<void>(`/system/configs/${id}`, { configValue: value })
}

export interface User {
  id: string
  username: string
  nickname?: string
  email?: string
  phone?: string
  status: string
  createdAt: string
}

export async function getUserList(params?: { page?: number; pageSize?: number; keyword?: string }) {
  const res = await request.get<{ records: User[]; total: number }>('/system/users', { params })
  return { list: res.records, total: res.total }
}

export function createUser(data: Omit<User, 'id' | 'createdAt'>) {
  return request.post<User>('/system/users', data)
}

export function updateUser(id: string, data: Partial<User>) {
  return request.put<User>(`/system/users/${id}`, data)
}

export function deleteUser(id: string) {
  return request.delete<void>(`/system/users/${id}`)
}

export interface Role {
  id: string
  name: string
  code: string
  description?: string
  createdAt: string
}

export async function getRoleList() {
  const res = await request.get<{ records: Role[]; total: number }>('/system/roles')
  return res.records
}

export interface Tenant {
  id: string
  name: string
  code: string
  status: string
  createdAt: string
}

export async function getTenantList() {
  const res = await request.get<{ records: Tenant[]; total: number }>('/system/tenants')
  return res.records
}

export interface ModelConfigItem {
  id: string
  provider: string
  model: string
  apiKey?: string
  baseUrl?: string
  priority: number
  enabled: boolean
}

export function getModelConfigs() {
  return request.get<ModelConfigItem[]>('/system/models')
}

export function updateModelConfig(id: string, data: Partial<ModelConfigItem>) {
  return request.put<ModelConfigItem>(`/system/models/${id}`, data)
}
