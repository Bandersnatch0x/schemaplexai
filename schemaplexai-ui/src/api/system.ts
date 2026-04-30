import request from './request'

export interface SystemConfig {
  id: string
  key: string
  value: string
  category: string
  description?: string
}

export interface SystemLog {
  id: string
  level: string
  module: string
  message: string
  createdAt: string
}

export function getSystemConfigs(params?: { category?: string }) {
  return request.get<SystemConfig[]>('/api/v1/system/configs', { params })
}

export function updateSystemConfig(id: string, value: string) {
  return request.put<void>(`/api/v1/system/configs/${id}`, { value })
}

export function getSystemLogs(params?: { page?: number; pageSize?: number; level?: string }) {
  return request.get<{ list: SystemLog[]; total: number }>('/api/v1/system/logs', { params })
}

export function getSystemMetrics() {
  return request.get<{
    cpuUsage: number
    memoryUsage: number
    diskUsage: number
    activeConnections: number
  }>('/api/v1/system/metrics')
}
