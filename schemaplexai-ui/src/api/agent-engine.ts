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

/**
 * Tenant-scoped agent statistics for the Cockpit page.
 * Backend: GET /agent-config/agents/stats (schemaplexai-agent-config service,
 * routed via the gateway's /agent-config/** route). Tenant isolation is
 * enforced server-side from the X-Tenant-Id header the request interceptor
 * attaches.
 */
export interface AgentStats {
  totalAgents: number
  activeAgents: number
  runningExecutions: number
  totalExecutions: number
  todayExecutions: number
  totalTokens: number
  pendingApprovals: number
}

export function getAgentStats() {
  return request.get<AgentStats>('/agent-config/agents/stats')
}
