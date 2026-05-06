import request from './request'
import type { Agent, ExecutionRecord } from '@/types'

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
  status?: string
  modelConfig?: {
    model: string
    temperature: number
    maxTokens: number
    topP: number
  }
  tools?: string[]
}

export function getAgentList(params: AgentQuery) {
  return request.get<{ list: Agent[]; total: number }>('/agent-config/agents', { params })
}

export function getAgentDetail(id: string) {
  return request.get<Agent>(`/agent-config/agents/${id}`)
}

export function createAgent(data: CreateAgentPayload) {
  return request.post<Agent>('/agent-config/agents', data)
}

export function updateAgent(id: string, data: CreateAgentPayload) {
  return request.put<Agent>(`/agent-config/agents/${id}`, data)
}

export function deleteAgent(id: string) {
  return request.delete<void>(`/agent-config/agents/${id}`)
}

export function executeAgent(id: string, prompt: string) {
  return request.post<string>(`/agents/${id}/execute`, { prompt })
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
