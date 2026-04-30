import request from './request'

export interface Workflow {
  id: string
  name: string
  description?: string
  status: 'draft' | 'published' | 'disabled'
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
  createdAt: string
  updatedAt: string
}

export interface WorkflowNode {
  id: string
  type: string
  label: string
  config?: Record<string, unknown>
  position?: { x: number; y: number }
}

export interface WorkflowEdge {
  id: string
  source: string
  target: string
  label?: string
}

export function getWorkflowList(params?: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<{ list: Workflow[]; total: number }>('/api/v1/workflows', { params })
}

export function getWorkflowDetail(id: string) {
  return request.get<Workflow>(`/api/v1/workflows/${id}`)
}

export function createWorkflow(data: Omit<Workflow, 'id' | 'createdAt' | 'updatedAt'>) {
  return request.post<Workflow>('/api/v1/workflows', data)
}

export function updateWorkflow(id: string, data: Partial<Workflow>) {
  return request.put<Workflow>(`/api/v1/workflows/${id}`, data)
}

export function deleteWorkflow(id: string) {
  return request.delete<void>(`/api/v1/workflows/${id}`)
}

export function runWorkflow(id: string, payload?: Record<string, unknown>) {
  return request.post<string>(`/api/v1/workflows/${id}/run`, payload)
}
