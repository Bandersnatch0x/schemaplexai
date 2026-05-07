import request from './request'
import type { PageResult } from '@/types'

export interface Workflow {
  id: string
  name: string
  description?: string
  status: 'draft' | 'published' | 'disabled'
  nodeConfigJson?: string
  createdAt: string
  updatedAt: string
}

export function getWorkflowList(params?: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<{ list: Workflow[]; total: number }>('/workflow/templates/page', { params })
}

export function getWorkflowDetail(id: string) {
  return request.get<Workflow>(`/workflow/templates/${id}`)
}

export function createWorkflow(data: Omit<Workflow, 'id' | 'createdAt' | 'updatedAt'>) {
  return request.post<Workflow>('/workflow/templates', data)
}

export function updateWorkflow(id: string, data: Partial<Workflow>) {
  return request.put<Workflow>(`/workflow/templates/${id}`, data)
}

export function deleteWorkflow(id: string) {
  return request.delete<void>(`/workflow/templates/${id}`)
}

export function runWorkflow(id: string, payload?: Record<string, unknown>) {
  return request.post<string>(`/workflow/instances/${id}/run`, payload)
}

export interface WorkflowInstance {
  id: string
  templateId: string
  status: string
  triggerType: string
  triggerConfig?: string
  createdAt: string
  updatedAt: string
}

export function getWorkflowInstances(params?: { current?: number; size?: number }) {
  return request.get<PageResult<WorkflowInstance>>('/workflow/instances/page', { params })
}
