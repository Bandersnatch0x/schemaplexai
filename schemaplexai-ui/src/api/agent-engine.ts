import request from './request'
import { getTenantId } from '@/utils/token'
import type { ExecutionRecord } from '@/types'

export function executeAgent(id: string, prompt: string) {
  const tenantId = getTenantId()
  return request.post<string>(`/agents/${id}/execute`, { prompt, tenantId })
}

export function getExecutionRecords(agentId?: string) {
  return request.get<{ records: ExecutionRecord[]; total: number }>('/web/executions', { params: { agentId } })
}

/** Stub — no backend endpoint exists yet. Returns zeroed stats. */
export function getAgentStats() {
  return Promise.resolve({
    totalAgents: 0,
    totalExecutions: 0,
    totalTokens: 0,
    pendingApprovals: 0,
  })
}
