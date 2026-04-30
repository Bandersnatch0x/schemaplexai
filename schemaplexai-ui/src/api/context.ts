import request from './request'

export interface ContextItem {
  id: string
  name: string
  type: 'knowledge' | 'memory' | 'document'
  content?: string
  metadata?: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export function getContextList(params?: { page?: number; pageSize?: number; keyword?: string; type?: string }) {
  return request.get<{ list: ContextItem[]; total: number }>('/api/v1/contexts', { params })
}

export function getContextDetail(id: string) {
  return request.get<ContextItem>(`/api/v1/contexts/${id}`)
}

export function createContext(data: Omit<ContextItem, 'id' | 'createdAt' | 'updatedAt'>) {
  return request.post<ContextItem>('/api/v1/contexts', data)
}

export function updateContext(id: string, data: Partial<ContextItem>) {
  return request.put<ContextItem>(`/api/v1/contexts/${id}`, data)
}

export function deleteContext(id: string) {
  return request.delete<void>(`/api/v1/contexts/${id}`)
}

export function searchKnowledge(query: string, topK = 5) {
  return request.post<{ results: { id: string; score: number; content: string }[] }>('/api/v1/contexts/search', {
    query,
    topK,
  })
}
