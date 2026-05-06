import request from './request'

export interface Integration {
  id: string
  name: string
  type: string
  configJson?: string
  status: number
  createdAt: string
  updatedAt: string
}

export function getIntegrationList() {
  return request.get<Integration[]>('/integration/integrations')
}

export function getIntegrationDetail(id: string) {
  return request.get<Integration>(`/integration/integrations/${id}`)
}

export function createIntegration(data: Omit<Integration, 'id' | 'createdAt' | 'updatedAt'>) {
  return request.post<Integration>('/integration/integrations', data)
}

export function updateIntegration(id: string, data: Partial<Integration>) {
  return request.put<Integration>(`/integration/integrations/${id}`, data)
}

export function deleteIntegration(id: string) {
  return request.delete<void>(`/integration/integrations/${id}`)
}

export function toggleIntegrationStatus(id: string, status: number) {
  return request.put<Integration>(`/integration/integrations/${id}`, { status })
}
