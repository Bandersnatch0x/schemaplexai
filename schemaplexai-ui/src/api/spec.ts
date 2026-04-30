import request from './request'

export interface SpecItem {
  id: string
  name: string
  version: string
  type: string
  status: string
  content?: string
  createdAt: string
  updatedAt: string
}

export function getSpecList(params?: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<{ list: SpecItem[]; total: number }>('/api/v1/specs', { params })
}

export function getSpecDetail(id: string) {
  return request.get<SpecItem>(`/api/v1/specs/${id}`)
}

export function createSpec(data: Omit<SpecItem, 'id' | 'createdAt' | 'updatedAt'>) {
  return request.post<SpecItem>('/api/v1/specs', data)
}

export function updateSpec(id: string, data: Partial<SpecItem>) {
  return request.put<SpecItem>(`/api/v1/specs/${id}`, data)
}

export function deleteSpec(id: string) {
  return request.delete<void>(`/api/v1/specs/${id}`)
}
