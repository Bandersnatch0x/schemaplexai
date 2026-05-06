import request from './request'

export interface QualityGate {
  id: string
  name: string
  rulesJson?: string
  status: number
  createdAt: string
  updatedAt: string
}

export interface QualityIssue {
  id: string
  title: string
  category: string
  status: string
  score: number
  checkedAt: string
  createdAt: string
  updatedAt: string
}

export function getQualityGates() {
  return request.get<QualityGate[]>('/quality/gates')
}

export function getQualityGateDetail(id: string) {
  return request.get<QualityGate>(`/quality/gates/${id}`)
}

export function createQualityGate(data: Omit<QualityGate, 'id' | 'createdAt' | 'updatedAt'>) {
  return request.post<QualityGate>('/quality/gates', data)
}

export function updateQualityGate(id: string, data: Partial<QualityGate>) {
  return request.put<QualityGate>(`/quality/gates/${id}`, data)
}

export function deleteQualityGate(id: string) {
  return request.delete<void>(`/quality/gates/${id}`)
}

export function getQualityIssues() {
  return request.get<QualityIssue[]>('/quality/issues')
}

export function getQualityIssueDetail(id: string) {
  return request.get<QualityIssue>(`/quality/issues/${id}`)
}
