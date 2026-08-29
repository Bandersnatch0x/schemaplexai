import request from './request'

export interface CostSummaryVO {
  totalCost: number
  totalExecutions: number
  byAgent?: Record<string, number>
  byTenant?: Record<string, number>
  period?: string
}

export interface ExecutionCostVO {
  executionId: number
  agentName?: string
  tokenCost: number
  toolCost: number
  totalCost: number
}

export function getCostSummary() {
  return request.get<CostSummaryVO>('/web/costs/summary')
}

export function getExecutionCost(executionId: number) {
  return request.get<ExecutionCostVO>(`/web/costs/executions/${executionId}`)
}
