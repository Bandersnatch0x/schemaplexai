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
