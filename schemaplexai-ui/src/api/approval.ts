import request from './request'

export interface ApprovalVO {
  ticketId: number
  executionId: number
  toolName?: string
  requestedBy?: string
  reason?: string
  status: string
  createdAt: string
}

export function getPendingApprovals() {
  return request.get<ApprovalVO[]>('/web/approvals')
}

export function approveTicket(ticketId: number, approverId: string, reason?: string) {
  return request.post(`/web/approvals/${ticketId}/approve`, null, { params: { approverId, reason } })
}

export function rejectTicket(ticketId: number, approverId: string, reason?: string) {
  return request.post(`/web/approvals/${ticketId}/reject`, null, { params: { approverId, reason } })
}

export function escalateTicket(ticketId: number, approverId: string, reason?: string) {
  return request.post(`/web/approvals/${ticketId}/escalate`, null, { params: { approverId, reason } })
}
