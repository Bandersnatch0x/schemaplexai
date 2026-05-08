import request from './request'
import type { ExecutionRecord } from '@/types'

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
