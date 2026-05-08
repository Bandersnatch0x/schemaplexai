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

export interface SystemLog {
  id: string
  level: string
  module: string
  message: string
  createdAt: string
}

export function getSystemConfigs(params?: { category?: string }) {
  return request.get<{ list: SystemConfig[]; total: number }>('/system/configs', { params })
}

export function updateSystemConfig(id: string, value: string) {
  return request.put<void>(`/system/configs/${id}`, { configValue: value })
}

export function getSystemLogs(params?: { page?: number; pageSize?: number; level?: string }) {
  return request.get<{ list: SystemLog[]; total: number }>('/system/logs', { params })
}

export function getSystemMetrics() {
  return request.get<{
    cpuUsage: number
    memoryUsage: number
    diskUsage: number
    activeConnections: number
  }>('/system/metrics')
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

export function getUserList(params?: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<{ list: User[]; total: number }>('/system/users', { params })
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

export function getRoleList() {
  return request.get<Role[]>('/system/roles')
}

export interface Tenant {
  id: string
  name: string
  code: string
  status: string
  createdAt: string
}

export function getTenantList() {
  return request.get<Tenant[]>('/system/tenants')
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
