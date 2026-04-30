import request from './request'
import type { Agent, ExecutionRecord, PageResult } from '@/types'

export interface AgentQuery {
  page?: number
  pageSize?: number
  keyword?: string
  status?: string
}

export interface CreateAgentPayload {
  name: string
  description?: string
  type: string
  modelConfig?: {
    model: string
    temperature: number
    maxTokens: number
    topP: number
  }
  tools?: string[]
}

export function getAgentList(params: AgentQuery) {
  return request.get<PageResult<Agent>>('/api/v1/agents', { params })
}

export function getAgentDetail(id: string) {
  return request.get<Agent>(`/api/v1/agents/${id}`)
}

export function createAgent(data: CreateAgentPayload) {
  return request.post<Agent>('/api/v1/agents', data)
}

export function updateAgent(id: string, data: CreateAgentPayload) {
  return request.put<Agent>(`/api/v1/agents/${id}`, data)
}

export function deleteAgent(id: string) {
  return request.delete<void>(`/api/v1/agents/${id}`)
}

export function executeAgent(id: string, prompt: string) {
  return request.post<string>(`/api/v1/agents/${id}/execute`, { prompt })
}

export function getExecutionRecords(agentId?: string) {
  return request.get<ExecutionRecord[]>('/api/v1/agents/executions', { params: { agentId } })
}

export function getAgentStats() {
  return request.get<{
    totalAgents: number
    totalExecutions: number
    totalTokens: number
    pendingApprovals: number
  }>('/api/v1/agents/stats')
}
