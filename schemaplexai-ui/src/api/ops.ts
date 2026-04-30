import request from './request'

export interface OpsTask {
  id: string
  title: string
  status: 'pending' | 'running' | 'completed' | 'failed'
  type: string
  assignee?: string
  deadline?: string
  createdAt: string
  updatedAt: string
}

export interface OpsMetric {
  date: string
  deployments: number
  incidents: number
  uptime: number
}

export function getOpsTasks(params?: { page?: number; pageSize?: number; status?: string }) {
  return request.get<{ list: OpsTask[]; total: number }>('/api/v1/ops/tasks', { params })
}

export function getOpsMetrics(params?: { startDate?: string; endDate?: string }) {
  return request.get<OpsMetric[]>('/api/v1/ops/metrics', { params })
}

export function createOpsTask(data: Omit<OpsTask, 'id' | 'createdAt' | 'updatedAt'>) {
  return request.post<OpsTask>('/api/v1/ops/tasks', data)
}

export function updateOpsTask(id: string, data: Partial<OpsTask>) {
  return request.put<OpsTask>(`/api/v1/ops/tasks/${id}`, data)
}

export function deleteOpsTask(id: string) {
  return request.delete<void>(`/api/v1/ops/tasks/${id}`)
}
