import request from './request'

export interface ExecutionStatusVO {
  executionId: number
  agentId: number
  agentName?: string
  state: string
  currentRound?: number
  consumedTokens?: number
  createdAt: string
  updatedAt?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export function getExecutionList(params: { current?: number; size?: number; state?: string; agentId?: number }) {
  return request.get<PageResult<ExecutionStatusVO>>('/web/executions', { params })
}

export function getExecutionStatus(id: number) {
  return request.get<ExecutionStatusVO>(`/web/executions/${id}`)
}

export function pauseExecution(id: number) {
  return request.post(`/web/executions/${id}/pause`)
}

export function resumeExecution(id: number) {
  return request.post(`/web/executions/${id}/resume`)
}

export function cancelExecution(id: number) {
  return request.post(`/web/executions/${id}/cancel`)
}
