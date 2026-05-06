import request from './request'

export interface OpsArtifact {
  id: string
  name: string
  version?: string
  fileUrl?: string
  artifactType?: string
  status: number
  createdAt: string
  updatedAt: string
}

export interface OpsCost {
  tenantId: string
  totalCost: number
  tokenCost: number
  requestCost: number
}

export function getArtifactList() {
  return request.get<OpsArtifact[]>('/ops/artifacts')
}

export function getArtifactDetail(id: string) {
  return request.get<OpsArtifact>(`/ops/artifacts/${id}`)
}

export function createArtifact(data: Omit<OpsArtifact, 'id' | 'createdAt' | 'updatedAt'>) {
  return request.post<OpsArtifact>('/ops/artifacts', data)
}

export function updateArtifact(id: string, data: Partial<OpsArtifact>) {
  return request.put<OpsArtifact>(`/ops/artifacts/${id}`, data)
}

export function deleteArtifact(id: string) {
  return request.delete<void>(`/ops/artifacts/${id}`)
}

export function getCostsByTenant(tenantId: string) {
  return request.get<Record<string, number>>('/ops/costs', { params: { tenantId } })
}
